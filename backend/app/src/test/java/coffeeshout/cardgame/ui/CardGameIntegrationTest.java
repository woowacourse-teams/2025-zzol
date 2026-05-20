package coffeeshout.cardgame.ui;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.fixture.CardGameDeckStub;
import coffeeshout.fixture.CardGameFake;
import coffeeshout.fixture.GameSessionFixture;
import coffeeshout.fixture.RoomFixture;
import coffeeshout.fixture.TestStompSession;
import coffeeshout.fixture.WebSocketIntegrationTestSupport;
import coffeeshout.MessageResponse;
import coffeeshout.minigame.domain.GameSessionRepository;
import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.player.Player;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.room.infra.persistence.RoomEntity;
import coffeeshout.room.infra.persistence.RoomJpaRepository;
import java.util.concurrent.TimeUnit;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 타이밍 설정 (application-test.yml): firstLoading=500ms, prepare=500ms, playing=2000ms, scoreBoard=500ms, loading=500ms,
 * earlyFinishDelay=500ms
 */
class CardGameIntegrationTest extends WebSocketIntegrationTestSupport {

    // application-test.yml 타이밍 값과 일치해야 함
    private static final long FIRST_LOADING_MS = 500L;
    private static final long PREPARE_MS = 500L;
    private static final long PLAYING_MS = 2000L;
    private static final long SCORE_BOARD_MS = 500L;
    private static final long LOADING_MS = 500L;

    JoinCode joinCode;
    Player host;
    TestStompSession session;
    CardGame cardGame;

    @BeforeEach
    void setUp(@Autowired RoomRepository roomRepository,
               @Autowired RoomJpaRepository roomJpaRepository,
               @Autowired JoinCodeGenerator joinCodeGenerator,
               @Autowired GameSessionRepository gameSessionRepository
    ) throws Exception {
        joinCode = joinCodeGenerator.generate();
        Room room = RoomFixture.호스트_꾹이(joinCode);
        room.getPlayers().forEach(player -> player.updateReadyState(true));
        host = room.getHost();
        cardGame = new CardGameFake(new CardGameDeckStub());
        gameSessionRepository.save(GameSessionFixture.게임세션_게임대기(joinCode, cardGame, host.getName()));

        roomRepository.save(room);

        RoomEntity roomEntity = new RoomEntity(joinCode.getValue());
        roomJpaRepository.save(roomEntity);

        session = createSession(joinCode, host.getName());
    }

    @Test
    void ㅁㅁㅁ카드게임을_실행한다() throws JSONException {
        String joinCodeValue = joinCode.getValue();
        String subscribeUrl = String.format("/topic/room/%s/gameState", joinCodeValue);
        String requestUrl = String.format("/app/room/%s/minigame/command", joinCodeValue);

        var responses = session.subscribe(subscribeUrl);

        session.send(requestUrl, String.format("""
                {
                  "commandType": "START_MINI_GAME",
                  "commandRequest": { "hostName": "%s" }
                }
                """, host.getName().value()));

        // 1라운드
        MessageResponse firstRoundLoading = responses.get();
        MessageResponse prepare = responses.get();
        MessageResponse firstRoundPlaying = responses.get();
        MessageResponse firstRoundScoreBoard = responses.get(3, TimeUnit.SECONDS);

        // 2라운드 (PREPARE 없음)
        MessageResponse secondRoundLoading = responses.get();
        MessageResponse secondRoundPlaying = responses.get();
        MessageResponse secondRoundScoreBoard = responses.get(3, TimeUnit.SECONDS);

        // 게임 종료
        MessageResponse done = responses.get();

        assertMessageCustomization(firstRoundLoading, """
                {
                   "success":true,
                   "data":{
                      "cardGameState":"FIRST_LOADING",
                      "currentRound":"FIRST",
                      "allSelected":false
                   }
                }
                """, getColorIndexCustomization());

        assertMessageContains(prepare, FIRST_LOADING_MS, "\"cardGameState\":\"PREPARE\"");
        assertMessageContains(firstRoundPlaying, PREPARE_MS, "\"cardGameState\":\"PLAYING\"");
        assertMessageContains(firstRoundScoreBoard, PLAYING_MS, "\"cardGameState\":\"SCORE_BOARD\"");
        assertMessageContains(secondRoundLoading, SCORE_BOARD_MS, "\"cardGameState\":\"LOADING\"");
        assertMessageContains(secondRoundPlaying, LOADING_MS, "\"cardGameState\":\"PLAYING\"");
        assertMessageContains(secondRoundScoreBoard, PLAYING_MS, "\"cardGameState\":\"SCORE_BOARD\"");
        assertMessageContains(done, SCORE_BOARD_MS, "\"cardGameState\":\"DONE\"");
    }

    @Test
    void 카드를_선택한다() throws JSONException {
        String subscribeUrl = String.format("/topic/room/%s/gameState", joinCode.getValue());
        String requestUrl = String.format("/app/room/%s/minigame/command", joinCode.getValue());

        var responses = session.subscribe(subscribeUrl);

        session.send(requestUrl, String.format("""
                {
                  "commandType": "START_MINI_GAME",
                  "commandRequest": { "hostName": "%s" }
                }
                """, host.getName().value()));

        responses.get(); // FIRST_LOADING
        responses.get(); // PREPARE
        responses.get(); // PLAYING

        session.send(requestUrl, """
                {
                   "commandType": "SELECT_CARD",
                   "commandRequest": { "playerName": "꾹이", "cardIndex": 0 }
                }
                """);
        MessageResponse afterSelect = responses.get();

        assertMessageCustomization(afterSelect, """
                {
                    "success":true,
                    "data":{
                        "cardGameState":"PLAYING",
                        "currentRound":"FIRST",
                        "allSelected":false,
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
                        ]
                    }
                }
                """, getColorIndexCustomization());
    }

    /**
     * 모든 플레이어(4명)가 PLAYING 중 카드를 선택하면, 남은 playing 시간을 기다리지 않고 earlyFinishDelay(500ms) 후 바로 SCORE_BOARD로 전환되는지 검증.
     * <p>
     * 룸 구성: 꾹이(호스트), 루키, 엠제이, 한스
     */
    @Test
    void 전원_카드선택_완료시_조기_라운드전환() {
        String subscribeUrl = String.format("/topic/room/%s/gameState", joinCode.getValue());
        String requestUrl = String.format("/app/room/%s/minigame/command", joinCode.getValue());

        var responses = session.subscribe(subscribeUrl);

        session.send(requestUrl, String.format("""
                {
                  "commandType": "START_MINI_GAME",
                  "commandRequest": { "hostName": "%s" }
                }
                """, host.getName().value()));

        responses.get(); // FIRST_LOADING
        responses.get(); // PREPARE
        responses.get(); // PLAYING

        // 4명 모두 카드 선택 (인덱스는 서로 달라야 함)
        session.send(requestUrl, """
                { "commandType": "SELECT_CARD", "commandRequest": { "playerName": "꾹이", "cardIndex": 0 } }
                """);
        responses.get(); // 꾹이 선택 알림 (allSelected:false)

        session.send(requestUrl, """
                { "commandType": "SELECT_CARD", "commandRequest": { "playerName": "루키", "cardIndex": 1 } }
                """);
        responses.get(); // 루키 선택 알림 (allSelected:false)

        session.send(requestUrl, """
                { "commandType": "SELECT_CARD", "commandRequest": { "playerName": "엠제이", "cardIndex": 2 } }
                """);
        responses.get(); // 엠제이 선택 알림 (allSelected:false)

        session.send(requestUrl, """
                { "commandType": "SELECT_CARD", "commandRequest": { "playerName": "한스", "cardIndex": 3 } }
                """);
        MessageResponse lastSelection = responses.get(); // 한스 선택 알림 (allSelected:true)

        // 전원 선택 완료 → earlyFinishDelay 후 SCORE_BOARD 도달
        // playing 제한시간(2000ms)을 기다리지 않고 earlyFinishDelay(500ms) 후 도달해야 함
        MessageResponse scoreBoard = responses.get(3, TimeUnit.SECONDS);

        assertMessageContains(lastSelection, "\"allSelected\":true");
        assertMessageContains(scoreBoard, "\"cardGameState\":\"SCORE_BOARD\"");

        // 핵심 검증: playing 제한시간(2000ms)보다 훨씬 빠르게 전환됨
        assertThat(scoreBoard.duration())
                .as("조기 전환은 playing 제한시간(%dms)보다 짧아야 합니다", PLAYING_MS)
                .isLessThan(PLAYING_MS);
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
