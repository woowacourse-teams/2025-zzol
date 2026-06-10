package coffeeshout.blindtimer.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.blindtimer.application.BlindTimerGameService;
import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blindtimer.ui.request.StopCommand;
import coffeeshout.blindtimer.ui.response.BlindTimerProgressResponse;
import coffeeshout.blindtimer.ui.response.BlindTimerStateResponse;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BlindTimerGameIntegrationTest extends GameModuleWebSocketTest {

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    BlindTimerGameService blindTimerGameService;

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;
    TestStompSession session;
    BlindTimerGame game;

    @BeforeEach
    void setUp() throws Exception {
        joinCode = new JoinCode("A4BX");
        host = GamerFixture.호스트_꾹이();
        gamers = GamerFixture.꾹이_루키_엠제이_한스();
        game = new BlindTimerGame(Duration.ofSeconds(10));
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(game));
        session = createSession(joinCode.getValue(), host.getName());
    }

    @Test
    void 블라인드타이머_게임을_시작하면_상태_변경_메시지가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/blind-timer/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/blind-timer/progress", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var progressResponses = session.subscribe(subscribeProgressUrl);

        // when
        startBlindTimerGame();

        // then - DESCRIPTION 상태 (targetTimeMillis 포함)
        BlindTimerStateResponse descriptionState = payloadAs(stateResponses.get(2, TimeUnit.SECONDS), BlindTimerStateResponse.class);
        assertThat(descriptionState.state()).isEqualTo("DESCRIPTION");
        assertThat(descriptionState.targetTimeMillis()).isEqualTo(10000);

        // PREPARE 상태
        BlindTimerStateResponse prepareState = payloadAs(stateResponses.get(6, TimeUnit.SECONDS), BlindTimerStateResponse.class);
        assertThat(prepareState.state()).isEqualTo("PREPARE");

        // PLAYING 상태
        BlindTimerStateResponse playingState = payloadAs(stateResponses.get(4, TimeUnit.SECONDS), BlindTimerStateResponse.class);
        assertThat(playingState.state()).isEqualTo("PLAYING");
    }

    @Test
    void STOP하면_진행도_브로드캐스트_메시지가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/blind-timer/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/blind-timer/progress", joinCodeValue);
        final String stopUrl = String.format("/app/room/%s/blind-timer/stop", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var progressResponses = session.subscribe(subscribeProgressUrl);

        // 게임 시작
        startBlindTimerGame();

        stateResponses.get(2, TimeUnit.SECONDS); // DESCRIPTION
        stateResponses.get(6, TimeUnit.SECONDS); // PREPARE
        progressResponses.get(6, TimeUnit.SECONDS); // PREPARE 시 초기 progress
        stateResponses.get(4, TimeUnit.SECONDS); // PLAYING

        // when - STOP
        session.send(stopUrl, new StopCommand(host.getName()));

        // then - 진행도 응답
        BlindTimerProgressResponse progressUpdate = payloadAs(progressResponses.get(3, TimeUnit.SECONDS), BlindTimerProgressResponse.class);
        assertThat(progressUpdate.players()).isNotEmpty();
    }

    @Test
    void 전원_STOP하면_DONE_상태가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/blind-timer/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/blind-timer/progress", joinCodeValue);
        final String stopUrl = String.format("/app/room/%s/blind-timer/stop", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var progressResponses = session.subscribe(subscribeProgressUrl);

        // 게임 시작 후 PLAYING 까지 대기
        startBlindTimerGame();

        stateResponses.get(2, TimeUnit.SECONDS); // DESCRIPTION
        stateResponses.get(6, TimeUnit.SECONDS); // PREPARE
        progressResponses.get(6, TimeUnit.SECONDS); // initial progress
        stateResponses.get(4, TimeUnit.SECONDS); // PLAYING

        // when - 모든 플레이어 STOP
        for (Gamer gamer : gamers) {
            final String playerName = gamer.getName();
            session.send(stopUrl, new StopCommand(playerName));

            await().atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(50))
                    .untilAsserted(() ->
                            assertThat(game.findPlayer(playerName).isStopped()).isTrue()
                    );
        }

        // then - DONE 상태 확인
        BlindTimerStateResponse doneState = payloadAs(stateResponses.get(10, TimeUnit.SECONDS), BlindTimerStateResponse.class);
        assertThat(doneState.state()).isEqualTo("DONE");
    }

    /**
     * WS START 커맨드(Room 검증·영속 경유) 대신 :game 서비스를 직접 호출해 게임을 시작한다.
     * {@code startGame}으로 READY→PLAYING 전이 후 {@code start}로 플로우를 스케줄한다(프로덕션 onGameStartReady와 동일 순서).
     */
    private void startBlindTimerGame() {
        gameSessionService.startGame(joinCode, host, gamers);
        blindTimerGameService.start(joinCode.getValue(), host.getName());
    }
}
