package coffeeshout.racinggame.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.fixture.GameSessionFixture;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.ServiceTest;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingGameState;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RacingGameServiceTest extends ServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private RacingGameService racingGameService;

    private static final String HOST_NAME = "꾹이";

    private Room room = RoomFixture.호스트_꾹이();
    private RacingGame racingGame = new RacingGame();

    @BeforeEach
    void setUp() {
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
    }

    @Test
    void 레이싱_게임을_시작하면_DESCRIPTION_PREPARE_PLAYING_순서로_상태가_전환된다() {
        // given
        gameSessionRepository.save(
                GameSessionFixture.게임세션_게임시작됨(
                        room.getJoinCode(), racingGame, new PlayerName(HOST_NAME),
                        room.getPlayers().stream().map(Player::getName).toList()));

        // when
        racingGameService.start(room.getJoinCode().getValue(), HOST_NAME);

        // then
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    assertThat(racingGame.getState()).isEqualTo(RacingGameState.PLAYING);
                    assertThat(racingGame.isStarted()).isTrue();
                });
    }
}
