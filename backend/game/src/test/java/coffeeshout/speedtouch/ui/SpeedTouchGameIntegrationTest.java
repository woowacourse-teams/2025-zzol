package coffeeshout.speedtouch.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.speedtouch.application.SpeedTouchGameService;
import coffeeshout.speedtouch.domain.SpeedTouchGame;
import coffeeshout.speedtouch.ui.request.TouchCommand;
import coffeeshout.speedtouch.ui.response.SpeedTouchProgressResponse;
import coffeeshout.speedtouch.ui.response.SpeedTouchStateResponse;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SpeedTouchGameIntegrationTest extends GameModuleWebSocketTest {

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    SpeedTouchGameService speedTouchGameService;

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;
    TestStompSession session;
    SpeedTouchGame game;

    @BeforeEach
    void setUp() throws Exception {
        joinCode = new JoinCode("A4BX");
        host = GamerFixture.호스트_꾹이();
        gamers = GamerFixture.꾹이_루키_엠제이_한스();
        game = new SpeedTouchGame();
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(game));
        session = createSession(joinCode.getValue(), host.getName());
    }

    @Test
    void 스피드터치_게임을_시작하면_상태_변경_메시지가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/speed-touch/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/speed-touch/progress", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var progressResponses = session.subscribe(subscribeProgressUrl);

        // when
        startSpeedTouchGame();

        // then - DESCRIPTION 상태
        MessageResponse descriptionState = stateResponses.get(2, TimeUnit.SECONDS);
        assertThat(payloadAs(descriptionState, SpeedTouchStateResponse.class).state()).isEqualTo("DESCRIPTION");

        // PREPARE 상태
        MessageResponse prepareState = stateResponses.get(6, TimeUnit.SECONDS);
        assertThat(payloadAs(prepareState, SpeedTouchStateResponse.class).state()).isEqualTo("PREPARE");

        // PLAYING 상태
        MessageResponse playingState = stateResponses.get(4, TimeUnit.SECONDS);
        assertThat(payloadAs(playingState, SpeedTouchStateResponse.class).state()).isEqualTo("PLAYING");
    }

    @Test
    void 터치하면_진행도_브로드캐스트_메시지가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/speed-touch/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/speed-touch/progress", joinCodeValue);
        final String touchUrl = String.format("/app/room/%s/speed-touch/touch", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var progressResponses = session.subscribe(subscribeProgressUrl);

        // 게임 시작
        startSpeedTouchGame();

        stateResponses.get(2, TimeUnit.SECONDS); // DESCRIPTION
        stateResponses.get(6, TimeUnit.SECONDS); // PREPARE
        progressResponses.get(6, TimeUnit.SECONDS); // PREPARE 시 초기 progress
        stateResponses.get(4, TimeUnit.SECONDS); // PLAYING

        // when - 터치
        session.send(touchUrl, new TouchCommand(host.getName(), 1));

        // then - 진행도 응답 (blocking get으로 메시지 도착까지 대기)
        MessageResponse progressUpdate = progressResponses.get(3, TimeUnit.SECONDS);
        assertThat(payloadAs(progressUpdate, SpeedTouchProgressResponse.class).players()).isNotEmpty();
    }

    @Test
    void 전원_완주하면_DONE_상태가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/speed-touch/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/speed-touch/progress", joinCodeValue);
        final String touchUrl = String.format("/app/room/%s/speed-touch/touch", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var progressResponses = session.subscribe(subscribeProgressUrl);

        // IT 가속: 터치는 strict sequential(number != currentNumber면 거부)이라 터치마다 직렬 await가 필수다.
        // 라운드트립 횟수가 곧 실행시간이므로, 전원 완주→DONE 검증(다중 플레이어 allMatch)은 유지하되
        // 플레이어를 2명으로 줄여 100회(4명×25)였던 직렬 라운드트립을 50회로 절반화한다.
        gamers = gamers.subList(0, 2);

        // 게임 시작 후 PLAYING 까지 대기
        startSpeedTouchGame();

        stateResponses.get(2, TimeUnit.SECONDS); // DESCRIPTION
        stateResponses.get(6, TimeUnit.SECONDS); // PREPARE
        progressResponses.get(6, TimeUnit.SECONDS); // initial progress
        stateResponses.get(4, TimeUnit.SECONDS); // PLAYING

        // when - 모든 플레이어 1~25 터치
        // 각 플레이어별로 1~25를 순차 전송하되, 이전 터치 처리 완료를 Awaitility로 확인
        for (Gamer gamer : gamers) {
            final String playerName = gamer.getName();
            for (int i = 1; i <= 25; i++) {
                final int expectedNext = i + 1;
                session.send(touchUrl, new TouchCommand(playerName, i));

                // 게임 도메인 객체의 currentNumber가 갱신될 때까지 대기
                await().atMost(Duration.ofSeconds(5))
                        .pollInterval(Duration.ofMillis(50))
                        .untilAsserted(() ->
                                assertThat(game.findPlayer(playerName).getCurrentNumber())
                                        .isGreaterThanOrEqualTo(expectedNext)
                        );
            }
        }

        // then - DONE 상태 확인
        MessageResponse doneState = stateResponses.get(10, TimeUnit.SECONDS);
        assertThat(payloadAs(doneState, SpeedTouchStateResponse.class).state()).isEqualTo("DONE");
    }

    private void startSpeedTouchGame() {
        gameSessionService.startGame(joinCode, host, gamers);
        speedTouchGameService.start(joinCode.getValue(), host.getName());
    }
}
