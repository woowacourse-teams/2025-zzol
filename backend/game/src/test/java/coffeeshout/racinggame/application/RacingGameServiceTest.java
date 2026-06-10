package coffeeshout.racinggame.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.GameModuleServiceTest;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingGameState;
import coffeeshout.room.domain.Room;
import coffeeshout.minigame.event.dto.MiniGameSelectEvent;
import coffeeshout.room.domain.repository.RoomRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RacingGameServiceTest extends GameModuleServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RacingGameService racingGameService;

    @Autowired
    private GameSessionService gameSessionService;

    private static final String HOST_NAME = "꾹이";

    private Room room = RoomFixture.호스트_꾹이();

    @BeforeEach
    void setUp() {
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
    }

    @Test
    void 레이싱_게임을_시작하면_DESCRIPTION_PREPARE_PLAYING_순서로_상태가_전환된다() {
        // given
        // 인메모리 GameSession 저장소는 테스트 간 공유되므로 이전 테스트의 잔여 세션을 정리한 뒤 재구성한다.
        // 세션은 방 생성 시 권위 있는 호스트로 사전 생성되므로(지연 생성 제거 — Option B), initSession 후 updateGames 한다.
        gameSessionService.deleteSession(room.getJoinCode());
        gameSessionService.initSession(room.getJoinCode(), Gamer.guest(HOST_NAME));
        gameSessionService.updateGames(new MiniGameSelectEvent(
                room.getJoinCode().getValue(), HOST_NAME, List.of(MiniGameType.RACING_GAME)));
        RacingGame racingGame = (RacingGame) gameSessionService.startGame(
                room.getJoinCode(), Gamer.guest(HOST_NAME), room.getGamers());

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
