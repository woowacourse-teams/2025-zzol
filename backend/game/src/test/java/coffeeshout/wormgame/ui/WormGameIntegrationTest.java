package coffeeshout.wormgame.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.fixture.TestDataHelper;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.GameStartReadyEvent;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.TestStompSession.MessageCollector;
import coffeeshout.wormgame.domain.WormGame;
import coffeeshout.wormgame.domain.WormGameState;
import coffeeshout.wormgame.fixture.WormGameRulesFixture;
import coffeeshout.wormgame.ui.request.SteerCommand;
import coffeeshout.wormgame.ui.response.WormGameStateResponse;
import coffeeshout.wormgame.ui.response.WormsStateResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 해피패스 1건 — Handler → Redis Stream → Consumer → Service → Notifier → 브로드캐스트 와이어링.
 * 타이밍(application-test-game.yml): description=500ms, prepare=500ms, 틱 50ms.
 */
class WormGameIntegrationTest extends GameModuleWebSocketTest {

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    TestDataHelper testDataHelper;

    @Autowired
    @Qualifier("wormGameScheduler")
    TaskScheduler wormGameScheduler;

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;
    TestStompSession session;
    WormGame game;

    @BeforeEach
    void setUp(@Autowired JoinCodeGenerator joinCodeGenerator) throws Exception {
        joinCode = joinCodeGenerator.generate();
        host = GamerFixture.호스트_꾹이();
        gamers = 루키가_회원인_명단(joinCode);
        testDataHelper.게임_시작_준비된_방_생성(joinCode, gamers);
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        game = new WormGame(WormGameRulesFixture.defaultRules());
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(game));
        session = createSession(joinCode.getValue(), host.getName());
    }

    /** 틱 루프는 컨텍스트 수명의 스케줄러에 걸려 테스트가 끝나도 돈다 — 예약 큐를 비워 세운다(racing 선례). */
    @AfterEach
    void 틱_루프_정리() {
        game.updateState(WormGameState.DONE);
        ((ThreadPoolTaskScheduler) wormGameScheduler)
                .getScheduledThreadPoolExecutor()
                .getQueue()
                .clear();
    }

    @Test
    void 시작하면_상태_전이가_브로드캐스트되고_틱_델타가_흐르며_조향이_Stream을_거쳐_도메인에_닿는다() {
        // given
        final String code = joinCode.getValue();
        final var stateResponses = session.subscribe(String.format("/topic/room/%s/worm/state", code));
        final var deltaResponses = session.subscribe(String.format("/topic/room/%s/worm", code));

        // when — 게임 시작
        startWormGame();

        // then — DESCRIPTION → PREPARE → PLAYING (순차 상태 토픽은 위치 기반 get 대신 목표 상태까지 훑는다)
        awaitState(stateResponses, WormGameState.DESCRIPTION);
        awaitState(stateResponses, WormGameState.PREPARE);
        awaitState(stateResponses, WormGameState.PLAYING);

        // then — 20Hz 틱 델타가 전원 머리 상태를 싣고 흐른다
        final WormsStateResponse delta = payloadAs(deltaResponses.get(2, TimeUnit.SECONDS), WormsStateResponse.class);
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(delta.tick()).isPositive();
        softly.assertThat(delta.worms()).hasSize(gamers.size());
        softly.assertAll();

        // when — 조향: STOMP → Redis Stream → Consumer → Service → 도메인
        session.send(String.format("/app/room/%s/worm/steer", code), new SteerCommand(1.0, 1));

        // then
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(hostTargetAngle()).isCloseTo(1.0, within(1e-9)));
    }

    @Test
    void 라운드가_끝나면_DONE이_브로드캐스트되고_결과와_정산_아웃박스가_남는다() {
        // given — 첫 틱에 전원이 경계 밖에서 죽는 규칙으로 갈아끼워 라운드를 즉시 끝낸다.
        game = new WormGame(WormGameRulesFixture.첫_틱에_전멸하는_규칙());
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(game));
        final var stateResponses = session.subscribe(String.format("/topic/room/%s/worm/state", joinCode.getValue()));

        // when
        startWormGame();

        // then — DONE 브로드캐스트는 결과 저장보다 앞줄에 예약되므로 저장까지 따로 확인한다(#1662).
        awaitState(stateResponses, WormGameState.DONE);
        결과_저장과_정산_아웃박스를_확인한다(MiniGameType.WORM_GAME, gamers.size());
    }

    /**
     * 프로덕션 시작 경로를 그대로 탄다 — {@code :room}의 {@code MiniGameStartConsumer}가 발행하는
     * {@code GameStartReadyEvent}부터다. 서비스의 {@code start()}를 직접 부르면 미니게임 엔티티·플레이어
     * 스냅샷을 만드는 단계가 통째로 건너뛰어져, 종료 시 결과 저장 경로가 돌 수 없다(#1663).
     */
    private void startWormGame() {
        eventPublisher.publishEvent(
                new GameStartReadyEvent("evt-" + joinCode.getValue(), joinCode.getValue(), host.getName(), gamers));
    }

    private WormGameStateResponse awaitState(MessageCollector stateResponses, WormGameState target) {
        for (int i = 0; i < 8; i++) {
            final WormGameStateResponse response =
                    payloadAs(stateResponses.get(3, TimeUnit.SECONDS), WormGameStateResponse.class);
            if (response.state() == target) {
                return response;
            }
        }
        throw new AssertionError(target + " 상태를 받지 못했습니다");
    }

    private double hostTargetAngle() {
        return game.getWorms().all().stream()
                .filter(worm -> worm.getGamer().getName().equals(host.getName()))
                .findFirst()
                .orElseThrow()
                .getTargetAngle();
    }
}
