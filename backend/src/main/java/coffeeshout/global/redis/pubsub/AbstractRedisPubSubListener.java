package coffeeshout.global.redis.pubsub;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.EventHandlerExecutor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AbstractRedisPubSubListener implements MessageListener {

    private final ObjectMapper objectMapper;
    private final RedisMessageListenerContainer redisMessageListenerContainer;
    private final EventHandlerExecutor eventHandlerExecutor;

    @PostConstruct
    public void subscribe() {
        Arrays.stream(PubSubChannelTopic.values()).forEach(channelTopic ->
                redisMessageListenerContainer.addMessageListener(this, channelTopic.channelTopic()));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        eventHandlerExecutor.handle(convertEvent(message));
    }

    private BaseEvent convertEvent(Message message) {
        try {
            return objectMapper.readValue(new String(message.getBody()), BaseEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
