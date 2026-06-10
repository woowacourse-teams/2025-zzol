package coffeeshout.racinggame.ui;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.racinggame.application.RacingGameService;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.racinggame.domain.RacingGameState;
import coffeeshout.racinggame.ui.request.TapCommand;
import coffeeshout.racinggame.ui.response.RacingGameRunnersStateResponse;
import coffeeshout.racinggame.ui.response.RacingGameStateResponse;
import coffeeshout.support.TestStompSession;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RacingGameIntegrationTest extends GameModuleWebSocketTest {

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    RacingGameService racingGameService;

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
    void 게임이_완주되면_FINISHED_상태가_전송된다() throws Exception {
        TestStompSession singleSession = createSession(joinCode.getValue(), host.getName());
        String joinCodeValue = joinCode.getValue();
        String subscribeStateUrl = String.format("/topic/room/%s/racing-game/state", joinCodeValue);
        String tapRequestUrl = String.format("/app/room/%s/racing-game/tap", joinCodeValue);

        var stateResponses = singleSession.subscribe(subscribeStateUrl);

        // 게임 시작
        startRacingGame();

        stateResponses.get(1, TimeUnit.SECONDS); // DESCRIPTION
        stateResponses.get(5, TimeUnit.SECONDS); // PREPARE (4초 후)
        stateResponses.get(3, TimeUnit.SECONDS); // PLAYING (2초 후)

        /*
         100ms마다 moveAll → 최고속도(30)로 달린다고 가정하면 결승점(3000)까지 10000ms걸림
        */
        for (int i = 0; i < 100; i++) {
            for (Gamer gamer : gamers) {
                singleSession.send(tapRequestUrl, new TapCommand(gamer.getName(), 10));
            }
            Thread.sleep(100);
        }

        // then - DONE 상태 확인 (최대 30초 대기)
        RacingGameStateResponse finishedState =
                payloadAs(stateResponses.get(30, TimeUnit.SECONDS), RacingGameStateResponse.class);
        assertThat(finishedState.state()).isEqualTo(RacingGameState.DONE);
    }

    private void startRacingGame() {
        gameSessionService.startGame(joinCode, host, gamers);
        racingGameService.start(joinCode.getValue(), host.getName());
    }
}
