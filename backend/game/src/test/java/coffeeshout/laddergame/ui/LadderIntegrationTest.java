package coffeeshout.laddergame.ui;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.laddergame.application.LadderService;
import coffeeshout.laddergame.application.response.LadderLineResponse;
import coffeeshout.laddergame.application.response.LadderStateResponse;
import coffeeshout.laddergame.domain.LadderGame;
import coffeeshout.laddergame.domain.LadderGameState;
import coffeeshout.laddergame.ui.request.LadderDrawRequest;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.support.TestStompSession;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 타이밍 설정 (application-test-game.yml): description=500ms, prepare=500ms, drawing=500ms(+grace 300ms), result=500ms
 */
class LadderIntegrationTest extends GameModuleWebSocketTest {

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;
    TestStompSession session;

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    LadderService ladderService;

    @BeforeEach
    void setUp(@Autowired JoinCodeGenerator joinCodeGenerator) throws Exception {
        joinCode = joinCodeGenerator.generate();
        host = GamerFixture.호스트_꾹이();
        gamers = GamerFixture.꾹이_루키_엠제이_한스();

        // GameSession을 READY 상태로 사전 구성한다 — Room 검증·영속을 거치지 않고 :game만으로 시작한다(ADR-0025).
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(new LadderGame()));

        session = createSession(joinCode.getValue(), host.getName());
    }

    @Nested
    class 상태_전환_테스트 {

        /**
         * 페이즈 전환은 한 번의 게임 플로우로 모든 상태 브로드캐스트가 순서대로 도착하므로,
         * 페이즈별 페이로드 검증을 단일 플로우에서 SoftAssertions로 한꺼번에 확인한다.
         * (페이즈마다 게임을 재시작하면 phase 대기가 중복되어 IT 시간이 불필요하게 늘어난다.)
         */
        @Test
        void 페이즈가_순서대로_전환되며_각_상태가_올바른_페이로드를_브로드캐스트한다() {
            final var stateResponses = session.subscribe(stateUrl());

            startLadderGame();

            final LadderStateResponse description = payloadAs(stateResponses.get(), LadderStateResponse.class);
            final LadderStateResponse prepare = payloadAs(stateResponses.get(2, TimeUnit.SECONDS), LadderStateResponse.class);
            final LadderStateResponse drawing = payloadAs(stateResponses.get(2, TimeUnit.SECONDS), LadderStateResponse.class);
            final LadderStateResponse result = payloadAs(stateResponses.get(3, TimeUnit.SECONDS), LadderStateResponse.class);
            final LadderStateResponse done = payloadAs(stateResponses.get(2, TimeUnit.SECONDS), LadderStateResponse.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(description.state()).isEqualTo(LadderGameState.DESCRIPTION);

                softly.assertThat(prepare.state()).isEqualTo(LadderGameState.PREPARE);
                softly.assertThat(prepare.poles()).isNotEmpty();
                softly.assertThat(prepare.bottomRanks()).isNotEmpty();

                softly.assertThat(drawing.state()).isEqualTo(LadderGameState.DRAWING);
                softly.assertThat(drawing.endTimeEpochMs()).isNotNull();

                softly.assertThat(result.state()).isEqualTo(LadderGameState.RESULT);
                softly.assertThat(result.rankings()).isNotEmpty();
                softly.assertThat(result.animationDurationMs()).isEqualTo(500);

                softly.assertThat(done.state()).isEqualTo(LadderGameState.DONE);
                softly.assertThat(done.poles()).isNull();
                softly.assertThat(done.rankings()).isNull();
            });
        }
    }

    @Nested
    class 선_긋기_테스트 {

        @Test
        void 이미_선을_그은_플레이어의_재요청은_브로드캐스트되지_않는다() {
            final var stateResponses = session.subscribe(stateUrl());
            final var lineResponses = session.subscribe(lineUrl());

            startLadderGame();

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

            session.send(drawCommandUrl(), drawRequest(0)); // 첫 번째 요청
            lineResponses.get(); // 브로드캐스트 수신

            session.send(drawCommandUrl(), drawRequest(1)); // 중복 요청

            lineResponses.assertNoMessage();
        }

        @Test
        void 유효하지_않은_segmentIndex_요청은_브로드캐스트되지_않는다() {
            final var stateResponses = session.subscribe(stateUrl());
            final var lineResponses = session.subscribe(lineUrl());

            startLadderGame();

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

            // 4명 플레이어 → 유효 구간 0~2, 3은 유효하지 않음
            session.send(drawCommandUrl(), drawRequest(3));

            lineResponses.assertNoMessage();
        }

        @Test
        void 여러_플레이어가_각자_선을_그으면_각_선이_line_토픽으로_브로드캐스트된다() throws Exception {
            try (final TestStompSession 루키세션 = createSession(joinCode.getValue(), "루키")) {
                final var stateResponses = session.subscribe(stateUrl());
                final var lineResponses = session.subscribe(lineUrl());

                startLadderGame();

                stateResponses.get(); // DESCRIPTION
                stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
                stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

                session.send(drawCommandUrl(), drawRequest(0));
                final LadderLineResponse 꾹이라인 = payloadAs(lineResponses.get(), LadderLineResponse.class);

                루키세션.send(drawCommandUrl(), drawRequest(2));
                final LadderLineResponse 루키라인 = payloadAs(lineResponses.get(), LadderLineResponse.class);

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(꾹이라인.playerName()).isEqualTo("꾹이");
                    softly.assertThat(꾹이라인.segmentIndex()).isEqualTo(0);
                    softly.assertThat(꾹이라인.row()).isPositive();
                    softly.assertThat(루키라인.playerName()).isEqualTo("루키");
                    softly.assertThat(루키라인.segmentIndex()).isEqualTo(2);
                    softly.assertThat(루키라인.row()).isPositive();
                });
            }
        }
    }

    // ---- 헬퍼 메서드 ----

    private String stateUrl() {
        return String.format("/topic/room/%s/ladder/state", joinCode.getValue());
    }

    private String lineUrl() {
        return String.format("/topic/room/%s/ladder/line", joinCode.getValue());
    }

    private String drawCommandUrl() {
        return String.format("/app/room/%s/ladder/draw", joinCode.getValue());
    }

    /**
     * WS START 커맨드(Room 검증·영속 경유) 대신 :game 서비스를 직접 호출해 게임을 시작한다.
     * {@code startGame}으로 READY→PLAYING 전이 후 {@code start}로 플로우를 스케줄한다(프로덕션 onGameStartReady와 동일 순서).
     */
    private void startLadderGame() {
        gameSessionService.startGame(joinCode, host, gamers);
        ladderService.start(joinCode.getValue(), host.getName());
    }

    private LadderDrawRequest drawRequest(int segmentIndex) {
        return new LadderDrawRequest(segmentIndex);
    }
}
