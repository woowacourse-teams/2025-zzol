package coffeeshout.minigame.cardgame.ui;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.fixture.CardGameDeckStub;
import coffeeshout.fixture.CardGameFake;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.fixture.TestStompSession;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import coffeeshout.global.MessageResponse;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.springframework.beans.factory.annotation.Autowired;

class CardGameIntegrationTest extends WebSocketIntegrationTestSupport {

    JoinCode joinCode;

    Player host;

    TestStompSession session;

    CardGame cardGame;

    @BeforeEach
    void setUp(@Autowired RoomRepository roomRepository,
               @Autowired RoomJpaRepository roomJpaRepository) throws Exception {
        joinCode = new JoinCode("A4BX");
        Room room = RoomFixture.호스트_꾹이();
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        host = room.getHost();
        cardGame = new CardGameFake(new CardGameDeckStub());
        room.addMiniGame(host.getName(), cardGame);

        // MemoryRepository에 저장
        roomRepository.save(room);

        // DB에 RoomEntity 저장 (Redis 이벤트 핸들러가 DB에서 조회하므로 필요)
        RoomEntity roomEntity = new RoomEntity(joinCode.getValue());
        roomJpaRepository.save(roomEntity);

        session = createSession();
    }

    @Test
    void 카드게임을_실행한다() throws JSONException {
        // given
        String joinCodeValue = joinCode.getValue();
        String subscribeUrlFormat = String.format("/topic/room/%s/gameState", joinCodeValue);
        String requestUrlFormat = String.format("/app/room/%s/minigame/command", joinCodeValue);

        var responses = session.subscribe(subscribeUrlFormat);

        session.send(requestUrlFormat, String.format("""
                {
                  "commandType": "START_MINI_GAME",
                  "commandRequest": {
                    "hostName": "%s"
                  }
                }
                """, host.getName().value()));

        MessageResponse firstRoundLoading = responses.get();
        MessageResponse prepare = responses.get();
        MessageResponse firstRoundPlaying = responses.get();
        MessageResponse firstRoundScoreBoard = responses.get(11, TimeUnit.SECONDS);
        MessageResponse secondRoundLoading = responses.get();
        MessageResponse secondRoundPlaying = responses.get();
        MessageResponse secondRoundScoreBoard = responses.get(11, TimeUnit.SECONDS);
        MessageResponse done = responses.get();

        assertMessageCustomization(firstRoundLoading, """
                {
                   "success":true,
                   "data":{
                      "cardGameState":"FIRST_LOADING",
                      "currentRound":"FIRST",
                      "cardInfoMessages":[
                         {
                            "cardType":"ADDITION",
                            "value":40,
                            "selected":false,
                            "playerName":null,
                            "colorIndex":null
                         },
                         {
                            "cardType":"ADDITION",
                            "value":30,
                            "selected":false,
                            "playerName":null,
                            "colorIndex":null
                         },
                         {
                            "cardType":"ADDITION",
                            "value":20,
                            "selected":false,
                            "playerName":null,
                            "colorIndex":null
                         },
                         {
                            "cardType":"ADDITION",
                            "value":10,
                            "selected":false,
                            "playerName":null,
                            "colorIndex":null
                         },
                         {
                            "cardType":"ADDITION",
                            "value":0,
                            "selected":false,
                            "playerName":null,
                            "colorIndex":null
                         },
                         {
                            "cardType":"ADDITION",
                            "value":-10,
                            "selected":false,
                            "playerName":null,
                            "colorIndex":null
                         },
                         {
                            "cardType":"MULTIPLIER",
                            "value":4,
                            "selected":false,
                            "playerName":null,
                            "colorIndex":null
                         },
                         {
                            "cardType":"MULTIPLIER",
                            "value":2,
                            "selected":false,
                            "playerName":null,
                            "colorIndex":null
                         },
                         {
                            "cardType":"MULTIPLIER",
                            "value":0,
                            "selected":false,
                            "playerName":null,
                            "colorIndex":null
                         }
                      ],
                      "allSelected":false
                   },
                   "errorMessage":null
                }
                """, getColorIndexCustomization());

        assertMessageContains(prepare, 4000L, "\"cardGameState\":\"PREPARE\"");
        assertMessageContains(firstRoundPlaying, 2000L, "\"cardGameState\":\"PLAYING\"");
        assertMessageContains(firstRoundScoreBoard, 10250L, "\"cardGameState\":\"SCORE_BOARD\"");
        assertMessageContains(secondRoundLoading, 1500L, "\"cardGameState\":\"LOADING\"");
        assertMessageContains(secondRoundPlaying, 3000L, "\"cardGameState\":\"PLAYING\"");
        assertMessageContains(secondRoundScoreBoard, 10250L, "\"cardGameState\":\"SCORE_BOARD\"");
        assertMessageContains(done, "\"cardGameState\":\"DONE\"");
    }

    @Test
    void 카드를_선택한다() throws Exception {
        // given
        String subscribeUrlFormat = String.format("/topic/room/%s/gameState", joinCode.getValue());
        String requestUrlFormat = String.format("/app/room/%s/minigame/command", joinCode.getValue());

        var responses = session.subscribe(subscribeUrlFormat);

        session.send(requestUrlFormat, String.format("""
                {
                  "commandType": "START_MINI_GAME",
                  "commandRequest": {
                    "hostName": "%s"
                  }
                }
                """, host.getName().value()));

        responses.get(); // FIRST_LOADING
        responses.get(); // PREPARE
        responses.get(); // PLAYING

        // when
        session.send(requestUrlFormat, """
                {
                   "commandType": "SELECT_CARD",
                   "commandRequest": {
                     "playerName": "꾹이",
                     "cardIndex": 0
                   }
                }
                """);
        MessageResponse firstRoundPlaying = responses.get();

        // then
        assertMessageCustomization(firstRoundPlaying, """
                {
                    "success":true,
                    "data":{
                        "cardGameState":"PLAYING",
                        "currentRound":"FIRST",
                        "cardInfoMessages":[
                            {
                                "cardType":"MULTIPLIER",
                                "value":2,
                                "selected":true,
                                "playerName":"꾹이",
                                "colorIndex":4
                            },
                            {
                                "cardType":"ADDITION",
                                "value":30,
                                "selected":false,
                                "playerName":null,
                                "colorIndex":null
                            },
                            {
                                "cardType":"MULTIPLIER",
                                "value":0,
                                "selected":false,
                                "playerName":null,
                                "colorIndex":null
                            },
                            {
                                "cardType":"ADDITION",
                                "value":0,
                                "selected":false,
                                "playerName":null,
                                "colorIndex":null
                            },
                            {
                                "cardType":"ADDITION",
                                "value":-10,
                                "selected":false,
                                "playerName":null,
                                "colorIndex":null
                            },
                            {
                                "cardType":"MULTIPLIER",
                                "value":4,
                                "selected":false,
                                "playerName":null,
                                "colorIndex":null
                            },
                            {
                                "cardType":"ADDITION",
                                "value":10,
                                "selected":false,
                                "playerName":null,
                                "colorIndex":null
                            },
                            {
                                "cardType":"ADDITION"
                                ,"value":20,
                                "selected":false,
                                "playerName":null,
                                "colorIndex":null
                            },
                            {
                                "cardType":"ADDITION",
                                "value":40,
                                "selected":false,
                                "playerName":null,
                                "colorIndex":null
                            }
                        ],
                        "allSelected":false
                    },
                    "errorMessage":null
                }
                """, getColorIndexCustomization());
    }

    private static Customization getColorIndexCustomization() {
        return new Customization("colorIndex", (actual, expect) -> {
            if (expect instanceof Integer value) {
                return value >= 0 && value <= 9;
            }
            return true;
        });
    }
}
