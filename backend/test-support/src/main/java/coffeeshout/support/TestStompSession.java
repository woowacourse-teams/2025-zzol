package coffeeshout.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
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
    private static final Map<String, String> SUBSCRIBE_BARRIER_PING = Map.of("ping", "1");

    private final StompSession session;
    private final WebSocketStompClient stompClient;
    private final ObjectMapper objectMapper;
    private String principalName;

    public TestStompSession(StompSession session, WebSocketStompClient stompClient, ObjectMapper objectMapper) {
        this.session = session;
        this.stompClient = stompClient;
        this.objectMapper = objectMapper;
    }

    public MessageCollector subscribe(String subscribeEndPoint) {
        MessageCollector messageCollector = new MessageCollector();
        session.subscribe(subscribeEndPoint, new MessageCollectorStompFrameHandler(messageCollector));
        return messageCollector;
    }

    /**
     * 이 세션이 직전까지 보낸 모든 SUBSCRIBE의 브로커 등록 완료를 결정론적으로 보장한다.
     *
     * <p>인메모리 SimpleBroker는 SUBSCRIBE에 RECEIPT를 보내지 않으므로(#1410 실측) 구독 ACK로 확인할 수
     * 없다. 대신 센티넬 토픽을 구독한 뒤 그 토픽으로 ping을 round-trip될 때까지 재전송한다. ping이
     * 돌아오면 센티넬 SUBSCRIBE가 등록된 것이고, 한 세션의 inbound 채널은 프레임을 순서대로 처리하므로
     * 그 이전에 보낸 SUBSCRIBE들도 모두 등록 완료다.
     *
     * <p>테스트는 게임 시작(첫 브로드캐스트 발행) 직전에 호출해, 구독 등록 전 발행된 가장 이른
     * 브로드캐스트가 유실되는 subscribe→publish 레이스를 제거한다. 고정 sleep과 달리 등록 완료를
     * 실제로 증명하므로 부하와 무관하게 결정론적이다.
     */
    public void awaitSubscribed() {
        awaitSubscribed(DEFAULT_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
    }

    public void awaitSubscribed(long timeout, TimeUnit unit) {
        String sentinelTopic = "/topic/__subscribe-barrier__/" + UUID.randomUUID();
        MessageCollector sentinel = subscribe(sentinelTopic);
        Awaitility.await()
            .atMost(timeout, unit)
            .pollDelay(Duration.ZERO)
            .pollInterval(Duration.ofMillis(50))
            .until(() -> {
                session.send(sentinelTopic, SUBSCRIBE_BARRIER_PING);
                return !sentinel.isEmpty();
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

        private void add(String message) {
            long now = System.currentTimeMillis();
            if (firstAddAt < 0L) {
                firstAddAt = now;
            }
            receivedHistory.add("+" + (now - firstAddAt) + "ms " + message);
            queue.add(message);
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
