package coffeeshout.blockstacking.ui;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.blockstacking.application.BlockStackingService;
import coffeeshout.blockstacking.application.response.BlockStackingProgressResponse;
import coffeeshout.blockstacking.application.response.BlockStackingStateResponse;
import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.blockstacking.domain.BlockStackingGameState;
import coffeeshout.blockstacking.domain.BlockStackingPlayerRankInfo;
import coffeeshout.blockstacking.ui.request.BlockStackingProgressRequest;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 타이밍 설정 (application-test.yml): prepare=500ms, playing=2000ms
 */
class BlockStackingIntegrationTest extends GameModuleWebSocketTest {

    private static final long PREPARE_MS = 500L;
    private static final long PLAYING_MS = 2000L;

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;
    TestStompSession session;

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    BlockStackingService blockStackingService;

    @BeforeEach
    void setUp(@Autowired JoinCodeGenerator joinCodeGenerator) throws Exception {
        joinCode = joinCodeGenerator.generate();
        host = GamerFixture.호스트_꾹이();
        gamers = GamerFixture.꾹이_루키_엠제이_한스();

        // GameSession을 READY 상태로 사전 구성한다 — Room 검증·영속을 거치지 않고 :game만으로 시작한다(ADR-0023).
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(new BlockStackingGame()));

        session = createSession(joinCode.getValue(), host.getName());
    }

    @Nested
    class 페이즈_전환_테스트 {

        @Test
        void 게임_페이즈가_PREPARE_PLAYING_DONE_순서로_전환된다() {
            final var stateResponses = session.subscribe(stateUrl());

            startBlockStackingGame();

            final BlockStackingStateResponse prepare = payloadAs(stateResponses.get(), BlockStackingStateResponse.class);
            final MessageResponse playingMessage = stateResponses.get();
            final MessageResponse doneMessage = stateResponses.get(4, TimeUnit.SECONDS);
            final BlockStackingStateResponse playing = payloadAs(playingMessage, BlockStackingStateResponse.class);
            final BlockStackingStateResponse done = payloadAs(doneMessage, BlockStackingStateResponse.class);

            assertThat(prepare.state()).isEqualTo(BlockStackingGameState.PREPARE);
            assertThat(playing.state()).isEqualTo(BlockStackingGameState.PLAYING);
            assertThat(playingMessage.duration()).isGreaterThanOrEqualTo(PREPARE_MS - 100);
            assertThat(playing.endTimeEpochMs()).isNotNull();
            assertThat(done.state()).isEqualTo(BlockStackingGameState.DONE);
            assertThat(doneMessage.duration()).isGreaterThanOrEqualTo(PLAYING_MS - 100);
            assertThat(done.endTimeEpochMs()).isNull();
        }

        @Test
        void PREPARE_단계에서_state_필드가_포함된_응답을_받는다() {
            final var stateResponses = session.subscribe(stateUrl());

            startBlockStackingGame();

            final BlockStackingStateResponse prepare = payloadAs(stateResponses.get(), BlockStackingStateResponse.class);

            assertThat(prepare.state()).isEqualTo(BlockStackingGameState.PREPARE);
        }

        @Test
        void 타이머_만료_후_게임이_완료된다() {
            final var stateResponses = session.subscribe(stateUrl());

            startBlockStackingGame();

            stateResponses.get(); // PREPARE
            stateResponses.get(); // PLAYING

            // playing(2000ms) 후 DONE 전환
            final MessageResponse doneMessage = stateResponses.get(4, TimeUnit.SECONDS);
            final BlockStackingStateResponse done = payloadAs(doneMessage, BlockStackingStateResponse.class);

            assertThat(done.state()).isEqualTo(BlockStackingGameState.DONE);

            // 핵심 검증: playing 제한 시간(2000ms) 이내에 done으로 전환되지 않음
            assertThat(doneMessage.duration())
                    .as("DONE 전환은 playing 제한 시간(%dms) 이후여야 합니다", PLAYING_MS)
                    .isGreaterThanOrEqualTo(PLAYING_MS - 100);
        }
    }

    @Nested
    class 진행_이벤트_테스트 {

        @Test
        void 유효한_블록_안착_이벤트가_랭킹을_업데이트한다() {
            final var stateResponses = session.subscribe(stateUrl());
            final var progressResponses = session.subscribe(progressUrl());

            startBlockStackingGame();

            stateResponses.get(); // PREPARE
            stateResponses.get(); // PLAYING

            session.send(progressCommandUrl(), progressCommand(1, 100.0, 85.0, 150.0));

            final BlockStackingProgressResponse progress =
                    payloadAs(progressResponses.get(), BlockStackingProgressResponse.class);

            final BlockStackingPlayerRankInfo 꾹이 = findByName(progress, "꾹이");
            assertThat(꾹이.floor()).isEqualTo(1);
        }

        @Test
        void 여러_플레이어의_진행을_랭킹_내림차순으로_브로드캐스트한다() throws Exception {
            final TestStompSession 루키세션 = createSession(joinCode.getValue(), "루키");

            final var stateResponses = session.subscribe(stateUrl());
            final var progressResponses = session.subscribe(progressUrl());

            startBlockStackingGame();

            stateResponses.get(); // PREPARE
            stateResponses.get(); // PLAYING

            // 꾹이: 2층, 루키: 1층
            session.send(progressCommandUrl(), progressCommand(1, 100.0, 85.0, 150.0));
            progressResponses.get(); // 꾹이 1층 브로드캐스트

            session.send(progressCommandUrl(), progressCommand(2, 100.0, 85.0, 135.0));
            progressResponses.get(); // 꾹이 2층 브로드캐스트

            루키세션.send(progressCommandUrl(), progressCommand(1, 100.0, 85.0, 135.0));
            final BlockStackingProgressResponse ranking =
                    payloadAs(progressResponses.get(), BlockStackingProgressResponse.class);

            // 꾹이(2층)가 루키(1층)보다 앞에 위치
            final int 꾹이위치 = indexOfName(ranking, "꾹이");
            final int 루키위치 = indexOfName(ranking, "루키");
            assertThat(꾹이위치).isLessThan(루키위치);
        }

        @Test
        void 유효하지_않은_overlap_이벤트는_브로드캐스트되지_않는다() {
            final var stateResponses = session.subscribe(stateUrl());
            final var progressResponses = session.subscribe(progressUrl());

            startBlockStackingGame();

            stateResponses.get(); // PREPARE
            stateResponses.get(); // PLAYING

            // overlap <= 0: movingBlockX=300으로 stackTop 범위(85~235) 완전 이탈
            session.send(progressCommandUrl(), progressCommand(1, 300.0, 85.0, 150.0));

            progressResponses.assertNoMessage();
        }

        @Test
        void 비연속적_floor_이벤트는_브로드캐스트되지_않는다() {
            final var stateResponses = session.subscribe(stateUrl());
            final var progressResponses = session.subscribe(progressUrl());

            startBlockStackingGame();

            stateResponses.get(); // PREPARE
            stateResponses.get(); // PLAYING

            // floor=1을 건너뛰고 floor=2 전송 → 무시됨
            session.send(progressCommandUrl(), progressCommand(2, 100.0, 85.0, 150.0));

            progressResponses.assertNoMessage();
        }
    }

    // ---- 헬퍼 메서드 ----

    private String stateUrl() {
        return String.format("/topic/room/%s/block-stacking/state", joinCode.getValue());
    }

    private String progressUrl() {
        return String.format("/topic/room/%s/block-stacking/progress", joinCode.getValue());
    }

    private String progressCommandUrl() {
        return String.format("/app/room/%s/block-stacking/progress", joinCode.getValue());
    }

    /**
     * WS START 커맨드(Room 검증·영속 경유) 대신 :game 서비스를 직접 호출해 게임을 시작한다.
     * {@code startGame}으로 READY→PLAYING 전이 후 {@code start}로 플로우를 스케줄한다(프로덕션 onGameStartReady와 동일 순서).
     */
    private void startBlockStackingGame() {
        gameSessionService.startGame(joinCode, host, gamers);
        blockStackingService.start(joinCode.getValue(), host.getName());
    }

    private BlockStackingProgressRequest progressCommand(
            int floor,
            double movingBlockX, double stackTopX, double stackTopWidth
    ) {
        return new BlockStackingProgressRequest(floor, movingBlockX, stackTopX, stackTopWidth);
    }

    private BlockStackingPlayerRankInfo findByName(BlockStackingProgressResponse response, String name) {
        return response.players().stream()
                .filter(player -> player.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("플레이어를 찾을 수 없습니다: " + name));
    }

    private int indexOfName(BlockStackingProgressResponse response, String name) {
        final List<BlockStackingPlayerRankInfo> players = response.players();
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).name().equals(name)) {
                return i;
            }
        }
        throw new AssertionError("플레이어를 찾을 수 없습니다: " + name);
    }
}
