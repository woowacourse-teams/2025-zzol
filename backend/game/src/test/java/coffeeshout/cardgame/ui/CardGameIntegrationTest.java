package coffeeshout.cardgame.ui;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.GameModuleWebSocketTest;
import coffeeshout.cardgame.application.CardGameService;
import coffeeshout.cardgame.application.response.MiniGameStateMessage;
import coffeeshout.cardgame.domain.CardGame;
import coffeeshout.fixture.CardGameDeckStub;
import coffeeshout.fixture.CardGameFake;
import coffeeshout.fixture.GamerFixture;
import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.ui.request.CommandType;
import coffeeshout.minigame.ui.request.MiniGameMessage;
import coffeeshout.minigame.ui.request.command.SelectCardCommand;
import coffeeshout.room.domain.service.JoinCodeGenerator;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.Customization;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 타이밍 설정 (application-test.yml): firstLoading=500ms, prepare=500ms, playing=2000ms, scoreBoard=500ms, loading=500ms,
 * earlyFinishDelay=500ms
 */
class CardGameIntegrationTest extends GameModuleWebSocketTest {

    // application-test.yml 타이밍 값과 일치해야 함
    private static final long PLAYING_MS = 2000L;

    JoinCode joinCode;
    Gamer host;
    List<Gamer> gamers;
    TestStompSession session;
    CardGame cardGame;

    @Autowired
    GameSessionService gameSessionService;

    @Autowired
    CardGameService cardGameService;

    @Autowired
    ObjectMapper objectMapper;

    @BeforeEach
    void setUp(@Autowired JoinCodeGenerator joinCodeGenerator) throws Exception {
        joinCode = joinCodeGenerator.generate();
        host = GamerFixture.호스트_꾹이();
        gamers = GamerFixture.꾹이_루키_엠제이_한스();
        cardGame = new CardGameFake(new CardGameDeckStub());
        gameSessionService.deleteSession(joinCode);
        gameSessionService.initSession(joinCode, host);
        gameSessionService.getSession(joinCode).replaceGames(host, List.of(cardGame));
        session = createSession(joinCode.getValue(), host.getName());
    }

    @Test
    void 카드게임을_실행한다() {
        String subscribeUrl = String.format("/topic/room/%s/gameState", joinCode.getValue());

        var responses = session.subscribe(subscribeUrl);

        startCardGame();

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

        assertThat(payloadAs(firstRoundLoading, MiniGameStateMessage.class).cardGameState()).isEqualTo("FIRST_LOADING");
        assertThat(payloadAs(firstRoundLoading, MiniGameStateMessage.class).currentRound()).isEqualTo("FIRST");
        assertThat(payloadAs(firstRoundLoading, MiniGameStateMessage.class).allSelected()).isFalse();

        assertThat(payloadAs(prepare, MiniGameStateMessage.class).cardGameState()).isEqualTo("PREPARE");
        assertThat(payloadAs(firstRoundPlaying, MiniGameStateMessage.class).cardGameState()).isEqualTo("PLAYING");
        assertThat(payloadAs(firstRoundScoreBoard, MiniGameStateMessage.class).cardGameState()).isEqualTo("SCORE_BOARD");
        assertThat(payloadAs(secondRoundLoading, MiniGameStateMessage.class).cardGameState()).isEqualTo("LOADING");
        assertThat(payloadAs(secondRoundPlaying, MiniGameStateMessage.class).cardGameState()).isEqualTo("PLAYING");
        assertThat(payloadAs(secondRoundScoreBoard, MiniGameStateMessage.class).cardGameState()).isEqualTo("SCORE_BOARD");
        assertThat(payloadAs(done, MiniGameStateMessage.class).cardGameState()).isEqualTo("DONE");
    }

    @Test
    void 카드를_선택한다() throws Exception {
        String subscribeUrl = String.format("/topic/room/%s/gameState", joinCode.getValue());

        var responses = session.subscribe(subscribeUrl);

        startCardGame();

        responses.get(); // FIRST_LOADING
        responses.get(); // PREPARE
        responses.get(); // PLAYING

        selectCard("꾹이", 0);
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

        var responses = session.subscribe(subscribeUrl);

        startCardGame();

        responses.get(); // FIRST_LOADING
        responses.get(); // PREPARE
        responses.get(); // PLAYING

        // 4명 모두 카드 선택 (인덱스는 서로 달라야 함)
        selectCard("꾹이", 0);
        responses.get(); // 꾹이 선택 알림 (allSelected:false)

        selectCard("루키", 1);
        responses.get(); // 루키 선택 알림 (allSelected:false)

        selectCard("엠제이", 2);
        responses.get(); // 엠제이 선택 알림 (allSelected:false)

        selectCard("한스", 3);
        MessageResponse lastSelection = responses.get(); // 한스 선택 알림 (allSelected:true)

        // 전원 선택 완료 → earlyFinishDelay 후 SCORE_BOARD 도달
        // playing 제한시간(2000ms)을 기다리지 않고 earlyFinishDelay(500ms) 후 도달해야 함
        MessageResponse scoreBoard = responses.get(3, TimeUnit.SECONDS);

        assertThat(payloadAs(lastSelection, MiniGameStateMessage.class).allSelected()).isTrue();
        assertThat(payloadAs(scoreBoard, MiniGameStateMessage.class).cardGameState()).isEqualTo("SCORE_BOARD");

        // 핵심 검증: playing 제한시간(2000ms)보다 훨씬 빠르게 전환됨
        assertThat(scoreBoard.duration())
                .as("조기 전환은 playing 제한시간(%dms)보다 짧아야 합니다", PLAYING_MS)
                .isLessThan(PLAYING_MS);
    }

    /**
     * WS START 커맨드(Room 검증·영속 경유) 대신 :game 서비스를 직접 호출해 게임을 시작한다.
     * {@code startGame}으로 READY→PLAYING 전이 후 {@code start}로 플로우를 스케줄한다(프로덕션 onGameStartReady와 동일 순서).
     */
    private void startCardGame() {
        // 구독 등록 완료 보장 후 시작 — 등록 전 첫 브로드캐스트 유실(subscribe→publish 레이스) 방지 (#1410)
        session.awaitSubscribed();
        gameSessionService.startGame(joinCode, host, gamers);
        cardGameService.start(joinCode.getValue(), host.getName());
    }

    /**
     * SELECT_CARD는 방 검증과 무관한 :game 커맨드이므로 실제 WebSocket 전송 경로(dispatch→Stream→Consumer→service→broadcast)를
     * 그대로 검증한다. 커맨드 봉투는 {@code MiniGameMessage}, 본문은 실제 {@code SelectCardCommand} 레코드를 타입으로 직렬화한다.
     */
    private void selectCard(String playerName, int cardIndex) {
        session.send(commandUrl(), new MiniGameMessage(
                CommandType.SELECT_CARD,
                objectMapper.valueToTree(new SelectCardCommand(playerName, cardIndex))));
    }

    private String commandUrl() {
        return String.format("/app/room/%s/minigame/command", joinCode.getValue());
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
