package coffeeshout.racinggame.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import coffeeshout.GameModuleServiceTest;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameSelectEvent;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingGameState;
import coffeeshout.racinggame.domain.event.RaceStateChangedEvent;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.RoomRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class RacingGameServiceTest extends GameModuleServiceTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RacingGameService racingGameService;

    @Autowired
    private GameSessionService gameSessionService;

    private static final String HOST_NAME = "꾹이";

    private Room room = RoomFixture.호스트_꾹이();

    @Autowired
    @Qualifier("racingGameScheduler")
    private TaskScheduler racingGameScheduler;

    @BeforeEach
    void setUp() {
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
    }

    /**
     * start() 가 건 100ms 자동 이동은 컨텍스트 수명 스케줄러에 남아 테스트가 끝나도 돈다.
     * 탭이 없으면 약 50초 뒤 혼자 완주해 그때 돌고 있는 다른 테스트의 세션에 결과를 덧쓴다.
     * 통합 테스트의 자동이동_정리 와 같은 이유로 예약 큐를 비운다.
     */
    @AfterEach
    void 자동이동_정리() {
        ((ThreadPoolTaskScheduler) racingGameScheduler)
                .getScheduledThreadPoolExecutor()
                .getQueue()
                .clear();
    }

    @Test
    void 레이싱_게임을_시작하면_DESCRIPTION_PREPARE_PLAYING_순서로_상태가_전환된다() {
        // given
        // 인메모리 GameSession 저장소는 테스트 간 공유되므로 이전 테스트의 잔여 세션을 정리한 뒤 재구성한다.
        // 세션은 방 생성 시 권위 있는 호스트로 사전 생성되므로(지연 생성 제거 — Option B), initSession 후 updateGames 한다.
        gameSessionService.deleteSession(room.getJoinCode());
        gameSessionService.initSession(room.getJoinCode(), Gamer.guest(HOST_NAME));
        gameSessionService.updateGames(
                new MiniGameSelectEvent(room.getJoinCode().getValue(), HOST_NAME, List.of(MiniGameType.RACING_GAME)));
        RacingGame racingGame =
                (RacingGame) gameSessionService.startGame(room.getJoinCode(), Gamer.guest(HOST_NAME), room.getGamers());

        // when
        racingGameService.start(room.getJoinCode().getValue(), HOST_NAME);

        // then
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            assertThat(racingGame.getState()).isEqualTo(RacingGameState.PLAYING);
            assertThat(racingGame.isStarted()).isTrue();
        });
    }

    @Test
    void PLAYING_발행_직후_들어온_첫_탭이_속도에_반영된다() {
        // given
        gameSessionService.deleteSession(room.getJoinCode());
        gameSessionService.initSession(room.getJoinCode(), Gamer.guest(HOST_NAME));
        gameSessionService.updateGames(
                new MiniGameSelectEvent(room.getJoinCode().getValue(), HOST_NAME, List.of(MiniGameType.RACING_GAME)));
        final RacingGame racingGame =
                (RacingGame) gameSessionService.startGame(room.getJoinCode(), Gamer.guest(HOST_NAME), room.getGamers());
        final String joinCode = room.getJoinCode().getValue();

        // 프론트는 PLAYING 을 받는 즉시 탭을 보낸다. 발행되는 그 자리에서 탭을 쳐 가장 이른 탭을 흉내 낸다.
        doAnswer(invocation -> {
                    if (invocation.getArgument(0) instanceof RaceStateChangedEvent event
                            && event.state() == RacingGameState.PLAYING) {
                        racingGameService.tap(joinCode, HOST_NAME, 5);
                    }
                    return null;
                })
                .when(eventPublisher)
                .publishEvent(any(Object.class));

        // when
        racingGameService.start(joinCode, HOST_NAME);

        // then
        // 발행 뒤에 속도를 초기화하면 이 탭이 지워져 최저 속도로 남는다.
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertSoftly(softly -> {
                    softly.assertThat(racingGame.getState()).isEqualTo(RacingGameState.PLAYING);
                    // 방 픽스처는 호스트 한 명이라 첫 러너가 곧 탭을 친 사람이다.
                    softly.assertThat(racingGame
                                    .getRunners()
                                    .getRunners()
                                    .getFirst()
                                    .getSpeed())
                            .isGreaterThan(RacingGame.MIN_SPEED);
                }));
    }
}
