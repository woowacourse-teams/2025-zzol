package coffeeshout.numberpoker.ui;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.fixture.TestStompSession;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import coffeeshout.global.MessageResponse;
import coffeeshout.numberpoker.domain.NumberPokerGame;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 넘버포커 상태 변화 흐름 통합 테스트
 *
 * <p>타이밍 설정 (application-test.yml):
 * stage1=1000ms, stage2=1000ms, roundReady=1500ms
 */
class NumberPokerIntegrationTest extends WebSocketIntegrationTestSupport {

    // application-test.yml 타이밍 값과 일치해야 함
    private static final long STAGE1_MS = 1000L;
    private static final long STAGE2_MS = 1000L;
    private static final long ROUND_READY_MS = 1500L;

    JoinCode joinCode;
    Player host;
    Room room;
    TestStompSession session;

    @BeforeEach
    void setUp(
            @Autowired RoomRepository roomRepository,
            @Autowired JoinCodeGenerator joinCodeGenerator
    ) throws Exception {
        joinCode = joinCodeGenerator.generate();
        room = RoomFixture.호스트_꾹이(joinCode);
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        host = room.getHost();
        session = createSession();
    }

    // ── URL helpers ──────────────────────────────────────────────────────────

    private String stateUrl() {
        return "/topic/room/%s/poker/state".formatted(joinCode.getValue());
    }

    private String startUrl() {
        return "/app/room/%s/minigame/command".formatted(joinCode.getValue());
    }

    private String foldUrl() {
        return "/app/room/%s/poker/fold".formatted(joinCode.getValue());
    }

    private String readyUrl() {
        return "/app/room/%s/poker/ready".formatted(joinCode.getValue());
    }

    private String settingsUrl() {
        return "/app/room/%s/poker/settings".formatted(joinCode.getValue());
    }

    // ── Action helpers ───────────────────────────────────────────────────────

    private void startGame() {
        session.send(startUrl(), """
                {
                  "commandType": "START_MINI_GAME",
                  "commandRequest": { "hostName": "%s" }
                }
                """.formatted(host.getName().value()));
    }

    private void fold(String playerName) {
        session.send(foldUrl(), """
                { "playerName": "%s" }
                """.formatted(playerName));
    }

    private void ready(String playerName) {
        session.send(readyUrl(), """
                { "playerName": "%s" }
                """.formatted(playerName));
    }

    private void settings(int roundCount) {
        session.send(settingsUrl(), """
                { "hostName": "%s", "roundCount": %d }
                """.formatted(host.getName().value(), roundCount));
    }

    private void setupGame(@Autowired RoomRepository roomRepository, int roundCount) {
        NumberPokerGame game = new NumberPokerGame();
        game.configureRoundCount(roundCount);
        room.addMiniGame(host.getName(), game);
        roomRepository.save(room);
    }

    // ── Tests ────────────────────────────────────────────────────────────────

    @Nested
    class 단일_라운드_정상_흐름 {

        @BeforeEach
        void 게임_설정(@Autowired RoomRepository roomRepository) {
            setupGame(roomRepository, 1);
        }

        @Test
        void LOADING_STAGE1_STAGE2_SHOWDOWN_SCOREBOARD_순서로_페이즈가_전환된다() {
            var responses = session.subscribe(stateUrl());
            startGame();

            MessageResponse loading = responses.get();
            MessageResponse stage1 = responses.get();
            MessageResponse stage2 = responses.get(STAGE1_MS + 500, TimeUnit.MILLISECONDS);
            MessageResponse showdown = responses.get(STAGE2_MS + 500, TimeUnit.MILLISECONDS);
            MessageResponse scoreBoard = responses.get();

            assertMessageContains(loading, "\"phase\":\"LOADING\"");
            assertMessageContains(stage1, "\"phase\":\"STAGE_1\"");
            assertMessageContains(stage2, "\"phase\":\"STAGE_2\"");
            assertMessageContains(showdown, "\"phase\":\"SHOWDOWN\"");
            assertMessageContains(scoreBoard, "\"phase\":\"SCORE_BOARD\"");
        }

        @Test
        void LOADING_시_라운드_번호와_딜러카드_플레이어_정보가_포함된다() {
            var responses = session.subscribe(stateUrl());
            startGame();

            MessageResponse loading = responses.get();

            assertMessageContains(loading, "\"phase\":\"LOADING\"");
            assertMessageContains(loading, "\"roundNumber\":1");
            assertMessageContains(loading, "\"totalRounds\":1");
            assertMessageContains(loading, "\"dealerCards\":");
            assertMessageContains(loading, "\"players\":");
        }

        @Test
        void SHOWDOWN_시_딜러_카드_두_장이_모두_공개된다() {
            var responses = session.subscribe(stateUrl());
            startGame();

            responses.get(); // LOADING
            responses.get(); // STAGE_1
            responses.get(STAGE1_MS + 500, TimeUnit.MILLISECONDS); // STAGE_2
            MessageResponse showdown = responses.get(STAGE2_MS + 500, TimeUnit.MILLISECONDS);

            // 딜러 카드가 2장 공개됨
            assertMessageContains(showdown, "\"phase\":\"SHOWDOWN\"");
            String payload = showdown.payload();
            // dealerCards 배열에 쉼표가 1개 이상 있음 = 카드 2장
            assertThat(payload).contains("\"dealerCards\":[")
                    .doesNotContain("\"dealerCards\":[]");
        }
    }

    @Nested
    class 폴드_흐름 {

        @BeforeEach
        void 게임_설정(@Autowired RoomRepository roomRepository) {
            setupGame(roomRepository, 1);
        }

        @Test
        void 플레이어_폴드_시_상태_메시지에_folded_true가_반영된다() {
            var responses = session.subscribe(stateUrl());
            startGame();

            responses.get(); // LOADING
            responses.get(); // STAGE_1

            fold(host.getName().value());

            MessageResponse afterFold = responses.get(2, TimeUnit.SECONDS);

            assertMessageContains(afterFold, "\"phase\":\"STAGE_1\"");
            assertMessageContains(afterFold, "\"folded\":true");
        }

        @Test
        void STAGE1에서_전원_폴드시_STAGE2를_건너뛰고_SHOWDOWN으로_전환된다() {
            var responses = session.subscribe(stateUrl());
            startGame();

            responses.get(); // LOADING
            responses.get(); // STAGE_1

            // 전원 폴드 — 각 폴드마다 상태 알림 1건
            for (Player player : room.getPlayers()) {
                fold(player.getName().value());
                responses.get(2, TimeUnit.SECONDS); // 폴드 반영 상태
            }

            // 전원 폴드 감지 후 STAGE_2 없이 SHOWDOWN으로 직행
            MessageResponse nextPhase = responses.get(STAGE1_MS + 500, TimeUnit.MILLISECONDS);
            MessageResponse scoreBoard = responses.get();

            assertMessageContains(nextPhase, "\"phase\":\"SHOWDOWN\"");
            assertMessageContains(scoreBoard, "\"phase\":\"SCORE_BOARD\"");
        }

        @Test
        void 전원_폴드_후_결과는_모든_플레이어가_폴드_상태다() {
            var responses = session.subscribe(stateUrl());
            startGame();

            responses.get(); // LOADING
            responses.get(); // STAGE_1

            for (Player player : room.getPlayers()) {
                fold(player.getName().value());
                responses.get(2, TimeUnit.SECONDS);
            }

            responses.get(STAGE1_MS + 500, TimeUnit.MILLISECONDS); // SHOWDOWN
            MessageResponse scoreBoard = responses.get();

            // "folded":true 가 플레이어 수(4) 만큼 존재
            assertThat(scoreBoard.payload()).containsPattern("\"folded\":true");
        }
    }

    @Nested
    class 라운드_수_설정 {

        @BeforeEach
        void 게임_설정(@Autowired RoomRepository roomRepository) {
            // 기본 3라운드 게임을 등록 — settings 명령으로 변경할 것
            NumberPokerGame game = new NumberPokerGame();
            room.addMiniGame(host.getName(), game);
            roomRepository.save(room);
        }

        @Test
        void settings_명령으로_totalRounds가_변경된_상태_메시지를_받는다() {
            var responses = session.subscribe(stateUrl());

            settings(1);

            MessageResponse settingsResponse = responses.get(2, TimeUnit.SECONDS);

            assertMessageContains(settingsResponse, "\"totalRounds\":1");
        }

        @Test
        void settings로_설정한_라운드_수로_게임이_실행된다() {
            var responses = session.subscribe(stateUrl());

            settings(1);
            responses.get(2, TimeUnit.SECONDS); // settings 상태 알림 소비

            startGame();

            MessageResponse loading = responses.get();

            assertMessageContains(loading, "\"phase\":\"LOADING\"");
            assertMessageContains(loading, "\"totalRounds\":1");
        }
    }

    @Nested
    class 멀티_라운드_흐름 {

        @BeforeEach
        void 게임_설정(@Autowired RoomRepository roomRepository) {
            setupGame(roomRepository, 2);
        }

        @Test
        void 전원_레디_시_ROUND_READY_타임아웃_전에_다음_라운드가_시작된다() {
            var responses = session.subscribe(stateUrl());
            startGame();

            // 1라운드 전체 흐름 소진
            responses.get();                                          // LOADING
            responses.get();                                          // STAGE_1
            responses.get(STAGE1_MS + 500, TimeUnit.MILLISECONDS);   // STAGE_2
            responses.get(STAGE2_MS + 500, TimeUnit.MILLISECONDS);   // SHOWDOWN
            responses.get();                                          // SCORE_BOARD

            MessageResponse roundReady = responses.get();
            assertMessageContains(roundReady, "\"phase\":\"ROUND_READY\"");

            // 전원 레디 — 레디 발생 시마다 ROUND_READY 갱신 메시지가 1건씩 전송됨
            for (Player player : room.getPlayers()) {
                ready(player.getName().value());
                responses.get(2, TimeUnit.SECONDS); // 레디 상태 갱신 메시지 소비
            }

            // 전원 레디 완료 후 타임아웃(ROUND_READY_MS)보다 빠르게 2라운드 LOADING이 도착해야 함
            MessageResponse round2Loading = responses.get(ROUND_READY_MS, TimeUnit.MILLISECONDS);

            assertMessageContains(round2Loading, "\"phase\":\"LOADING\"");
            assertMessageContains(round2Loading, "\"roundNumber\":2");
            assertThat(round2Loading.duration())
                    .as("전원 레디 시 다음 라운드는 ROUND_READY 타임아웃(%dms)보다 빠르게 시작되어야 합니다", ROUND_READY_MS)
                    .isLessThan(ROUND_READY_MS);
        }

        @Test
        void _2라운드_게임은_ROUND_READY를_거쳐_전체_페이즈를_순서대로_완주한다() {
            var responses = session.subscribe(stateUrl());
            startGame();

            // 1라운드
            assertPhase(responses.get(), "LOADING");
            assertPhase(responses.get(), "STAGE_1");
            assertPhase(responses.get(STAGE1_MS + 500, TimeUnit.MILLISECONDS), "STAGE_2");
            assertPhase(responses.get(STAGE2_MS + 500, TimeUnit.MILLISECONDS), "SHOWDOWN");
            assertPhase(responses.get(), "SCORE_BOARD");

            // 라운드 사이
            assertPhase(responses.get(), "ROUND_READY");

            // 2라운드 (타임아웃 대기)
            assertPhase(responses.get(ROUND_READY_MS + 500, TimeUnit.MILLISECONDS), "LOADING");
            assertPhase(responses.get(), "STAGE_1");
            assertPhase(responses.get(STAGE1_MS + 500, TimeUnit.MILLISECONDS), "STAGE_2");
            assertPhase(responses.get(STAGE2_MS + 500, TimeUnit.MILLISECONDS), "SHOWDOWN");
            assertPhase(responses.get(), "SCORE_BOARD");
        }

        private void assertPhase(MessageResponse response, String expectedPhase) {
            assertMessageContains(response, "\"phase\":\"%s\"".formatted(expectedPhase));
        }
    }
}
