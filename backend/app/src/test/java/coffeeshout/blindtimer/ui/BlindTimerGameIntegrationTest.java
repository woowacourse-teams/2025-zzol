package coffeeshout.blindtimer.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.blindtimer.domain.BlindTimerGame;
import coffeeshout.fixture.GameSessionFixture;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.fixture.TestStompSession;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import coffeeshout.MessageResponse;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class BlindTimerGameIntegrationTest extends WebSocketIntegrationTestSupport {

    JoinCode joinCode;
    Player host;
    TestStompSession session;
    Room room;
    BlindTimerGame game;

    @BeforeEach
    void setUp(@Autowired RoomRepository roomRepository,
               @Autowired GameSessionRepository gameSessionRepository) throws Exception {
        joinCode = new JoinCode("A4BX");
        room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        host = room.getHost();
        game = new BlindTimerGame(Duration.ofSeconds(10));
        gameSessionRepository.save(GameSessionFixture.게임세션_게임대기(joinCode, game, host.getName()));
        roomRepository.save(room);
        session = createSession(joinCode, host.getName());
    }

    @Test
    void 블라인드타이머_게임을_시작하면_상태_변경_메시지가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/blind-timer/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/blind-timer/progress", joinCodeValue);
        final String requestUrl = String.format("/app/room/%s/minigame/command", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var progressResponses = session.subscribe(subscribeProgressUrl);

        // when
        session.send(requestUrl, String.format("""
                {
                  "commandRequest": {
                    "hostName": "%s"
                  },
                  "commandType": "START_MINI_GAME"
                }
                """, host.getName().value()));

        // then - DESCRIPTION 상태 (targetTimeMillis 포함)
        MessageResponse descriptionState = stateResponses.get(2, TimeUnit.SECONDS);
        assertMessageContains(descriptionState, "\"state\":\"DESCRIPTION\"");
        assertMessageContains(descriptionState, "\"success\":true");
        assertMessageContains(descriptionState, "\"targetTimeMillis\":10000");

        // PREPARE 상태
        MessageResponse prepareState = stateResponses.get(6, TimeUnit.SECONDS);
        assertMessageContains(prepareState, "\"state\":\"PREPARE\"");

        // PLAYING 상태
        MessageResponse playingState = stateResponses.get(4, TimeUnit.SECONDS);
        assertMessageContains(playingState, "\"state\":\"PLAYING\"");
    }

    @Test
    void STOP하면_진행도_브로드캐스트_메시지가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/blind-timer/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/blind-timer/progress", joinCodeValue);
        final String startUrl = String.format("/app/room/%s/minigame/command", joinCodeValue);
        final String stopUrl = String.format("/app/room/%s/blind-timer/stop", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var progressResponses = session.subscribe(subscribeProgressUrl);

        // 게임 시작
        session.send(startUrl, String.format("""
                {
                  "commandRequest": {
                    "hostName": "%s"
                  },
                  "commandType": "START_MINI_GAME"
                }
                """, host.getName().value()));

        stateResponses.get(2, TimeUnit.SECONDS); // DESCRIPTION
        stateResponses.get(6, TimeUnit.SECONDS); // PREPARE
        progressResponses.get(6, TimeUnit.SECONDS); // PREPARE 시 초기 progress
        stateResponses.get(4, TimeUnit.SECONDS); // PLAYING

        // when - STOP
        session.send(stopUrl, String.format("""
                {
                  "playerName": "%s"
                }
                """, host.getName().value()));

        // then - 진행도 응답
        MessageResponse progressUpdate = progressResponses.get(3, TimeUnit.SECONDS);
        assertMessageContains(progressUpdate, "\"success\":true");
        assertMessageContains(progressUpdate, "\"players\"");
    }

    @Test
    void 전원_STOP하면_DONE_상태가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/blind-timer/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/blind-timer/progress", joinCodeValue);
        final String startUrl = String.format("/app/room/%s/minigame/command", joinCodeValue);
        final String stopUrl = String.format("/app/room/%s/blind-timer/stop", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var progressResponses = session.subscribe(subscribeProgressUrl);

        // 게임 시작 후 PLAYING 까지 대기
        session.send(startUrl, String.format("""
                {
                  "commandRequest": {
                    "hostName": "%s"
                  },
                  "commandType": "START_MINI_GAME"
                }
                """, host.getName().value()));

        stateResponses.get(2, TimeUnit.SECONDS); // DESCRIPTION
        stateResponses.get(6, TimeUnit.SECONDS); // PREPARE
        progressResponses.get(6, TimeUnit.SECONDS); // initial progress
        stateResponses.get(4, TimeUnit.SECONDS); // PLAYING

        // when - 모든 플레이어 STOP
        for (Player player : room.getPlayers()) {
            final String playerName = player.getName().value();
            session.send(stopUrl, String.format("""
                    {
                      "playerName": "%s"
                    }
                    """, playerName));

            await().atMost(Duration.ofSeconds(5))
                    .pollInterval(Duration.ofMillis(50))
                    .untilAsserted(() ->
                            assertThat(game.findPlayer(new PlayerName(playerName)).isStopped()).isTrue()
                    );
        }

        // then - DONE 상태 확인
        MessageResponse doneState = stateResponses.get(10, TimeUnit.SECONDS);
        assertMessageContains(doneState, "\"state\":\"DONE\"");
        assertMessageContains(doneState, "\"success\":true");
    }
}
