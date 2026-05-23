package coffeeshout.speedtouch.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.support.ServiceTest;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.speedtouch.domain.SpeedTouchGame;
import coffeeshout.speedtouch.domain.SpeedTouchGameState;
import coffeeshout.speedtouch.domain.event.SpeedTouchProgressEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SpeedTouchGameProgressHandlerTest extends ServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SpeedTouchGameProgressHandler progressHandler;

    private static final String HOST_NAME = "꾹이";

    private Room room;
    private SpeedTouchGame game;
    private String joinCode;

    @BeforeEach
    void setUp() {
        room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
        game = new SpeedTouchGame();
        room.addMiniGame(new PlayerName(HOST_NAME), game);
        room.startNextGame(HOST_NAME);
        joinCode = room.getJoinCode().getValue();

        // TestTaskScheduler를 우회하여 직접 PLAYING 상태로 세팅
        game.setUp(room.getPlayers().stream().map(p -> p.toGamer()).toList());
        game.startPlaying();
    }

    @Nested
    class 터치_처리 {

        @Test
        void 올바른_번호를_터치하면_진행도_이벤트가_발행된다() {
            // when
            progressHandler.handleTouch(joinCode, "꾹이", 1);

            // then
            verify(eventPublisher, atLeastOnce()).publishEvent(any(SpeedTouchProgressEvent.class));
        }

        @Test
        void 잘못된_번호를_터치하면_진행도가_변하지_않는다() {
            // when
            progressHandler.handleTouch(joinCode, "꾹이", 10);

            // then
            final var player = game.findPlayer(new PlayerName("꾹이"));
            assertThat(player.getCurrentNumber()).isEqualTo(1);
        }
    }

    @Nested
    class 전원_완주_시_게임_종료 {

        @Test
        void 모든_플레이어가_완주하면_게임이_종료된다() {
            // given
            for (var player : room.getPlayers()) {
                final String name = player.getName().value();
                for (int i = 1; i <= 25; i++) {
                    progressHandler.handleTouch(joinCode, name, i);
                }
            }

            // then
            assertThat(game.getState()).isEqualTo(SpeedTouchGameState.DONE);
        }
    }
}
