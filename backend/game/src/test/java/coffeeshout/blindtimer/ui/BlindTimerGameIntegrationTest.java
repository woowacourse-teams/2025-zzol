package coffeeshout.blindtimer.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.blindtimer.ui.request.StopCommand;
import coffeeshout.blindtimer.ui.response.BlindTimerProgressResponse;
import coffeeshout.blindtimer.ui.response.BlindTimerStateResponse;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.fixture.TestDataHelper;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.event.GameStartReadyEvent;
import coffeeshout.support.TestStompSession;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

class BlindTimerGameIntegrationTest extends GameModuleWebSocketTest {

    private static final String JOIN_CODE_CHARSET = "ABCDFGHJKLMNPQRSTUVWXYZ346789";
    private static final AtomicInteger JOIN_CODE_SEQUENCE = new AtomicInteger();

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    ApplicationEventPublisher eventPublisher;

    @Autowired
    TestDataHelper testDataHelper;

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;
    TestStompSession session;
    BlindTimerGame game;

    @BeforeEach
    void setUp() throws Exception {
        joinCode = uniqueJoinCode();
        host = GamerFixture.호스트_꾹이();
        gamers = GamerFixture.꾹이_루키_엠제이_한스();
        game = new BlindTimerGame(Duration.ofSeconds(10));
        testDataHelper.게임_시작_준비된_방_생성(joinCode);
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(game));
        session = createSession(joinCode.getValue(), host.getName());
    }

    /**
     * 시작→상태 전환→STOP 진행도→전원 STOP DONE은 모두 하나의 게임 플로우 위에서 순서대로 일어난다.
     * 페이즈마다 게임을 재시작하면 description/prepare/playing 대기가 중복되므로,
     * 단일 플로우에서 상태 전환·진행도 브로드캐스트·DONE 전환을 함께 검증한다.
     */
    @Test
    void 게임_시작부터_전원_STOP까지_상태와_진행도가_순서대로_브로드캐스트된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/blind-timer/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/blind-timer/progress", joinCodeValue);
        final String stopUrl = String.format("/app/room/%s/blind-timer/stop", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var progressResponses = session.subscribe(subscribeProgressUrl);

        // when - 게임 시작
        startBlindTimerGame();

        // 상태 전환: DESCRIPTION(targetTimeMillis) → PREPARE → PLAYING
        BlindTimerStateResponse descriptionState =
                payloadAs(stateResponses.get(2, TimeUnit.SECONDS), BlindTimerStateResponse.class);
        BlindTimerStateResponse prepareState =
                payloadAs(stateResponses.get(6, TimeUnit.SECONDS), BlindTimerStateResponse.class);
        progressResponses.get(6, TimeUnit.SECONDS); // PREPARE 시 초기 progress
        BlindTimerStateResponse playingState =
                payloadAs(stateResponses.get(10, TimeUnit.SECONDS), BlindTimerStateResponse.class);

        // host STOP → 진행도 브로드캐스트
        session.send(stopUrl, new StopCommand(host.getName()));
        BlindTimerProgressResponse progressUpdate =
                payloadAs(progressResponses.get(3, TimeUnit.SECONDS), BlindTimerProgressResponse.class);

        // 전원 STOP (host 포함, 재STOP은 멱등) → DONE
        for (Gamer gamer : gamers) {
            final String playerName = gamer.getName();
            session.send(stopUrl, new StopCommand(playerName));

            await().atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(50))
                    .untilAsserted(() ->
                            assertThat(game.findPlayer(playerName).isStopped()).isTrue());
        }
        BlindTimerStateResponse doneState =
                payloadAs(stateResponses.get(10, TimeUnit.SECONDS), BlindTimerStateResponse.class);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(descriptionState.state()).isEqualTo("DESCRIPTION");
            softly.assertThat(descriptionState.targetTimeMillis()).isEqualTo(10000);
            softly.assertThat(prepareState.state()).isEqualTo("PREPARE");
            softly.assertThat(playingState.state()).isEqualTo("PLAYING");
            softly.assertThat(progressUpdate.players()).isNotEmpty();
            softly.assertThat(doneState.state()).isEqualTo("DONE");
        });
    }

    /**
     * 프로덕션 시작 경로를 그대로 탄다 — {@code :room}의 {@code MiniGameStartConsumer}가 발행하는
     * {@code GameStartReadyEvent}부터다. 서비스의 {@code start()}를 직접 부르면 미니게임 엔티티·플레이어
     * 스냅샷을 만드는 단계가 통째로 건너뛰어져, 종료 시 결과 저장 경로가 돌 수 없다(#1663).
     */
    private void startBlindTimerGame() {
        eventPublisher.publishEvent(
                new GameStartReadyEvent("evt-" + joinCode.getValue(), joinCode.getValue(), host.getName(), gamers));
    }

    /**
     * 테스트마다 고유한 joinCode를 발급한다.
     *
     * <p>여러 테스트가 같은 joinCode를 쓰면 동일한 {@code /topic/room/{code}/blind-timer/state} 토픽을 공유한다.
     * 한 테스트가 시작한 게임의 스케줄된 트레일링 브로드캐스트(타임아웃→DONE 등)는 {@code deleteSession}이
     * 스케줄러를 정리하지 않으므로 다음 테스트의 같은 토픽으로 새어들어, "첫 state 메시지가 DESCRIPTION이 아님"
     * 같은 부하 의존 flaky 실패를 만든다. 테스트별 고유 코드로 토픽을 분리해 누수를 차단한다.</p>
     */
    private static JoinCode uniqueJoinCode() {
        final int sequence = JOIN_CODE_SEQUENCE.getAndIncrement();
        final int radix = JOIN_CODE_CHARSET.length();
        final char first = JOIN_CODE_CHARSET.charAt((sequence / (radix * radix)) % radix);
        final char second = JOIN_CODE_CHARSET.charAt((sequence / radix) % radix);
        final char third = JOIN_CODE_CHARSET.charAt(sequence % radix);
        return new JoinCode("" + first + second + third + 'B');
    }
}
