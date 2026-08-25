package coffeeshout.wormgame.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

import coffeeshout.GameModuleServiceTest;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameSelectEvent;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.wormgame.domain.WormGame;
import coffeeshout.wormgame.domain.WormGameState;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WormGameServiceTest extends GameModuleServiceTest {

    private static final String HOST_NAME = "꾹이";

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private WormGameService wormGameService;

    @Autowired
    private GameSessionService gameSessionService;

    private final Room room = RoomFixture.호스트_꾹이();

    @BeforeEach
    void setUp() {
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
    }

    private WormGame startedGame() {
        // 인메모리 GameSession 저장소는 테스트 간 공유되므로 잔여 세션을 정리한 뒤 재구성한다(racing 선례).
        gameSessionService.deleteSession(room.getJoinCode());
        gameSessionService.initSession(room.getJoinCode(), Gamer.guest(HOST_NAME));
        gameSessionService.updateGames(
                new MiniGameSelectEvent(room.getJoinCode().getValue(), HOST_NAME, List.of(MiniGameType.WORM_GAME)));
        return (WormGame) gameSessionService.startGame(room.getJoinCode(), Gamer.guest(HOST_NAME), room.getGamers());
    }

    @Test
    void 시작하면_DESCRIPTION_PREPARE_PLAYING_순서로_전이되고_틱_루프가_돈다() {
        // given
        final WormGame game = startedGame();

        // when
        wormGameService.start(room.getJoinCode().getValue(), HOST_NAME);

        // then
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(game.getState()).isEqualTo(WormGameState.PLAYING);
            assertThat(game.getTickCount()).isPositive();
        });
    }

    @Test
    void 플레이_중_조향은_도메인_목표각에_반영된다() {
        // given
        final WormGame game = startedGame();
        wormGameService.start(room.getJoinCode().getValue(), HOST_NAME);
        await().atMost(Duration.ofSeconds(3)).until(game::isPlaying);

        // when
        wormGameService.steer(room.getJoinCode().getValue(), HOST_NAME, 1.0, 1);

        // then
        final double targetAngle = game.getWorms().all().stream()
                .filter(worm -> worm.getGamer().getName().equals(HOST_NAME))
                .findFirst()
                .orElseThrow()
                .getTargetAngle();
        assertThat(targetAngle).isCloseTo(1.0, within(1e-9));
    }
}
