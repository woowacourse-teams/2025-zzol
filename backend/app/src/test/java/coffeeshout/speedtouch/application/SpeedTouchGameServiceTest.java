package coffeeshout.speedtouch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.speedtouch.domain.SpeedTouchGame;
import coffeeshout.speedtouch.domain.SpeedTouchGameState;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SpeedTouchGameServiceTest extends ServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SpeedTouchGameService speedTouchGameService;

    private static final String HOST_NAME = "꾹이";

    private Room room;
    private SpeedTouchGame game;

    @BeforeEach
    void setUp() {
        room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
        game = new SpeedTouchGame();
    }

    @Test
    void 스피드터치_게임을_시작하면_타임아웃으로_DONE까지_전환된다() {
        // given
        room.addMiniGame(new PlayerName(HOST_NAME), game);
        room.startNextGame(HOST_NAME);

        // when - TestTaskScheduler는 모든 schedule을 즉시 실행하므로
        // DESCRIPTION → PREPARE → PLAYING → timeout → DONE까지 한번에 진행됨
        speedTouchGameService.start(room.getJoinCode().getValue(), HOST_NAME);

        // then - 비동기 스케줄러가 상태 전이를 완료할 때까지 대기
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(game.getState()).isEqualTo(SpeedTouchGameState.DONE);
                    assertThat(game.getStartTime()).isNotNull();
                });
    }

    @Test
    void getMiniGameType은_SPEED_TOUCH를_반환한다() {
        // when & then
        assertThat(speedTouchGameService.getMiniGameType()).isEqualTo(MiniGameType.SPEED_TOUCH);
    }
}
