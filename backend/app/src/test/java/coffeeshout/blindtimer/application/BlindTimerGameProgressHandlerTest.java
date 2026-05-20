package coffeeshout.blindtimer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blindtimer.domain.BlindTimerGameState;
import coffeeshout.blindtimer.domain.event.BlindTimerProgressEvent;
import coffeeshout.fixture.GameSessionFixture;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.ServiceTest;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BlindTimerGameProgressHandlerTest extends ServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private BlindTimerGameProgressHandler progressHandler;

    private static final String HOST_NAME = "꾹이";

    private Room room;
    private BlindTimerGame game;
    private String joinCode;

    @BeforeEach
    void setUp() {
        room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
        game = new BlindTimerGame(Duration.ofSeconds(10));
        gameSessionRepository.save(
                GameSessionFixture.게임세션_게임시작됨(
                        room.getJoinCode(), game, new PlayerName(HOST_NAME),
                        room.getPlayers().stream().map(Player::getName).toList()));
        joinCode = room.getJoinCode().getValue();

        game.startPlaying();
    }

    @Nested
    class STOP_처리 {

        @Test
        void STOP하면_진행도_이벤트가_발행된다() {
            // when
            progressHandler.handleStop(joinCode, "꾹이");

            // then
            verify(eventPublisher, atLeastOnce()).publishEvent(any(BlindTimerProgressEvent.class));
        }

        @Test
        void 이미_STOP한_플레이어가_다시_STOP하면_무시된다() {
            // given
            progressHandler.handleStop(joinCode, "꾹이");

            // when
            progressHandler.handleStop(joinCode, "꾹이");

            // then
            assertThat(game.findPlayer(new PlayerName("꾹이")).isStopped()).isTrue();
        }
    }

    @Nested
    class 전원_STOP_시_게임_종료 {

        @Test
        void 모든_플레이어가_STOP하면_게임이_종료된다() {
            // given
            for (var player : room.getPlayers()) {
                progressHandler.handleStop(joinCode, player.getName().value());
            }

            // then
            assertThat(game.getState()).isEqualTo(BlindTimerGameState.DONE);
        }
    }
}
