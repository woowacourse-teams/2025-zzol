package coffeeshout.fixture;

import coffeeshout.global.MessageResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;

public class TestStompSession {

    private static final int DEFAULT_RESPONSE_TIMEOUT_SECONDS = 5;

    private final StompSession session;
    private final ObjectMapper objectMapper;

    protected TestStompSession(StompSession session, ObjectMapper objectMapper) {
        this.session = session;
        this.objectMapper = objectMapper;
    }

    public MessageCollector subscribe(String subscribeEndPoint) {
        MessageCollector messageCollector = new MessageCollector();
        session.subscribe(subscribeEndPoint, new MessageCollectorStompFrameHandler(messageCollector));
        return messageCollector;
    }

    public void send(String sendEndpoint, Object bodyMessage) {
        session.send(String.format(sendEndpoint), bodyMessage);
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

    public static class MessageCollector {
        private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        private void add(String message) {
            queue.add(message);
        }

        public MessageResponse get() {
            return get(DEFAULT_RESPONSE_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }

        public MessageResponse get(long timeout, TimeUnit unit) {
            try {
                long start = System.currentTimeMillis();
                String message = queue.poll(timeout, unit);
                if (message == null) {
                    throw new RuntimeException("메시지 수신 대기 시간을 초과했습니다");
                }
                long end = System.currentTimeMillis();
                return new MessageResponse(end - start, message);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        public int size() {
            return queue.size();
        }

        public boolean isEmpty() {
            return queue.isEmpty();
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
            synchronized (messageCollector) {
                try {
                    String jsonString = new String((byte[]) payload, StandardCharsets.UTF_8);
                    messageCollector.add(jsonString);
                } catch (Exception e) {
                    throw new RuntimeException("메시지 변환 실패: " + payload, e);
                }
            }
        }
    }
}
