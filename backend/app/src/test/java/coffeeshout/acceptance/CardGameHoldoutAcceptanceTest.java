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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * 카드게임 인수 테스트 — 홀드아웃 스위트 (최소 계약 실험, 비공개).
 *
 * <p><b>샌드박스에 주입하지 않는다.</b> 오케스트레이터가 채점(Tier 1) 시 후보 브랜치 위에서 실행한다 —
 * 후보에게 노출되지 않으므로 "케이스를 스스로 만들어냈는가" 측정(재현율)을 오염시키지 않는다.
 *
 * <p>{@code @Disabled} 케이스는 현행(Redis Stream) 구현이 보장하지 않는 <b>신규 아키텍처 요구</b>다 —
 * 후보 채점 시 활성화한다(파일럿에서 실행 가능성 검증 후).
 */
@Tag("holdout")
class CardGameHoldoutAcceptanceTest extends WebSocketIntegrationTestSupport {

    private static final String HOST = "꾹이";
    private static final List<String> GUESTS = List.of("루키", "엠제이", "한스");

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
        await().atMost(Duration.ofSeconds(5)).until(() -> {
            try {
                roomRepository.findByJoinCode(new JoinCode(joinCode)).orElseThrow().validateStartable(HOST);
                return true;
            } catch (final Exception e) {
                return false;
            }
        });

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
    void 같은_카드를_동시에_선택하면_정확히_한_명만_가져간다() throws Exception {
        final var gameStates = subscribeGameState();
        startGame();
        awaitGameState(gameStates, "PLAYING");

        // 두 플레이어가 같은 인덱스를 최대한 동시에 전송
        final CountDownLatch ready = new CountDownLatch(1);
        final Thread first = new Thread(() -> {
            awaitLatch(ready);
            selectCard(HOST, 0);
        });
        final Thread second = new Thread(() -> {
            awaitLatch(ready);
            selectCard(GUESTS.get(0), 0);
        });
        first.start();
        second.start();
        ready.countDown();
        first.join(3000);
        second.join(3000);

        final JsonNode contested = awaitCardOwned(gameStates, 0);
        final String owner = contested.path("cardInfoMessages").get(0).path("playerName").asText();
        assertThat(owner).as("경합 카드의 소유자는 정확히 한 명").isIn(HOST, GUESTS.get(0));

        // 패자는 다른 카드를 정상적으로 선택할 수 있어야 한다
        final String loser = owner.equals(HOST) ? GUESTS.get(0) : HOST;
        selectCard(loser, 1);
        final JsonNode next = awaitCardOwned(gameStates, 1);

        final SoftAssertions softly = new SoftAssertions();
        softly.assertThat(next.path("cardInfoMessages").get(0).path("playerName").asText())
                .as("경합 승자의 소유권이 뒤집히면 안 된다")
                .isEqualTo(owner);
        softly.assertThat(next.path("cardInfoMessages").get(1).path("playerName").asText()).isEqualTo(loser);
        softly.assertAll();
    }

    @Test
    @Disabled("현행 구현 미보장 — R10·게임 규칙(라운드당 1장). 신규 구현 채점 시 활성화")
    void 같은_플레이어는_한_라운드에_한_장만_선택할_수_있다() throws Exception {
        final var gameStates = subscribeGameState();
        startGame();
        awaitGameState(gameStates, "PLAYING");

        selectCard(HOST, 0);
        awaitCardOwned(gameStates, 0);

        selectCard(HOST, 1);   // 같은 라운드 두 번째 선택 — 거부되어야 한다
        selectCard(GUESTS.get(0), 2);
        final JsonNode snapshot = awaitCardOwned(gameStates, 2);

        assertThat(snapshot.path("cardInfoMessages").get(1).path("selected").asBoolean())
                .as("한 라운드 두 번째 선택은 반영되면 안 된다")
                .isFalse();
    }

    @Test
    @Disabled("R12 신규 요구 — 재연결·재구독 스냅샷 재동기화. 신규 구현 채점 시 활성화")
    void 재구독한_클라이언트는_현재_상태_스냅샷을_받는다() throws Exception {
        final var gameStates = subscribeGameState();
        startGame();
        awaitGameState(gameStates, "PLAYING");

        // 새로 구독한 컬렉터가 다음 이벤트 없이도 현재 스냅샷을 받아야 한다
        final var resubscribed = sessions.get(GUESTS.get(1))
                .subscribe("/topic/room/%s/gameState".formatted(joinCode));
        final JsonNode snapshot = data(resubscribed.get(3, TimeUnit.SECONDS));
        assertThat(snapshot.path("cardGameState").asText()).isEqualTo("PLAYING");
    }

    @Test
    @Disabled("현행 SELECT_CARD는 본문 playerName을 신뢰(#1706 미적용) — R5 신규 요구. 신규 구현 채점 시 활성화")
    void 본문_이름을_위장한_선택은_다른_플레이어의_카드가_되지_않는다() throws Exception {
        final var gameStates = subscribeGameState();
        startGame();
        awaitGameState(gameStates, "PLAYING");

        // 루키 세션이 꾹이 이름으로 위장 전송 — 꾹이의 선택으로 기록되면 안 된다
        sessions.get(GUESTS.get(0)).send(commandUrl(),
                "{\"commandType\":\"SELECT_CARD\",\"commandRequest\":{\"playerName\":\"%s\",\"cardIndex\":0}}"
                        .formatted(HOST));
        selectCard(GUESTS.get(1), 1);
        final JsonNode snapshot = awaitCardOwned(gameStates, 1);

        assertThat(snapshot.path("cardInfoMessages").get(0).path("playerName").asText())
                .as("위장 선택이 꾹이 소유로 기록되면 안 된다")
                .isNotEqualTo(HOST);
    }

    @Test
    @Disabled("현행 Stream 컨슈머 경로는 거부를 통지하지 않음 — 신규 구현 요구(스펙 §4 에러). 채점 시 활성화")
    void 거부된_선택은_개인_에러큐로_통지된다() throws Exception {
        final var gameStates = subscribeGameState();
        final var errors = sessions.get(HOST).subscribe("/user/queue/errors");
        startGame();
        awaitGameState(gameStates, "DONE");

        selectCard(HOST, 0);   // DONE 이후 선택 — 거부 통지가 개인 큐로 와야 한다
        final MessageResponse error = errors.get(3, TimeUnit.SECONDS);
        assertThat(objectMapper.readTree(error.payload()).path("success").asBoolean()).isFalse();
    }

    private TestStompSession.MessageCollector subscribeGameState() {
        return sessions.get(HOST).subscribe("/topic/room/%s/gameState".formatted(joinCode));
    }

    private void startGame() {
        sessions.get(HOST).send(commandUrl(),
                "{\"commandType\":\"START_MINI_GAME\",\"commandRequest\":{\"hostName\":\"%s\"}}".formatted(HOST));
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
        return objectMapper.readTree(response.payload()).path("data");
    }

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

    private static void awaitLatch(CountDownLatch latch) {
        try {
            latch.await();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
