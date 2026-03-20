package coffeeshout.fixture;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;

public class TypeReferenceFrameHandler<T> implements StompFrameHandler {

    private final BlockingQueue<T> queue;
    private final TypeReference<T> typeReference;
    private final ObjectMapper objectMapper;

    // 🎯 생성자에서 TypeReference를 직접 받음
    public TypeReferenceFrameHandler(BlockingQueue<T> queue, TypeReference<T> typeReference,
                                     ObjectMapper objectMapper) {
        this.queue = queue;
        this.typeReference = typeReference;
        this.objectMapper = objectMapper;
    }

    @Override
    public Type getPayloadType(StompHeaders headers) {
        return Object.class;
    }

    @Override
    public void handleFrame(StompHeaders headers, Object payload) {
        try {
            String jsonString = extractJsonString(payload);
            System.out.println("🎯 수신된  JSON: " + jsonString);

            // TypeReference 사용해서 파싱
            T parsedData = objectMapper.readValue(jsonString, typeReference);
            queue.offer(parsedData);

            System.out.println("✨파싱 완료");

        } catch (Exception e) {
            System.err.println("❌  파싱 실패: " + e.getMessage());
            e.printStackTrace();
            queue.offer(null);
        }
    }

    private String extractJsonString(Object payload) {
        if (payload instanceof byte[]) {
            return new String((byte[]) payload, StandardCharsets.UTF_8);
        }
        return payload.toString();
    }
}