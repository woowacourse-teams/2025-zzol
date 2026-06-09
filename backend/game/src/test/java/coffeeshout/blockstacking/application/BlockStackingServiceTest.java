package coffeeshout.blockstacking.application;

import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.blockstacking.domain.BlockStackingGame;
import coffeeshout.blockstacking.domain.BlockStackingGameErrorCode;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.GameModuleServiceTest;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.RoomRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class BlockStackingServiceTest extends GameModuleServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BlockStackingService service;

    @Autowired
    private GameSessionService gameSessionService;

    @MockitoSpyBean
    private BlockStackingNotifier notifier;

    private static final String HOST_NAME = "꾹이";

    private Room room;
    private BlockStackingGame game;
    private JoinCode joinCode;

    @BeforeEach
    void setUp() {
        room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
        joinCode = room.getJoinCode();

        game = new BlockStackingGame();
        final Gamer host = Gamer.guest(HOST_NAME);
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(game));
        gameSessionService.startGame(joinCode, host, room.getGamers()); // game.setUp(gamers) 호출
        game.prepare();
        game.startPlay();              // state = PLAYING
    }

    @Nested
    class 유효한_progress {

        @Test
        void 유효한_블록_안착_이벤트가_플레이어_floor를_갱신한다() {
            service.recordProgress(joinCode.getValue(), HOST_NAME,
                    1, 100.0, 85.0, 150.0);

            assertThat(floorOf(HOST_NAME)).isEqualTo(1);
        }

        @Test
        void 연속된_floor를_전송하면_순차적으로_갱신된다() {
            service.recordProgress(joinCode.getValue(), HOST_NAME,
                    1, 100.0, 85.0, 150.0);
            service.recordProgress(joinCode.getValue(), HOST_NAME,
                    2, 100.0, 85.0, 135.0);

            assertThat(floorOf(HOST_NAME)).isEqualTo(2);
        }

        @Test
        void 유효한_이벤트마다_notifier를_호출한다() {
            service.recordProgress(joinCode.getValue(), HOST_NAME,
                    1, 100.0, 85.0, 150.0);
            service.recordProgress(joinCode.getValue(), HOST_NAME,
                    2, 100.0, 85.0, 135.0);

            verify(notifier, times(2)).notifyProgressUpdated(any(), any());
        }
    }

    @Nested
    class 잘못된_progress {

        @Test
        void 비연속적_floor_이벤트는_floor를_갱신하지_않는다() {
            // floor=1 을 건너뛰고 floor=2 전송
            service.recordProgress(joinCode.getValue(), HOST_NAME,
                    2, 100.0, 85.0, 150.0);

            assertThat(floorOf(HOST_NAME)).isZero();
        }

        @Test
        void overlap이_0_이하인_이벤트는_floor를_갱신하지_않는다() {
            // movingBlockX=300 → stackTop 범위(85~235) 완전 이탈, overlap < 0
            service.recordProgress(joinCode.getValue(), HOST_NAME,
                    1, 300.0, 85.0, 150.0);

            assertThat(floorOf(HOST_NAME)).isZero();
        }

        @Test
        void 유효하지_않은_이벤트는_notifier를_호출하지_않는다() {
            service.recordProgress(joinCode.getValue(), HOST_NAME,
                    1, 300.0, 85.0, 150.0);

            verify(notifier, never()).notifyProgressUpdated(any(), any());
        }
    }

    @Nested
    class 예외_상황 {

        @Test
        void 존재하지_않는_플레이어_이름이면_PLAYER_NOT_FOUND_예외가_발생한다() {
            assertCoffeeShoutException(
                    () -> service.recordProgress(joinCode.getValue(), "없는플레이어",
                            1, 100.0, 85.0, 150.0),
                    BlockStackingGameErrorCode.PLAYER_NOT_FOUND
            );
        }

        @Test
        void PLAYING_상태가_아니면_NOT_PLAYING_STATE_예외가_발생한다() {
            game.finish(); // state = DONE

            assertCoffeeShoutException(
                    () -> service.recordProgress(joinCode.getValue(), HOST_NAME,
                            1, 100.0, 85.0, 150.0),
                    BlockStackingGameErrorCode.NOT_PLAYING_STATE
            );
        }
    }

    private int floorOf(String playerName) {
        return game.getRanking().stream()
                .filter(r -> r.name().equals(playerName))
                .findFirst()
                .orElseThrow()
                .floor();
    }
}
