package coffeeshout.racinggame.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.event.GameStartReadyEvent;
import coffeeshout.racinggame.application.RacingGameService;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingGameState;
import coffeeshout.racinggame.ui.request.TapCommand;
import coffeeshout.racinggame.ui.response.RacingGameRunnersStateResponse;
import coffeeshout.racinggame.ui.response.RacingGameStateResponse;
import coffeeshout.room.application.port.RoomEntityRepository;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.support.TestStompSession;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class RacingGameIntegrationTest extends GameModuleWebSocketTest {

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    RacingGameService racingGameService;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    RoomEntityRepository roomEntityRepository;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    @Qualifier("racingGameScheduler")
    TaskScheduler racingGameScheduler;

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;
    TestStompSession session;
    RacingGame racingGame;

    @BeforeEach
    void setUp() throws Exception {
        joinCode = new JoinCode("A4BX");
        host = GamerFixture.호스트_꾹이();
        gamers = GamerFixture.꾹이_루키_엠제이_한스();
        racingGame = new RacingGame();
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(racingGame));

        session = createSession(joinCode.getValue(), host.getName());
    }

    /**
     * 자동 이동은 컨텍스트 수명의 스케줄러 빈에 걸리므로 테스트 메서드가 끝나도 계속 돈다. 완주까지 가지 않는
     * 테스트가 남긴 레이스는 무탭 최소 속도(3)로 약 50초 뒤 혼자 완주해, 그때 진행 중인 다른 테스트의 방·
     * 미니게임 엔티티에 결과를 덧쓴다(joinCode·플레이어 픽스처가 같고 eventId는 매번 UUID라 @RedisLock
     * 중복 마커도 막지 못한다). 저장 건수를 세는 검증이 그 유령 완주에 오염되지 않도록 여기서 세워둔다.
     * <p>
     * stopAutoMove()만으로는 부족하다 — autoMoveFuture는 시작 +1s(description 500ms + prepare 500ms)에
     * startAutoMove에서야 대입되고, 그 전에 테스트가 실패하면 null이라 즉시 리턴한다. 그 앞의
     * description·prepare 예약 태스크는 어디에도 보관되지 않아 취소할 대상 자체가 없고, teardown 뒤에
     * 레이스를 새로 시작한다. 그래서 취소(재예약 차단) 후 스케줄러 예약 큐를 비운다.
     */
    @AfterEach
    void 자동이동_정리() {
        racingGame.stopAutoMove();
        ((ThreadPoolTaskScheduler) racingGameScheduler)
                .getScheduledThreadPoolExecutor()
                .getQueue()
                .clear();
    }

    @Test
    void 레이싱_게임을_시작한다() {
        // given
        String joinCodeValue = joinCode.getValue();
        String subscribeStateUrl = String.format("/topic/room/%s/racing-game/state", joinCodeValue);
        String subscribePositionUrl = String.format("/topic/room/%s/racing-game", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var positionResponses = session.subscribe(subscribePositionUrl);

        // when - 게임 시작
        startRacingGame();

        // then - 첫 번째 응답: DESCRIPTION 상태 (4초 후)
        RacingGameStateResponse descriptionState =
                payloadAs(stateResponses.get(1, TimeUnit.SECONDS), RacingGameStateResponse.class);
        assertThat(descriptionState.state()).isEqualTo(RacingGameState.DESCRIPTION);

        // 두 번째 응답: PREPARE 상태 (추가 2초 후)
        RacingGameStateResponse prepareState =
                payloadAs(stateResponses.get(5, TimeUnit.SECONDS), RacingGameStateResponse.class);
        assertThat(prepareState.state()).isEqualTo(RacingGameState.PREPARE);

        // 세 번째 응답: PLAYING 상태 (바로 이어서)
        RacingGameStateResponse playingState =
                payloadAs(stateResponses.get(3, TimeUnit.SECONDS), RacingGameStateResponse.class);
        assertThat(playingState.state()).isEqualTo(RacingGameState.PLAYING);

        // 자동 이동으로 위치 업데이트 메시지가 계속 발행됨
        RacingGameRunnersStateResponse positionUpdate1 =
                payloadAs(positionResponses.get(1, TimeUnit.SECONDS), RacingGameRunnersStateResponse.class);
        assertThat(positionUpdate1.distance()).isNotNull();
        assertThat(positionUpdate1.players()).isNotEmpty();
    }

    @Test
    void 게임이_완주되면_DONE_상태가_전송되고_결과가_저장된다() throws Exception {
        // given - 결과 저장 리스너가 요구하는 방·플레이어 기반. 도메인 Room은 인메모리, RoomEntity는 DB라 둘 다 채워야
        // room_session·player id가 해석된다(RoomSnapshotQueryAdapter).
        final Room room = RoomFixture.호스트_꾹이(joinCode);
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        roomRepository.save(room);
        roomEntityRepository.save(new RoomEntity(joinCode.getValue()));

        TestStompSession singleSession = createSession(joinCode.getValue(), host.getName());
        String joinCodeValue = joinCode.getValue();
        String subscribeStateUrl = String.format("/topic/room/%s/racing-game/state", joinCodeValue);
        String tapRequestUrl = String.format("/app/room/%s/racing-game/tap", joinCodeValue);

        var stateResponses = singleSession.subscribe(subscribeStateUrl);

        // 게임 시작 - :game 모듈 경계(:room의 MiniGameStartConsumer가 발행하는 GameStartReadyEvent)부터 태운다.
        // 그래야 MiniGameEntity·플레이어 스냅샷이 만들어져 종료 시 결과가 저장될 수 있다. startRacingGame()은
        // 서비스를 직접 불러 이 앞단을 건너뛴다. 그 앞의 STOMP→Redis Stream 구간은 이 테스트 범위 밖이다.
        eventPublisher.publishEvent(
                new GameStartReadyEvent("evt-" + joinCodeValue, joinCodeValue, host.getName(), gamers));

        stateResponses.get(1, TimeUnit.SECONDS); // DESCRIPTION
        stateResponses.get(5, TimeUnit.SECONDS); // PREPARE (4초 후)
        stateResponses.get(3, TimeUnit.SECONDS); // PLAYING (2초 후)

        /*
         탭은 주기적으로 보내야 한다. 주행 중에는 매 틱 속도가 감쇠하므로(RacingGame.SPEED_DECAY_RATE)
         한 발만 쏘면 속도가 MIN_SPEED까지 떨어져 아무도 결승(3000)에 닿지 못한다 — 실제 플레이와 같이
         계속 눌러야 한다. 프론트도 200ms마다 누적 탭을 보낸다(RacingGameOverlay). 다만 프론트는
         안 눌렀을 때 tapCount 0을 보내고 그건 MIN_SPEED로 환산되므로, 여기서 재현하는 것은
         '탭 메시지가 계속 도착하는' 정상 경로다.

         주기를 400ms로 두는 이유는 WS Rate Limiter다. 세션당 초당 20건을 넘기면 초과분이 조용히
         드롭되는데(#1664 CI 실패) 이 테스트는 한 세션으로 4명분을 보낸다. 400ms 주기면 초당 10건이라
         상한의 절반이다. 200ms로 줄이면 정확히 상한에 걸려 드롭 여부가 부하에 좌우된다.

         tapCount 40은 경과 400ms 기준 초당 100탭이라 MAX_SPEED(60)로 clamp된다.
        */
        final ScheduledExecutorService tapper = Executors.newSingleThreadScheduledExecutor();
        tapper.scheduleAtFixedRate(
                () -> gamers.forEach(gamer -> singleSession.send(tapRequestUrl, new TapCommand(gamer.getName(), 40))),
                0,
                400,
                TimeUnit.MILLISECONDS);

        // then - DONE 상태 확인. 주행 약 9주기(3.6초) + 완주 후 감속 + race-finished-delay 만큼 걸린다.
        final RacingGameStateResponse finishedState;
        try {
            finishedState = payloadAs(stateResponses.get(10, TimeUnit.SECONDS), RacingGameStateResponse.class);
        } finally {
            // DONE 이후의 탭은 PLAYING이 아니라 BusinessException이 된다 — 받자마자 멈춘다.
            tapper.shutdownNow();
        }
        assertThat(finishedState.state()).isEqualTo(RacingGameState.DONE);

        // then - 종료 결과가 실제로 저장된다. DONE 브로드캐스트는 결과 저장보다 앞서 예약되므로(#1662)
        // 종료 신호만 검증하면 저장이 통째로 실패해도 이 테스트는 초록으로 남는다.
        await().atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> assertThat(저장된_레이싱_결과_수()).isEqualTo(gamers.size()));
    }

    private Integer 저장된_레이싱_결과_수() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM mini_game_result WHERE mini_game_type = 'RACING_GAME'", Integer.class);
    }

    private void startRacingGame() {
        gameSessionService.startGame(joinCode, host, gamers);
        racingGameService.start(joinCode.getValue(), host.getName());
    }
}
