package coffeeshout.wormgame.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.TestStompSession.MessageCollector;
import coffeeshout.wormgame.application.WormGameService;
import coffeeshout.wormgame.domain.WormGame;
import coffeeshout.wormgame.domain.WormGameState;
import coffeeshout.wormgame.ui.request.SteerCommand;
import coffeeshout.wormgame.ui.response.WormGameStateResponse;
import coffeeshout.wormgame.ui.response.WormsStateResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    WormGameService wormGameService;

    @Autowired
    @Qualifier("wormGameScheduler")
    TaskScheduler wormGameScheduler;

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;
    TestStompSession session;
    WormGame game;

    @BeforeEach
    void setUp() throws Exception {
        joinCode = new JoinCode("W4RM");
        host = GamerFixture.호스트_꾹이();
        gamers = GamerFixture.꾹이_루키_엠제이_한스();
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        game = new WormGame();
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
        gameSessionService.startGame(joinCode, host, gamers);
        wormGameService.start(code, host.getName());

        // then — DESCRIPTION → PREPARE → PLAYING (순차 상태 토픽은 위치 기반 get 대신 목표 상태까지 훑는다)
        awaitState(stateResponses, WormGameState.DESCRIPTION);
        awaitState(stateResponses, WormGameState.PREPARE);
        awaitState(stateResponses, WormGameState.PLAYING);

        // then — 20Hz 틱 델타가 전원 머리 상태를 싣고 흐른다
        final WormsStateResponse delta = payloadAs(deltaResponses.get(2, TimeUnit.SECONDS), WormsStateResponse.class);
        assertThat(delta.tick()).isPositive();
        assertThat(delta.worms()).hasSize(gamers.size());

        // when — 조향: STOMP → Redis Stream → Consumer → Service → 도메인
        session.send(String.format("/app/room/%s/worm/steer", code), new SteerCommand(host.getName(), 1.0, 1));

        // then
        await().atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(hostTargetAngle()).isCloseTo(1.0, within(1e-9)));
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
