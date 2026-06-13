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
import org.assertj.core.api.SoftAssertions;
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

        // GameSession을 READY 상태로 사전 구성한다 — Room 검증·영속을 거치지 않고 :game만으로 시작한다(ADR-0025).
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
            // DONE 전환은 playing 제한 시간(2000ms) 이후여야 한다
            assertThat(doneMessage.duration()).isGreaterThanOrEqualTo(PLAYING_MS - 100);
            assertThat(done.endTimeEpochMs()).isNull();
        }
    }

    @Nested
    class 진행_이벤트_테스트 {

        /**
         * 유효한 안착이 층수를 갱신하고, 여러 플레이어의 진행이 랭킹 내림차순으로 정렬되는지를
         * 한 번의 PLAYING 플로우에서 함께 검증한다. (개별 안착 검증과 랭킹 검증이 같은 진행 시퀀스를 공유)
         */
        @Test
        void 유효한_안착이_층수를_갱신하고_여러_플레이어가_랭킹_내림차순으로_브로드캐스트된다() throws Exception {
            final TestStompSession 루키세션 = createSession(joinCode.getValue(), "루키");

            final var stateResponses = session.subscribe(stateUrl());
            final var progressResponses = session.subscribe(progressUrl());

            startBlockStackingGame();

            stateResponses.get(); // PREPARE
            stateResponses.get(); // PLAYING

            // 꾹이 1층
            session.send(progressCommandUrl(), progressCommand(1, 100.0, 85.0, 150.0));
            final BlockStackingProgressResponse 꾹이1층 =
                    payloadAs(progressResponses.get(), BlockStackingProgressResponse.class);

            // 꾹이 2층
            session.send(progressCommandUrl(), progressCommand(2, 100.0, 85.0, 135.0));
            progressResponses.get(); // 꾹이 2층 브로드캐스트

            // 루키 1층
            루키세션.send(progressCommandUrl(), progressCommand(1, 100.0, 85.0, 135.0));
            final BlockStackingProgressResponse ranking =
                    payloadAs(progressResponses.get(), BlockStackingProgressResponse.class);

            SoftAssertions.assertSoftly(softly -> {
                // 유효한 안착이 층수를 1로 갱신
                softly.assertThat(findByName(꾹이1층, "꾹이").floor()).isEqualTo(1);
                // 꾹이(2층)가 루키(1층)보다 앞에 위치
                softly.assertThat(indexOfName(ranking, "꾹이")).isLessThan(indexOfName(ranking, "루키"));
            });
        }

        @Test
        void 유효하지_않은_진행_이벤트는_브로드캐스트되지_않는다() {
            final var stateResponses = session.subscribe(stateUrl());
            final var progressResponses = session.subscribe(progressUrl());

            startBlockStackingGame();

            stateResponses.get(); // PREPARE
            stateResponses.get(); // PLAYING

            // (1) overlap <= 0: movingBlockX=300으로 stackTop 범위(85~235) 완전 이탈
            session.send(progressCommandUrl(), progressCommand(1, 300.0, 85.0, 150.0));
            // (2) 비연속 floor: floor=1을 건너뛰고 floor=2 전송 (앞의 이벤트가 무시되어 currentFloor=0 유지)
            session.send(progressCommandUrl(), progressCommand(2, 100.0, 85.0, 150.0));

            // 둘 다 무시되어 progress 토픽으로 어떤 브로드캐스트도 발행되지 않는다
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
        // 구독 등록 완료 보장 후 시작 — 등록 전 첫 브로드캐스트 유실(subscribe→publish 레이스) 방지 (#1410)
        session.awaitSubscribed();
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
