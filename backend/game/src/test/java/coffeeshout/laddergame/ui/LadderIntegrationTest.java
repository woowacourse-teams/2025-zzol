package coffeeshout.laddergame.ui;

import static org.assertj.core.api.Assertions.assertThat;

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
 * 타이밍 설정 (application-test.yml): description=500ms, prepare=500ms, drawing=1000ms, result=500ms
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

        // GameSession을 READY 상태로 사전 구성한다 — Room 검증·영속을 거치지 않고 :game만으로 시작한다(ADR-0023).
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(new LadderGame()));

        session = createSession(joinCode.getValue(), host.getName());
    }

    @Nested
    class 상태_전환_테스트 {

        @Test
        void 게임_페이즈가_DESCRIPTION_PREPARE_DRAWING_RESULT_DONE_순서로_전환된다() {
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
                softly.assertThat(drawing.state()).isEqualTo(LadderGameState.DRAWING);
                softly.assertThat(result.state()).isEqualTo(LadderGameState.RESULT);
                softly.assertThat(done.state()).isEqualTo(LadderGameState.DONE);
            });
        }

        @Test
        void PREPARE_응답에_poles와_bottomRanks가_포함된다() {
            final var stateResponses = session.subscribe(stateUrl());

            startLadderGame();

            stateResponses.get(); // DESCRIPTION
            final LadderStateResponse prepare = payloadAs(stateResponses.get(2, TimeUnit.SECONDS), LadderStateResponse.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(prepare.state()).isEqualTo(LadderGameState.PREPARE);
                softly.assertThat(prepare.poles()).isNotEmpty();
                softly.assertThat(prepare.bottomRanks()).isNotEmpty();
            });
        }

        @Test
        void DRAWING_응답에_endTimeEpochMs가_포함된다() {
            final var stateResponses = session.subscribe(stateUrl());

            startLadderGame();

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            final LadderStateResponse drawing = payloadAs(stateResponses.get(2, TimeUnit.SECONDS), LadderStateResponse.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(drawing.state()).isEqualTo(LadderGameState.DRAWING);
                softly.assertThat(drawing.endTimeEpochMs()).isNotNull();
            });
        }

        @Test
        void RESULT_응답에_rankings와_animationDurationMs가_포함된다() {
            final var stateResponses = session.subscribe(stateUrl());

            startLadderGame();

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING
            final LadderStateResponse result = payloadAs(stateResponses.get(3, TimeUnit.SECONDS), LadderStateResponse.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(result.state()).isEqualTo(LadderGameState.RESULT);
                softly.assertThat(result.rankings()).isNotEmpty();
                softly.assertThat(result.animationDurationMs()).isEqualTo(500);
            });
        }

        @Test
        void DONE_응답에는_state_필드만_포함된다() {
            final var stateResponses = session.subscribe(stateUrl());

            startLadderGame();

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING
            stateResponses.get(3, TimeUnit.SECONDS); // RESULT
            final LadderStateResponse done = payloadAs(stateResponses.get(2, TimeUnit.SECONDS), LadderStateResponse.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(done.state()).isEqualTo(LadderGameState.DONE);
                softly.assertThat(done.poles()).isNull();
                softly.assertThat(done.rankings()).isNull();
            });
        }
    }

    @Nested
    class 선_긋기_테스트 {

        @Test
        void DRAWING_중_선_긋기_요청이_line_토픽으로_브로드캐스트된다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());
            final var lineResponses = session.subscribe(lineUrl());

            startLadderGame();

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

            session.send(drawCommandUrl(), drawRequest(0));

            final LadderLineResponse line = payloadAs(lineResponses.get(), LadderLineResponse.class);

            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(line.playerName()).isEqualTo("꾹이");
                softly.assertThat(line.segmentIndex()).isEqualTo(0);
                softly.assertThat(line.row()).isPositive();
            });
        }

        @Test
        void 브로드캐스트된_선의_row는_양수다() throws Exception {
            final var stateResponses = session.subscribe(stateUrl());
            final var lineResponses = session.subscribe(lineUrl());

            startLadderGame();

            stateResponses.get(); // DESCRIPTION
            stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
            stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

            session.send(drawCommandUrl(), drawRequest(0));

            final LadderLineResponse line = payloadAs(lineResponses.get(), LadderLineResponse.class);

            assertThat(line.row()).isPositive();
        }

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
        void 여러_플레이어가_각자_선을_그을_수_있다() throws Exception {
            try (final TestStompSession 루키세션 = createSession(joinCode.getValue(), "루키")) {
                final var stateResponses = session.subscribe(stateUrl());
                final var lineResponses = session.subscribe(lineUrl());

                startLadderGame();

                stateResponses.get(); // DESCRIPTION
                stateResponses.get(2, TimeUnit.SECONDS); // PREPARE
                stateResponses.get(2, TimeUnit.SECONDS); // DRAWING

                session.send(drawCommandUrl(), drawRequest(0));
                lineResponses.get(); // 꾹이 선 브로드캐스트

                루키세션.send(drawCommandUrl(), drawRequest(2));
                final LadderLineResponse 루키라인 = payloadAs(lineResponses.get(), LadderLineResponse.class);

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(루키라인.playerName()).isEqualTo("루키");
                    softly.assertThat(루키라인.segmentIndex()).isEqualTo(2);
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
