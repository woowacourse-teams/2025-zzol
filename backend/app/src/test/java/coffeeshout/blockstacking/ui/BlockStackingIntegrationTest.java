package coffeeshout.blockstacking.ui;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.app.WebSocketIntegrationTestSupport;
import coffeeshout.support.MessageResponse;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 타이밍 설정 (application-test.yml): prepare=500ms, playing=2000ms
 */
class BlockStackingIntegrationTest extends WebSocketIntegrationTestSupport {

    private static final long PREPARE_MS = 500L;
    private static final long PLAYING_MS = 2000L;

    JoinCode joinCode;
    Player host;
    TestStompSession session;

    @BeforeEach
    void setUp(
            @Autowired RoomRepository roomRepository,
            @Autowired RoomJpaRepository roomJpaRepository,
            @Autowired JoinCodeGenerator joinCodeGenerator
    ) throws Exception {
        joinCode = joinCodeGenerator.generate();
        Room room = RoomFixture.호스트_꾹이(joinCode);
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        host = room.getHost();

        room.addMiniGame(host.getName(), new BlockStackingGame());

        roomRepository.save(room);
        roomJpaRepository.save(new RoomEntity(joinCode.getValue()));

        session = createSession(joinCode, host.getName());
    }

    @Nested
    class 페이즈_전환_테스트 {

        @Test
        void 게임_페이즈가_PREPARE_PLAYING_DONE_순서로_전환된다() {
            final var stateResponses = session.subscribe(stateUrl());

            session.send(startCommandUrl(), hostStartCommand());

            final MessageResponse prepare = stateResponses.get();
            final MessageResponse playing = stateResponses.get();
            final MessageResponse done = stateResponses.get(4, TimeUnit.SECONDS);

            assertMessageContains(prepare, "\"state\":\"PREPARE\"");
            assertMessageContains(playing, PREPARE_MS, "\"state\":\"PLAYING\"");
            assertThat(playing.payload()).contains("\"endTimeEpochMs\":");
            assertMessageContains(done, PLAYING_MS, "\"state\":\"DONE\"");
            assertThat(done.payload()).doesNotContain("\"endTimeEpochMs\":");
        }

        @Test
        void PREPARE_단계에서_state_필드가_포함된_응답을_받는다() {
            final var stateResponses = session.subscribe(stateUrl());

            session.send(startCommandUrl(), hostStartCommand());

            final MessageResponse prepare = stateResponses.get();

            assertMessageContains(prepare, "\"state\":\"PREPARE\"");
            assertThat(prepare.payload()).contains("\"success\":true");
        }

        @Test
        void 타이머_만료_후_게임이_완료된다() {
            final var stateResponses = session.subscribe(stateUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // PREPARE
            stateResponses.get(); // PLAYING

            // playing(2000ms) 후 DONE 전환
            final MessageResponse done = stateResponses.get(4, TimeUnit.SECONDS);

            assertMessageContains(done, "\"state\":\"DONE\"");

            // 핵심 검증: playing 제한 시간(2000ms) 이내에 done으로 전환되지 않음
            assertThat(done.duration())
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

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // PREPARE
            stateResponses.get(); // PLAYING

            session.send(progressCommandUrl(), progressCommand(1, 100.0, 85.0, 150.0));

            final MessageResponse progressResponse = progressResponses.get();

            assertMessageContains(progressResponse, "\"name\":\"꾹이\"");
            assertMessageContains(progressResponse, "\"floor\":1");
            assertThat(progressResponse.payload()).contains("\"success\":true");
        }

        @Test
        void 여러_플레이어의_진행을_랭킹_내림차순으로_브로드캐스트한다() throws Exception {
            final TestStompSession 루키세션 = createSession(joinCode.getValue(), "루키");

            final var stateResponses = session.subscribe(stateUrl());
            final var progressResponses = session.subscribe(progressUrl());

            session.send(startCommandUrl(), hostStartCommand());

            stateResponses.get(); // PREPARE
            stateResponses.get(); // PLAYING

            // 꾹이: 2층, 루키: 1층
            session.send(progressCommandUrl(), progressCommand(1, 100.0, 85.0, 150.0));
            progressResponses.get(); // 꾹이 1층 브로드캐스트

            session.send(progressCommandUrl(), progressCommand(2, 100.0, 85.0, 135.0));
            progressResponses.get(); // 꾹이 2층 브로드캐스트

            루키세션.send(progressCommandUrl(), progressCommand(1, 100.0, 85.0, 135.0));
            final MessageResponse rankingResponse = progressResponses.get();

            // 꾹이(2층)가 루키(1층)보다 앞에 위치
            final int 꾹이위치 = rankingResponse.payload().indexOf("\"name\":\"꾹이\"");
            final int 루키위치 = rankingResponse.payload().indexOf("\"name\":\"루키\"");
            assertThat(꾹이위치).isLessThan(루키위치);
        }

        @Test
        void 유효하지_않은_overlap_이벤트는_브로드캐스트되지_않는다() {
            final var stateResponses = session.subscribe(stateUrl());
            final var progressResponses = session.subscribe(progressUrl());

            session.send(startCommandUrl(), hostStartCommand());

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

            session.send(startCommandUrl(), hostStartCommand());

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

    private String startCommandUrl() {
        return String.format("/app/room/%s/minigame/command", joinCode.getValue());
    }

    private String progressCommandUrl() {
        return String.format("/app/room/%s/block-stacking/progress", joinCode.getValue());
    }

    private String hostStartCommand() {
        return String.format("""
                {
                  "commandType": "START_MINI_GAME",
                  "commandRequest": { "hostName": "%s" }
                }
                """, host.getName().value());
    }

    private String progressCommand(
            int floor,
            double movingBlockX, double stackTopX, double stackTopWidth
    ) {
        return String.format("""
                {
                  "floor": %d,
                  "movingBlockX": %f,
                  "stackTopX": %f,
                  "stackTopWidth": %f
                }
                """, floor, movingBlockX, stackTopX, stackTopWidth);
    }
}
