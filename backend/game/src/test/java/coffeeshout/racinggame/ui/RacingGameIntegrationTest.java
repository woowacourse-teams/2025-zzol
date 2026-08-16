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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

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
     */
    @AfterEach
    void 자동이동_정리() {
        racingGame.stopAutoMove();
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
         IT 가속: move-interval=50ms(application-test-game.yml)로 자동이동이 돌고, 속도는 rate 기반이라
         최고속도 유지를 위해 ~50ms마다 탭한다. 결승(3000) 도달·감속·정지하면 DONE.
         고정 대기(Thread.sleep)는 컨벤션상 금지이므로 Awaitility 폴링으로 탭을 송신하고,
         충분한 탭 라운드(120회 ≈ 6s) 후 종료한다. (racingGame.state는 비휘발성이라 폴링 조건으로 쓰지 않고,
         종료 판정은 아래의 신뢰 가능한 DONE 브로드캐스트로 한다.) 프로덕션 100ms 기준 ~10s 구간이 ~6s로 단축된다.
        */
        final AtomicInteger tapRounds = new AtomicInteger();
        await().atMost(Duration.ofSeconds(8))
                .pollInterval(Duration.ofMillis(50))
                .until(() -> {
                    for (Gamer gamer : gamers) {
                        singleSession.send(tapRequestUrl, new TapCommand(gamer.getName(), 10));
                    }
                    return tapRounds.incrementAndGet() >= 120;
                });

        // then - DONE 상태 확인
        RacingGameStateResponse finishedState =
                payloadAs(stateResponses.get(5, TimeUnit.SECONDS), RacingGameStateResponse.class);
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
