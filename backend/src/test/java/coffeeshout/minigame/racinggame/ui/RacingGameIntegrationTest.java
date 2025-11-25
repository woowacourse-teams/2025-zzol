package coffeeshout.minigame.racinggame.ui;

import coffeeshout.fixture.RoomFixture;
import coffeeshout.fixture.TestStompSession;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import coffeeshout.global.MessageResponse;
import coffeeshout.racinggame.domain.RacingGame;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.RoomRepository;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RacingGameIntegrationTest extends WebSocketIntegrationTestSupport {

    JoinCode joinCode;
    Player host;
    TestStompSession session;
    Room room;
    RacingGame racingGame;

    @BeforeEach
    void setUp(@Autowired RoomRepository roomRepository) throws Exception {
        joinCode = new JoinCode("A4BX");
        room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        host = room.getHost();
        racingGame = new RacingGame();
        room.addMiniGame(new PlayerName(host.getName().value()), racingGame);
        roomRepository.save(room);
        session = createSession();
    }

    @Test
    void 레이싱_게임을_시작한다() {
        // given
        String joinCodeValue = joinCode.getValue();
        String subscribeStateUrl = String.format("/topic/room/%s/racing-game/state", joinCodeValue);
        String subscribePositionUrl = String.format("/topic/room/%s/racing-game", joinCodeValue);
        String requestUrl = String.format("/app/room/%s/minigame/command", joinCodeValue);

        var stateResponses = session.subscribe(subscribeStateUrl);
        var positionResponses = session.subscribe(subscribePositionUrl);

        // when - command 전송
        session.send(requestUrl, String.format("""
                {
                  "commandRequest": {
                    "hostName": "%s"
                  },
                  "commandType": "START_MINI_GAME"
                }
                """, host.getName().value()));

        // then - 첫 번째 응답: DESCRIPTION 상태 (4초 후)
        MessageResponse descriptionState = stateResponses.get(1, TimeUnit.SECONDS);
        assertMessageContains(descriptionState, "\"state\":\"DESCRIPTION\"");
        assertMessageContains(descriptionState, "\"success\":true");

        // 두 번째 응답: PREPARE 상태 (추가 2초 후)
        MessageResponse prepareState = stateResponses.get(5, TimeUnit.SECONDS);
        assertMessageContains(prepareState, "\"state\":\"PREPARE\"");
        assertMessageContains(prepareState, "\"success\":true");

        // 세 번째 응답: PLAYING 상태 (바로 이어서)
        MessageResponse playingState = stateResponses.get(3, TimeUnit.SECONDS);
        assertMessageContains(playingState, "\"state\":\"PLAYING\"");
        assertMessageContains(playingState, "\"success\":true");

        // 자동 이동으로 위치 업데이트 메시지가 계속 발행됨
        MessageResponse positionUpdate1 = positionResponses.get(1, TimeUnit.SECONDS);
        assertMessageContains(positionUpdate1, "\"position\"");
        assertMessageContains(positionUpdate1, "\"distance\"");
    }

    @Test
    void 게임이_완주되면_FINISHED_상태가_전송된다() throws Exception {
        TestStompSession singleSession = createSession();
        String joinCodeValue = joinCode.getValue();
        String subscribeStateUrl = String.format("/topic/room/%s/racing-game/state", joinCodeValue);
        String startRequestUrl = String.format("/app/room/%s/minigame/command", joinCodeValue);
        String tapRequestUrl = String.format("/app/room/%s/racing-game/tap", joinCodeValue);

        var stateResponses = singleSession.subscribe(subscribeStateUrl);

        // 게임 시작
        singleSession.send(startRequestUrl, String.format("""
                {
                  "commandRequest": {
                    "hostName": "%s"
                  },
                  "commandType": "START_MINI_GAME"
                }
                """, host.getName().value()));

        stateResponses.get(1, TimeUnit.SECONDS); // DESCRIPTION
        stateResponses.get(5, TimeUnit.SECONDS); // PREPARE (4초 후)
        stateResponses.get(3, TimeUnit.SECONDS); // PLAYING (2초 후)

        /*
         100ms마다 moveAll → 최고속도(30)로 달린다고 가정하면 결승점(3000)까지 10000ms걸림
        */
        for (int i = 0; i < 100; i++) {
            for (Player player : room.getPlayers()) {
                singleSession.send(tapRequestUrl, String.format("""
                        {
                          "playerName": "%s",
                          "tapCount": 10
                        }
                        """, player.getName().value()));
            }
            Thread.sleep(100);
        }

        // then - DONE 상태 확인 (최대 15초 대기)
        MessageResponse finishedState = stateResponses.get(30, TimeUnit.SECONDS);
        assertMessageContains(finishedState, "\"state\":\"DONE\"");
        assertMessageContains(finishedState, "\"success\":true");
    }
}
