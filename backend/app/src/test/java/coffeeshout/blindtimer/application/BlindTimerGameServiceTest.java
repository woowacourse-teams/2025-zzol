package coffeeshout.blindtimer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blindtimer.domain.BlindTimerGameState;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.support.app.ServiceTest;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BlindTimerGameServiceTest extends ServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BlindTimerGameService blindTimerGameService;

    private static final String HOST_NAME = "꾹이";

    private Room room;
    private BlindTimerGame game;

    @BeforeEach
    void setUp() {
        room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
        game = new BlindTimerGame(Duration.ofSeconds(10));
    }

    @Test
    void 블라인드타이머_게임을_시작하면_타임아웃으로_DONE까지_전환된다() {
        // given
        room.addMiniGame(new PlayerName(HOST_NAME), game);
        room.startNextGame(HOST_NAME);

        // when
        blindTimerGameService.start(room.getJoinCode().getValue(), HOST_NAME);

        // then
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(game.getState()).isEqualTo(BlindTimerGameState.DONE);
                    assertThat(game.getStartTime()).isNotNull();
                });
    }

    @Test
    void getMiniGameType은_BLIND_TIMER를_반환한다() {
        // when & then
        assertThat(blindTimerGameService.getMiniGameType()).isEqualTo(MiniGameType.BLIND_TIMER);
    }
}
