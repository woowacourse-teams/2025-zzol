package coffeeshout.support;

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
            T parsedData = objectMapper.readValue(jsonString, typeReference);
            queue.offer(parsedData);
        } catch (Exception e) {
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
