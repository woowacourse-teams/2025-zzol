package coffeeshout.nunchi.ui;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.nunchi.application.NunchiService;
import coffeeshout.nunchi.application.response.NunchiStandResponse;
import coffeeshout.nunchi.application.response.NunchiStateResponse;
import coffeeshout.nunchi.config.NunchiTimingProperties;
import coffeeshout.nunchi.domain.NunchiGame;
import coffeeshout.nunchi.domain.NunchiState;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import coffeeshout.websocket.WsRecoveryService;
import coffeeshout.websocket.ui.dto.RecoveryMessage;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 눈치게임 WebSocket 통합 테스트(ADR-0031). TestContainers(Redis) 기반이라 Docker가 필요하다 —
 * 입력 경로 Handler→Redis Stream→Consumer→Service→Notifier가 실제로 동작하는지(아키텍처 불변 규칙)와
 * stand/state 브로드캐스트 컨트랙트(결정 8)를 검증한다.
 *
 * <p>타이밍(application-test-game.yml): number-window=300ms, collision-cooldown=500ms,
 * idle-timeout=2000ms, hard-cap=5000ms.
 */
class NunchiIntegrationTest extends GameModuleWebSocketTest {

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;
    TestStompSession session;

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    NunchiService nunchiService;

    @Autowired
    NunchiTimingProperties timing;

    @Autowired
    WsRecoveryService wsRecoveryService;

    @BeforeEach
    void setUp(@Autowired JoinCodeGenerator joinCodeGenerator) throws Exception {
        joinCode = joinCodeGenerator.generate();
        host = GamerFixture.호스트_꾹이();
        gamers = GamerFixture.꾹이_루키_엠제이_한스();

        // GameSession을 READY로 사전 구성한다 — Room 검증·영속을 거치지 않고 :game만으로 시작(ADR-0025, BlockStacking IT와 동일).
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode)
                .replaceGames(host, List.of(new NunchiGame(timing.numberWindow().toMillis())));

        session = createSession(joinCode.getValue(), host.getName());
    }

    @Nested
    class 상태_브로드캐스트_테스트 {

        @Test
        void 시작하면_PLAYING_상태를_브로드캐스트한다() {
            final var stateResponses = session.subscribe(stateUrl());

            startNunchiGame();

            final NunchiStateResponse playing =
                    payloadAs(stateResponses.get(), NunchiStateResponse.class);
            assertThat(playing.state()).isEqualTo(NunchiState.PLAYING);
            assertThat(playing.currentNumber()).isEqualTo(1);
            assertThat(playing.serverNowEpochMs()).isNotNull();
            assertThat(playing.idleDeadlineEpochMs()).isNotNull();
            assertThat(playing.hardCapEpochMs()).isNotNull();
        }
    }

    @Nested
    class 입력_브로드캐스트_테스트 {

        @Test
        void 단독_press는_stand를_브로드캐스트한다() {
            final var standResponses = session.subscribe(standUrl());
            final var stateResponses = session.subscribe(stateUrl());

            startNunchiGame();
            stateResponses.get(); // PLAYING

            session.send(pressCommandUrl()); // 호스트(꾹이) 단독 press

            final NunchiStandResponse stand =
                    payloadAs(standResponses.get(), NunchiStandResponse.class);
            assertThat(stand.name()).isEqualTo(host.getName());
            assertThat(stand.number()).isEqualTo(1);
            assertThat(stand.idleDeadlineEpochMs()).isPositive();
        }

        @Test
        void 윈도우_내_동시_press_2명은_COLLISION_COOLDOWN을_브로드캐스트한다() throws Exception {
            final TestStompSession 루키세션 = createSession(joinCode.getValue(), "루키");
            final var stateResponses = session.subscribe(stateUrl());

            startNunchiGame();
            stateResponses.get(); // PLAYING

            // 윈도우(300ms) 안에 두 명이 누르면 충돌
            session.send(pressCommandUrl());
            루키세션.send(pressCommandUrl());

            // PLAYING 다음의 상태 변경에서 COLLISION_COOLDOWN을 찾는다(stand는 별도 토픽)
            final MessageResponse next = stateResponses.get(2, TimeUnit.SECONDS);
            final NunchiStateResponse cooldown = payloadAs(next, NunchiStateResponse.class);
            assertThat(cooldown.state()).isEqualTo(NunchiState.COLLISION_COOLDOWN);
            assertThat(cooldown.collided()).contains(host.getName(), "루키");
            assertThat(cooldown.resumeAtEpochMs()).isNotNull();
        }
    }

    @Nested
    class 재접속_복구_테스트 {

        /**
         * 재접속 스냅샷(ADR-0031 결정 8)은 별도 push 인프라 없이 기존 WS 복구({@link WsRecoveryService})에
         * 얹힌다 — {@code NunchiNotifier}가 {@code LoggingSimpMessagingTemplate}로 보내므로 stand/state가
         * 자동으로 복구 스트림에 저장된다. 재연결한 클라이언트는 {@code /api/rooms/{joinCode}/recovery}로
         * 이 메시지들을 재생해 PLAYING 스냅샷(currentNumber·stood)부터 상태를 복원한다.
         */
        @Test
        void 복구_스트림에서_PLAYING과_stand를_재생할_수_있다() {
            final var standResponses = session.subscribe(standUrl());
            final var stateResponses = session.subscribe(stateUrl());

            startNunchiGame();
            stateResponses.get(); // PLAYING

            session.send(pressCommandUrl());
            standResponses.get(); // stand 수신 = 복구 저장 완료(save가 broadcast 직전 실행)

            // lastId="0-0"(스트림 시작)부터 재생 — 시드 PLAYING과 이후 stand가 모두 복구 대상에 들어있다
            final List<String> destinations =
                    wsRecoveryService.getMessagesSince(joinCode.getValue(), "0-0").stream()
                            .map(RecoveryMessage::destination)
                            .toList();

            assertThat(destinations).contains(stateUrl(), standUrl());
        }
    }

    // ---- 헬퍼 ----

    private String stateUrl() {
        return String.format("/topic/room/%s/nunchi/state", joinCode.getValue());
    }

    private String standUrl() {
        return String.format("/topic/room/%s/nunchi/stand", joinCode.getValue());
    }

    private String pressCommandUrl() {
        return String.format("/app/room/%s/nunchi/press", joinCode.getValue());
    }

    /**
     * WS START 커맨드(Room 경유) 대신 :game 서비스를 직접 호출해 시작한다(BlockStacking IT와 동일 순서 —
     * startGame으로 READY→PLAYING 전이 후 start로 플로우 시작).
     */
    private void startNunchiGame() {
        gameSessionService.startGame(joinCode, host, gamers);
        nunchiService.start(joinCode.getValue(), host.getName());
    }
}
