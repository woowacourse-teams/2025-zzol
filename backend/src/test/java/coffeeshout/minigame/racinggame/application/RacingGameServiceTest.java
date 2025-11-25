package coffeeshout.minigame.racinggame.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.global.ServiceTest;
import coffeeshout.racinggame.application.RacingGameService;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingGameState;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RacingGameServiceTest extends ServiceTest {

    @Autowired
    private RoomRepository roomRepository;

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
    void 레이싱_게임을_시작하면_DESCRIPTION_PREPARE_PLAYING_순서로_상태가_전환된다() throws InterruptedException {
        // given
        room.addMiniGame(new PlayerName(HOST_NAME), racingGame);
        room.startNextGame(HOST_NAME);

        // when
        racingGameService.start(room.getJoinCode().getValue(), HOST_NAME);
        Thread.sleep(100);

        // then
        assertThat(racingGame.getState()).isEqualTo(RacingGameState.PLAYING);
        assertThat(racingGame.isStarted()).isTrue();
    }
}
