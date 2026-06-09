package coffeeshout.blindtimer.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blindtimer.domain.BlindTimerGameState;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.GameModuleServiceTest;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.RoomRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BlindTimerGameServiceTest extends GameModuleServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BlindTimerGameService blindTimerGameService;

    @Autowired
    private GameSessionService gameSessionService;

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
        startGameInSession();

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

    /**
     * 테스트가 구성한 {@code game} 인스턴스를 GameSession 대기열에 넣고 시작시킨다(ADR-0023 Step 4).
     * {@code startNextGame}이 동일 인스턴스를 완료 목록에 저장하므로 서비스의 {@code findCompletedGame}이
     * 같은 객체를 반환하고, 세션을 PLAYING으로 전이시켜 종료 시 {@code finishGame}이 동작한다.
     */
    private void startGameInSession() {
        final Gamer host = Gamer.guest(HOST_NAME);
        gameSessionService.deleteSession(room.getJoinCode());
        gameSessionService.initSession(room.getJoinCode(), host);
        gameSessionService.getSession(room.getJoinCode()).replaceGames(host, List.of(game));
        gameSessionService.startGame(room.getJoinCode(), host, room.getGamers());
    }
}
