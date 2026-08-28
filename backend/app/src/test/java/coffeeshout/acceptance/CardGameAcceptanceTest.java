package coffeeshout.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.room.application.service.RoomCreateResult;
import coffeeshout.room.application.service.RoomEnterResult;
import coffeeshout.room.application.service.RoomService;
import coffeeshout.room.domain.repository.RoomRepository;
import coffeeshout.room.ui.request.MiniGameSelectMessage;
import coffeeshout.room.ui.request.ReadyChangeMessage;
import coffeeshout.support.MessageResponse;
import coffeeshout.support.TestStompSession;
import coffeeshout.support.app.WebSocketIntegrationTestSupport;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 카드게임 인수 테스트 — 공개 스위트 (최소 계약 실험).
 *
 * <p><b>동결 컨트랙트(WS·REST)와 룸 경로(재구현 대상 아님)만 사용한다.</b> 카드게임 구현 클래스를 import하지
 * 않으므로 카드게임을 어떤 내부 구조로 재구현해도 그대로 컴파일·실행된다. 커맨드는 컨트랙트 JSON 문자열
 * 리터럴로 보내고, 브로드캐스트는 JSON 트리로 검증한다.
 *
 * <p><b>이 파일은 수정·삭제 금지다(실험 수칙).</b> 재구현은 이 스위트를 전부 통과해야 한다.
 *
 * <p>방 셋업은 {@code RoomFixture}+리포지토리 주입 대신 실제 서비스 경로(createRoom/enterRoomAsync)를
 * 쓴다 — 인수 테스트의 목적이 컨트랙트 전 구간 검증이기 때문이다. 타이밍(application-test-game.yml):
 * first-loading/loading/prepare/score-board/early-finish-delay=500ms, playing=2000ms.
 */
@Tag("acceptance")
class CardGameAcceptanceTest extends WebSocketIntegrationTestSupport {

    private static final String HOST = "꾹이";
    private static final List<String> GUESTS = List.of("루키", "엠제이", "한스");
    private static final long PLAYING_MS = 2000L;

    @Autowired
    RoomService roomService;

    @Autowired
    RoomRepository roomRepository;

    @Autowired
    ObjectMapper objectMapper;

    String joinCode;
    Map<String, TestStompSession> sessions;

    @BeforeEach
    void setUp() throws Exception {
        sessions = new LinkedHashMap<>();

        final RoomCreateResult created = roomService.createRoom(HOST);
        joinCode = created.room().getJoinCode().getValue();
        sessions.put(HOST, createSessionWithRoomToken(created.roomSessionToken()));

        for (final String guest : GUESTS) {
            final RoomEnterResult entered =
                    roomService.enterRoomAsync(joinCode, guest).get(5, TimeUnit.SECONDS);
            sessions.put(guest, createSessionWithRoomToken(entered.roomSessionToken()));
        }

        for (final String guest : GUESTS) {
            sessions.get(guest).send(
                    "/app/room/%s/update-ready".formatted(joinCode),
                    new ReadyChangeMessage(joinCode, guest, true));
        }
        // ready 반영은 Stream 비동기 — 방이 시작 가능한 상태가 될 때까지 게이트한다(Room 공개 API만 사용)
        await().atMost(Duration.ofSeconds(5)).until(() -> {
            try {
                roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow().validateStartable(HOST);
                return true;
            } catch (final Exception e) {
                return false;
            }
        });

        // 카드게임을 대기열에 선택 (구현 클래스가 아니라 타입 이름만 — 컨트랙트 경로)
        final var minigameTopic = sessions.get(HOST).subscribe("/topic/room/%s/minigame".formatted(joinCode));
        sessions.get(HOST).send(
                "/app/room/%s/update-minigames".formatted(joinCode),
                new MiniGameSelectMessage(HOST, List.of(MiniGameType.CARD_GAME)));
        minigameTopic.get();
    }

    @AfterEach
    void tearDown() {
        sessions.values().forEach(TestStompSession::disconnect);
    }

    @Test
    void 게임_시작부터_종료까지_컨트랙트_상태_시퀀스대로_브로드캐스트된다() throws Exception {
        final var gameStates = subscribeGameState();
        final var rounds = sessions.get(HOST).subscribe("/topic/room/%s/round".formatted(joinCode));

        startGame(HOST);

        // 시작 알림 — round 토픽 (gameState와의 도착 순서는 컨트랙트상 비보장)
        final JsonNode roundData = data(rounds.get());
        assertThat(roundData.path("miniGameType").asText()).isEqualTo("CARD_GAME");

        // 상태 시퀀스: 선택이 없으므로 전이 브로드캐스트가 정확히 8회
        final JsonNode first = data(gameStates.get());
        final String[] expectedAfterFirst =
                {"PREPARE", "PLAYING", "SCORE_BOARD", "LOADING", "PLAYING", "SCORE_BOARD", "DONE"};

        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(first.path("cardGameState").asText()).isEqualTo("FIRST_LOADING");
        softly.assertThat(first.path("currentRound").asText()).isEqualTo("FIRST");
        softly.assertThat(first.path("allSelected").asBoolean()).isFalse();
        assertDeckShape(softly, first.path("cardInfoMessages"));
        for (final String expected : expectedAfterFirst) {
            final MessageResponse response = gameStates.get(5, TimeUnit.SECONDS);
            softly.assertThat(data(response).path("cardGameState").asText()).isEqualTo(expected);
        }
        softly.assertAll();
    }

    @Test
    void 카드_선택이_현재_라운드_스냅샷에_반영된다() throws Exception {
        final var gameStates = subscribeGameState();
        startGame(HOST);
        awaitGameState(gameStates, "PLAYING");

        selectCard(HOST, 0);
        final JsonNode afterSelect = data(gameStates.get());

        final JsonNode cards = afterSelect.path("cardInfoMessages");
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(afterSelect.path("cardGameState").asText()).isEqualTo("PLAYING");
        softly.assertThat(afterSelect.path("allSelected").asBoolean()).isFalse();
        softly.assertThat(cards.get(0).path("selected").asBoolean()).isTrue();
        softly.assertThat(cards.get(0).path("playerName").asText()).isEqualTo(HOST);
        softly.assertThat(cards.get(0).path("colorIndex").asInt()).isBetween(0, 9);
        for (int i = 1; i < cards.size(); i++) {
            softly.assertThat(cards.get(i).path("selected").asBoolean()).isFalse();
            softly.assertThat(cards.get(i).path("playerName").isNull()).isTrue();
        }
        softly.assertAll();
    }

    @Test
    void 전원이_선택하면_잔여시간을_기다리지_않고_점수판으로_조기_전환된다() throws Exception {
        final var gameStates = subscribeGameState();
        startGame(HOST);
        awaitGameState(gameStates, "PLAYING");

        selectCard(HOST, 0);
        gameStates.get();
        int index = 1;
        for (final String guest : GUESTS.subList(0, 2)) {
            selectCard(guest, index++);
            gameStates.get();
        }
        selectCard(GUESTS.get(2), index);
        final JsonNode lastSelection = data(gameStates.get());

        final MessageResponse scoreBoard = gameStates.get(3, TimeUnit.SECONDS);

        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(lastSelection.path("allSelected").asBoolean()).isTrue();
        softly.assertThat(data(scoreBoard).path("cardGameState").asText()).isEqualTo("SCORE_BOARD");
        softly.assertThat(scoreBoard.duration())
                .as("전원 선택 후에는 playing 제한시간(%dms)을 기다리지 않아야 한다", PLAYING_MS)
                .isLessThan(PLAYING_MS);
        softly.assertAll();
    }

    @Test
    void 플레이_상태가_아닐_때의_선택은_게임_상태를_바꾸지_못한다() throws Exception {
        final var gameStates = subscribeGameState();
        startGame(HOST);
        awaitGameState(gameStates, "DONE");

        // DONE 이후의 선택 — 거부된 명령은 어떤 상태 브로드캐스트도 만들지 않아야 한다
        selectCard(HOST, 0);
        gameStates.assertNoMessage(1500, TimeUnit.MILLISECONDS);
    }

    @Test
    void 이미_선택된_카드는_다른_플레이어가_가져갈_수_없다() throws Exception {
        final var gameStates = subscribeGameState();
        startGame(HOST);
        awaitGameState(gameStates, "PLAYING");

        selectCard(HOST, 0);
        gameStates.get();

        selectCard(GUESTS.get(0), 0);   // 중복 카드 — 거부되어야 한다
        selectCard(GUESTS.get(0), 1);   // 거부된 플레이어는 다른 카드를 선택할 수 있다
        final JsonNode snapshot = awaitCardOwned(gameStates, 1);

        final JsonNode cards = snapshot.path("cardInfoMessages");
        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(cards.get(0).path("playerName").asText())
                .as("중복 선택 시도가 기존 소유자를 바꾸면 안 된다")
                .isEqualTo(HOST);
        softly.assertThat(cards.get(1).path("playerName").asText()).isEqualTo(GUESTS.get(0));
        softly.assertAll();
    }

    @Test
    void 잘못된_카드_인덱스_선택은_게임을_깨뜨리지_않는다() throws Exception {
        final var gameStates = subscribeGameState();
        startGame(HOST);
        awaitGameState(gameStates, "PLAYING");

        selectCard(HOST, 99);   // 범위 밖 — 거부되어야 한다
        selectCard(HOST, 0);    // 거부 후에도 정상 선택은 동작해야 한다
        final JsonNode snapshot = awaitCardOwned(gameStates, 0);

        assertThat(snapshot.path("cardInfoMessages").get(0).path("playerName").asText()).isEqualTo(HOST);
        awaitGameState(gameStates, "SCORE_BOARD");
    }

    @Test
    void 방장이_아닌_플레이어는_게임을_시작시킬_수_없다() throws Exception {
        final var gameStates = subscribeGameState();
        final var rounds = sessions.get(HOST).subscribe("/topic/room/%s/round".formatted(joinCode));

        startGame(GUESTS.get(0));
        rounds.assertNoMessage(2, TimeUnit.SECONDS);
        gameStates.assertNoMessage(500, TimeUnit.MILLISECONDS);

        // 시스템은 살아 있어야 한다 — 진짜 방장의 시작은 정상 동작
        startGame(HOST);
        awaitGameState(gameStates, "FIRST_LOADING");
    }

    private TestStompSession.MessageCollector subscribeGameState() {
        return sessions.get(HOST).subscribe("/topic/room/%s/gameState".formatted(joinCode));
    }

    /** 커맨드는 컨트랙트 JSON 그대로 보낸다 — 서버측 요청 클래스에 컴파일 의존하지 않는다. */
    private void startGame(String hostName) {
        sessions.get(hostName).send(commandUrl(),
                "{\"commandType\":\"START_MINI_GAME\",\"commandRequest\":{\"hostName\":\"%s\"}}".formatted(hostName));
    }

    private void selectCard(String playerName, int cardIndex) {
        sessions.get(playerName).send(commandUrl(),
                "{\"commandType\":\"SELECT_CARD\",\"commandRequest\":{\"playerName\":\"%s\",\"cardIndex\":%d}}"
                        .formatted(playerName, cardIndex));
    }

    private String commandUrl() {
        return "/app/room/%s/minigame/command".formatted(joinCode);
    }

    private JsonNode data(MessageResponse response) {
        final JsonNode root = objectMapper.readTree(response.payload());
        assertThat(root.path("success").asBoolean()).as("브로드캐스트는 success=true 래퍼여야 한다").isTrue();
        return root.path("data");
    }

    /** 중복 스냅샷·순서 밀림에 안전하게, 목표 상태가 나올 때까지 훑는다 (awaitState 패턴). */
    private JsonNode awaitGameState(TestStompSession.MessageCollector collector, String state) {
        for (int i = 0; i < 20; i++) {
            final JsonNode node = data(collector.get(5, TimeUnit.SECONDS));
            if (state.equals(node.path("cardGameState").asText())) {
                return node;
            }
        }
        throw new AssertionError("상태 %s 브로드캐스트가 도착하지 않았다".formatted(state));
    }

    private JsonNode awaitCardOwned(TestStompSession.MessageCollector collector, int cardIndex) {
        for (int i = 0; i < 10; i++) {
            final JsonNode node = data(collector.get(5, TimeUnit.SECONDS));
            if (node.path("cardInfoMessages").get(cardIndex).path("selected").asBoolean()) {
                return node;
            }
        }
        throw new AssertionError("카드 %d 선택 반영 스냅샷이 도착하지 않았다".formatted(cardIndex));
    }

    private void assertDeckShape(SoftAssertions softly, JsonNode cards) {
        softly.assertThat(cards.size()).as("덱은 항상 9장").isEqualTo(9);
        int additions = 0;
        int multipliers = 0;
        for (final JsonNode card : cards) {
            final int value = card.path("value").asInt();
            if ("ADDITION".equals(card.path("cardType").asText())) {
                additions++;
                softly.assertThat(value % 5).as("덧셈 카드 값은 5 단위").isZero();
                softly.assertThat(Math.abs(value)).isLessThanOrEqualTo(40);
            } else {
                multipliers++;
                softly.assertThat(value).as("곱셈 카드 풀은 4/2/-1").isIn(4, 2, -1);
            }
            softly.assertThat(card.path("selected").asBoolean()).isFalse();
            softly.assertThat(card.path("playerName").isNull()).isTrue();
        }
        softly.assertThat(additions).as("덧셈 7장").isEqualTo(7);
        softly.assertThat(multipliers).as("곱셈 2장").isEqualTo(2);
    }
}
