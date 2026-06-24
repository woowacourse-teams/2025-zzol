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
import coffeeshout.support.TestStompSession.MessageCollector;
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
 * <p>타이밍(application-test-game.yml): description=500ms, ready=500ms, number-window=300ms,
 * collision-cooldown=500ms, idle-timeout=500ms, hard-cap=3000ms. 시작 시 DESCRIPTION → READY가
 * 먼저 나가고 description+ready(1000ms) 뒤 PLAYING으로 전이하므로, PLAYING 이후를 보는 테스트는
 * {@link #awaitState}로 앞선 DESCRIPTION·READY를 흘려보낸 뒤 단언한다.
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
        void 시작하면_DESCRIPTION_READY_PLAYING_순으로_상태를_브로드캐스트한다() {
            final MessageCollector stateResponses = session.subscribe(stateUrl());

            startNunchiGame();

            // 첫 메시지는 규칙 설명(DESCRIPTION) — serverNowEpochMs만 싣는다(결정 9)
            final NunchiStateResponse description =
                    payloadAs(stateResponses.get(), NunchiStateResponse.class);
            assertThat(description.state()).isEqualTo(NunchiState.DESCRIPTION);
            assertThat(description.serverNowEpochMs()).isNotNull();
            assertThat(description.playStartEpochMs()).isNull();

            // description(500ms) 뒤 READY — playStartEpochMs로 PLAYING 시작 시각을 싣는다(결정 8·9)
            final NunchiStateResponse ready = awaitState(stateResponses, NunchiState.READY);
            assertThat(ready.serverNowEpochMs()).isNotNull();
            assertThat(ready.playStartEpochMs()).isNotNull();

            // ready(500ms) 뒤 PLAYING으로 전이(중복 스냅샷이 끼면 흘려보낸다)
            final NunchiStateResponse playing = awaitState(stateResponses, NunchiState.PLAYING);
            assertThat(playing.currentNumber()).isEqualTo(1);
            assertThat(playing.serverNowEpochMs()).isNotNull();
            assertThat(playing.idleDeadlineEpochMs()).isNotNull();
            assertThat(playing.hardCapEpochMs()).isNotNull();
        }

        /**
         * 시작 직후 단발로 나가는 DESCRIPTION을 놓친 늦은 구독자도 결국 PLAYING을 받아야 한다.
         *
         * <p>프로덕션 로그(joinCode=HJ6G)에서 확인됐던 start 레이스: {@code /nunchi/state} 구독이
         * {@code 미니게임 시작됨}보다 늦게 등록돼 시작 broadcast를 구독자 0명에게 보내 유실하고, 이후 재발행이
         * 없어 FE가 멈췄다. 과거엔 구독 스냅샷 핸들러로 막았으나, DESCRIPTION 단계 도입으로 더 단순한 구조가
         * 됐다 — 시작 broadcast(DESCRIPTION)를 놓쳐도 {@code onDescriptionEnd}(→ READY)·{@code onReadyEnd}
         * (→ PLAYING)가 입력과 무관하게 각 단계 종료 시점(여기선 500ms·1000ms)에 다음 상태를 <b>자동 재발행</b>하므로,
         * 그 전에 구독만 하면 PLAYING을 받는다.
         *
         * <p>즉 이 자동 재발행이 별도 스냅샷 인프라 없이 start 레이스를 흡수한다(게임 중 새로고침 재접속은
         * {@link WsRecoveryService}가 따로 커버 — 아래 재접속 복구 테스트).
         */
        @Test
        void 시작_후_늦게_구독해도_description_종료_자동재발행으로_PLAYING을_받는다() {
            startNunchiGame(); // 구독 전에 시작 — 단발 DESCRIPTION은 구독자 0명에게 전달돼 유실(레이스 재현)

            final MessageCollector stateResponses = session.subscribe(stateUrl());

            // description→ready 종료(1000ms) 시 onReadyEnd가 자동 재발행한 PLAYING을 받는다
            final NunchiStateResponse playing = awaitState(stateResponses, NunchiState.PLAYING);
            assertThat(playing.currentNumber()).isEqualTo(1);
            assertThat(playing.idleDeadlineEpochMs()).isNotNull();
            assertThat(playing.hardCapEpochMs()).isNotNull();
        }
    }

    @Nested
    class 입력_브로드캐스트_테스트 {

        @Test
        void 단독_press는_stand를_브로드캐스트한다() {
            final MessageCollector standResponses = session.subscribe(standUrl());
            final MessageCollector stateResponses = session.subscribe(stateUrl());

            startNunchiGame();
            awaitState(stateResponses, NunchiState.PLAYING); // DESCRIPTION→READY→PLAYING 후에야 입력 수락

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
            final MessageCollector stateResponses = session.subscribe(stateUrl());

            startNunchiGame();
            awaitState(stateResponses, NunchiState.PLAYING); // DESCRIPTION→READY→PLAYING 후에야 입력 수락

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
            final MessageCollector standResponses = session.subscribe(standUrl());
            final MessageCollector stateResponses = session.subscribe(stateUrl());

            startNunchiGame();
            awaitState(stateResponses, NunchiState.PLAYING); // DESCRIPTION→READY→PLAYING 후에야 입력 수락

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

    @Nested
    class 라운드_라이프사이클_테스트 {

        /**
         * 가장 결정적인 종료 경로 — 아무도 누르지 않으면 idle 타임아웃(500ms)만으로 끝난다.
         * 상태 토픽은 DESCRIPTION → READY → PLAYING(1) → DONE 순으로 받는다(PLAYING 이후 중간 전이 없음).
         */
        @Test
        void 아무도_누르지_않으면_idle_타임아웃으로_DONE이_된다() {
            final MessageCollector stateResponses = session.subscribe(stateUrl());

            startNunchiGame();

            awaitState(stateResponses, NunchiState.PLAYING);

            final NunchiStateResponse done = awaitState(stateResponses, NunchiState.DONE);
            assertThat(done.state()).isEqualTo(NunchiState.DONE);
        }

        /**
         * 충돌 후 재개 상태 머신(ADR-0031 결정 6): PLAYING(1) → COLLISION_COOLDOWN → PLAYING(1, reset)
         * → DONE. 충돌 두 명(꾹이·루키)은 OUT되고 쿨다운(500ms)이 끝나면 <b>같은 번호(1)로</b> 재개되며,
         * 남은 미입력자(엠제이·한스)가 끝내 누르지 않아 idle(500ms)로 종료된다.
         */
        @Test
        void 충돌_후_쿨다운이_끝나면_같은_번호로_재개했다가_idle로_종료한다() throws Exception {
            final TestStompSession 루키세션 = createSession(joinCode.getValue(), "루키");
            final MessageCollector stateResponses = session.subscribe(stateUrl());

            startNunchiGame();

            final NunchiStateResponse playing = awaitState(stateResponses, NunchiState.PLAYING);
            assertThat(playing.currentNumber()).isEqualTo(1);

            // 윈도우(300ms) 안에 두 명이 누르면 충돌 → COLLISION_COOLDOWN
            session.send(pressCommandUrl());
            루키세션.send(pressCommandUrl());

            final NunchiStateResponse cooldown = awaitState(stateResponses, NunchiState.COLLISION_COOLDOWN);
            assertThat(cooldown.collided()).contains(host.getName(), "루키");

            // 쿨다운(500ms) 종료 → 충돌 번호(1)로 PLAYING 재개(전진 안 함)
            final NunchiStateResponse resumed = awaitState(stateResponses, NunchiState.PLAYING);
            assertThat(resumed.currentNumber()).isEqualTo(1);

            // 남은 미입력자가 끝내 안 누르면 idle(500ms)로 DONE
            awaitState(stateResponses, NunchiState.DONE);
        }

        /**
         * 예외 케이스의 통합 검증(ADR-0031 결정 1·N6): 이미 누른 사람의 재press는 도메인이 IGNORED로
         * 흡수해 <b>추가 stand 브로드캐스트가 없다</b>(에러 응답·재전송 없음). 상태가 전진하지 않음을
         * stand 토픽에 더 이상 메시지가 오지 않는 것으로 확인한다(idle→DONE 등 후속은 state 토픽 소관).
         */
        @Test
        void 이미_누른_사람의_재press는_stand를_다시_브로드캐스트하지_않는다() {
            final MessageCollector standResponses = session.subscribe(standUrl());
            final MessageCollector stateResponses = session.subscribe(stateUrl());

            startNunchiGame();
            awaitState(stateResponses, NunchiState.PLAYING); // DESCRIPTION→READY→PLAYING 후에야 입력 수락

            session.send(pressCommandUrl()); // 호스트 첫 press
            final NunchiStandResponse first =
                    payloadAs(standResponses.get(), NunchiStandResponse.class);
            assertThat(first.name()).isEqualTo(host.getName());

            session.send(pressCommandUrl()); // 같은 사람의 재press → IGNORED

            standResponses.assertNoMessage();
        }
    }

    // ---- 헬퍼 ----

    /**
     * 상태 토픽에서 {@code target} 상태 메시지가 올 때까지 앞선 메시지를 흘려보내고 그 메시지를 돌려준다.
     * 시작 DESCRIPTION을 건너뛰고 PLAYING·COLLISION_COOLDOWN·DONE 등 이후 상태를 결정적으로 기다리는 데
     * 쓴다(positional read 회피 — flaky 방지).
     */
    private NunchiStateResponse awaitState(MessageCollector stateResponses, NunchiState target) {
        for (int i = 0; i < 8; i++) {
            final NunchiStateResponse response =
                    payloadAs(stateResponses.get(2, TimeUnit.SECONDS), NunchiStateResponse.class);
            if (response.state() == target) {
                return response;
            }
        }
        throw new AssertionError(target + " 상태를 받지 못했습니다");
    }

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
