package coffeeshout.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.socket.messaging.WebSocketStompClient;

public class TestStompSession implements AutoCloseable {

    private static final int DEFAULT_RESPONSE_TIMEOUT_SECONDS = 5;
    private static final String SUBSCRIBE_BARRIER_KEY = "__subscribeBarrier__";

    private final StompSession session;
    private final WebSocketStompClient stompClient;
    private final ObjectMapper objectMapper;
    private String principalName;

    public TestStompSession(StompSession session, WebSocketStompClient stompClient, ObjectMapper objectMapper) {
        this.session = session;
        this.stompClient = stompClient;
        this.objectMapper = objectMapper;
    }

    /**
     * 구독하고, 브로커 등록 완료까지 블록한 뒤 {@link MessageCollector}를 반환한다.
     *
     * <p>STOMP SUBSCRIBE는 비동기라 {@code session.subscribe()}는 등록 완료를 기다리지 않고 즉시 반환한다.
     * 구독 직후 동기적으로 브로드캐스트를 트리거하면(예: 게임 시작), 등록 전에 발행된 가장 이른
     * 브로드캐스트가 구독자 0명에게 전달되어 유실되고 이후 메시지가 한 칸씩 밀린다(subscribe→publish
     * 레이스, #1410). 이를 막기 위해 반환 전 {@link #awaitRegistered}로 등록 완료를 보장한다.
     */
    public MessageCollector subscribe(String subscribeEndPoint) {
        MessageCollector messageCollector = new MessageCollector();
        session.subscribe(subscribeEndPoint, new MessageCollectorStompFrameHandler(messageCollector));
        awaitRegistered(subscribeEndPoint, messageCollector);
        return messageCollector;
    }

    /**
     * 방금 구독한 {@code topic}의 브로커 등록 완료까지 블록한다.
     *
     * <p>인메모리 SimpleBroker는 SUBSCRIBE에 RECEIPT를 보내지 않으므로(#1410 실측) 구독 ACK로 확인할 수
     * 없다. 대신 <b>그 토픽 자체로</b> 고유 토큰을 담은 barrier ping을 컬렉터에 도착할 때까지 재전송한다.
     * SimpleBroker는 등록된 구독자에게만 전달하므로, ping이 이 컬렉터에 도착했다는 사실 자체가 해당 구독이
     * 등록됐다는 증거다(전달 ⟹ 등록). 이 논거는 프레임 처리 순서에 의존하지 않으므로, inbound 채널이
     * 멀티스레드(WebSocketMessageBrokerConfig: 32 threads)라 SUBSCRIBE·SEND 순서가 보장되지 않아도
     * 성립한다 — 고정 sleep과 달리 부하와 무관하게 결정론적이다.
     *
     * <p>barrier ping은 {@link MessageCollector}가 일반 메시지 큐에서 걸러내므로 테스트 단언을 오염시키지 않는다.
     */
    private void awaitRegistered(String topic, MessageCollector collector) {
        String token = UUID.randomUUID().toString();
        Map<String, String> ping = Map.of(SUBSCRIBE_BARRIER_KEY, token);
        Awaitility.await()
            .atMost(DEFAULT_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .pollDelay(Duration.ZERO)
            .pollInterval(Duration.ofMillis(50))
            .until(() -> {
                session.send(topic, ping);
                return collector.receivedBarrierPing(token);
            });
    }

    public void send(String sendEndpoint, Object bodyMessage) {
        session.send(sendEndpoint, bodyMessage);
    }

    public void send(String sendEndpoint, String jsonString) {
        try {
            Object jsonObject = objectMapper.readValue(jsonString, Object.class);
            session.send(sendEndpoint, jsonObject);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("JSON 파싱 실패: " + jsonString, e);
        }
    }

    public void send(String sendEndpoint) {
        session.send(sendEndpoint, null);
    }

    public boolean isConnected() {
        return session.isConnected();
    }

    public String getPrincipalName() {
        return principalName;
    }

    public void setPrincipalName(String principalName) {
        this.principalName = principalName;
    }

    public void disconnect() {
        session.disconnect();
        stompClient.stop();
    }

    @Override
    public void close() {
        disconnect();
    }

    public static class MessageCollector {
        private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        /**
         * [진단 계측 — #1410] 폴링되어 큐에서 빠져나간 메시지까지 포함해 이 컬렉터가 수신한
         * 모든 프레임을 도착 시각(첫 수신 기준 상대 ms)과 함께 보존한다. {@code get()} 타임아웃 시
         * 예외 메시지에 노출해, 세 번의 {@code get()}이 모두 끝난 뒤 단언하는 테스트 구조 때문에
         * 가려져 있던 "실제로 어떤 메시지가 도착/유실됐는가"를 CI 실패 출력에서 직접 확인하기 위함이다.
         * 진단 종료 후 제거 예정.
         */
        private final List<String> receivedHistory = new CopyOnWriteArrayList<>();
        private volatile long firstAddAt = -1L;

        /** subscribe() 등록 확인용 barrier ping. 일반 큐에서 걸러내 단언을 오염시키지 않고 등록 확인에만 쓴다(#1410). */
        private final Set<String> barrierPings = ConcurrentHashMap.newKeySet();

        private void add(String message) {
            if (message.contains(SUBSCRIBE_BARRIER_KEY)) {
                barrierPings.add(message);
                return;
            }
            long now = System.currentTimeMillis();
            if (firstAddAt < 0L) {
                firstAddAt = now;
            }
            receivedHistory.add("+" + (now - firstAddAt) + "ms " + message);
            queue.add(message);
        }

        private boolean receivedBarrierPing(String token) {
            return barrierPings.stream().anyMatch(ping -> ping.contains(token));
        }

        public MessageResponse get() {
            return get(DEFAULT_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        public MessageResponse get(long timeout, TimeUnit unit) {
            long start = System.currentTimeMillis();
            try {
                Awaitility.await()
                    .atMost(timeout, unit)
                    .until(() -> !queue.isEmpty());
            } catch (ConditionTimeoutException e) {
                // [진단 계측 — #1410] 예외 타입은 유지하고 메시지만 보강한다.
                // ConditionTimeoutException은 (String) 생성자만 제공하므로 initCause로 원본을 cause에 보존한다.
                ConditionTimeoutException enriched = new ConditionTimeoutException(String.format(
                    "메시지 미수신 (timeout=%d %s). 미폴링 큐 크기=%d, 누적 수신 이력(%d건)=%s",
                    timeout, unit.name(), queue.size(), receivedHistory.size(),
                    Collections.unmodifiableList(receivedHistory)));
                enriched.initCause(e);
                throw enriched;
            }
            long end = System.currentTimeMillis();
            return new MessageResponse(end - start, queue.poll());
        }

        public int size() {
            return queue.size();
        }

        public boolean isEmpty() {
            return queue.isEmpty();
        }

        public void assertNoMessage() {
            assertNoMessage(1, TimeUnit.SECONDS);
        }

        public void assertNoMessage(long timeout, TimeUnit unit) {
            Awaitility.await()
                .during(timeout, unit)
                .atMost(unit.toMillis(timeout) + 200, TimeUnit.MILLISECONDS)
                .until(queue::isEmpty);
        }
    }

    private static class MessageCollectorStompFrameHandler implements StompFrameHandler {
        private final MessageCollector messageCollector;

        public MessageCollectorStompFrameHandler(MessageCollector messageCollector) {
            this.messageCollector = messageCollector;
        }

        @Override
        public Type getPayloadType(StompHeaders headers) {
            return byte[].class;
        }

        @Override
        public void handleFrame(StompHeaders headers, Object payload) {
            String jsonString = new String((byte[]) payload, StandardCharsets.UTF_8);
            messageCollector.add(jsonString);
        }
    }
}
