package coffeeshout.speedtouch.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.support.TestStompSession;
import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.support.MessageResponse;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.speedtouch.domain.SpeedTouchGame;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SpeedTouchGameIntegrationTest extends GameModuleWebSocketTest {

    JoinCode joinCode;
    Player host;
    TestStompSession session;
    Room room;
    SpeedTouchGame game;

    @BeforeEach
    void setUp(@Autowired RoomRepository roomRepository) throws Exception {
        joinCode = new JoinCode("A4BX");
        room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        host = room.getHost();
        game = new SpeedTouchGame();
        room.addMiniGame(new PlayerName(host.getName().value()), game);
        roomRepository.save(room);
        session = createSession(joinCode, host.getName());
    }

    @Test
    void 스피드터치_게임을_시작하면_상태_변경_메시지가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/speed-touch/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/speed-touch/progress", joinCodeValue);
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

        // then - DESCRIPTION 상태
        MessageResponse descriptionState = stateResponses.get(2, TimeUnit.SECONDS);
        assertMessageContains(descriptionState, "\"state\":\"DESCRIPTION\"");
        assertMessageContains(descriptionState, "\"success\":true");

        // PREPARE 상태
        MessageResponse prepareState = stateResponses.get(6, TimeUnit.SECONDS);
        assertMessageContains(prepareState, "\"state\":\"PREPARE\"");

        // PLAYING 상태
        MessageResponse playingState = stateResponses.get(4, TimeUnit.SECONDS);
        assertMessageContains(playingState, "\"state\":\"PLAYING\"");
    }

    @Test
    void 터치하면_진행도_브로드캐스트_메시지가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/speed-touch/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/speed-touch/progress", joinCodeValue);
        final String startUrl = String.format("/app/room/%s/minigame/command", joinCodeValue);
        final String touchUrl = String.format("/app/room/%s/speed-touch/touch", joinCodeValue);

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

        // when - 터치
        session.send(touchUrl, String.format("""
                {
                  "playerName": "%s",
                  "touchedNumber": 1
                }
                """, host.getName().value()));

        // then - 진행도 응답 (blocking get으로 메시지 도착까지 대기)
        MessageResponse progressUpdate = progressResponses.get(3, TimeUnit.SECONDS);
        assertMessageContains(progressUpdate, "\"success\":true");
        assertMessageContains(progressUpdate, "\"players\"");
    }

    @Test
    void 전원_완주하면_DONE_상태가_전송된다() {
        // given
        final String joinCodeValue = joinCode.getValue();
        final String subscribeStateUrl = String.format("/topic/room/%s/speed-touch/state", joinCodeValue);
        final String subscribeProgressUrl = String.format("/topic/room/%s/speed-touch/progress", joinCodeValue);
        final String startUrl = String.format("/app/room/%s/minigame/command", joinCodeValue);
        final String touchUrl = String.format("/app/room/%s/speed-touch/touch", joinCodeValue);

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

        // when - 모든 플레이어 1~25 터치
        // 각 플레이어별로 1~25를 순차 전송하되, 이전 터치 처리 완료를 Awaitility로 확인
        for (Player player : room.getPlayers()) {
            final String playerName = player.getName().value();
            for (int i = 1; i <= 25; i++) {
                final int expectedNext = i + 1;
                session.send(touchUrl, String.format("""
                        {
                          "playerName": "%s",
                          "touchedNumber": %d
                        }
                        """, playerName, i));

                // 게임 도메인 객체의 currentNumber가 갱신될 때까지 대기
                await().atMost(Duration.ofSeconds(5))
                        .pollInterval(Duration.ofMillis(50))
                        .untilAsserted(() ->
                                assertThat(game.findPlayer(new PlayerName(playerName)).getCurrentNumber())
                                        .isGreaterThanOrEqualTo(expectedNext)
                        );
            }
        }

        // then - DONE 상태 확인
        MessageResponse doneState = stateResponses.get(10, TimeUnit.SECONDS);
        assertMessageContains(doneState, "\"state\":\"DONE\"");
        assertMessageContains(doneState, "\"success\":true");
    }
}
