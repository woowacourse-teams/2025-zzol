package coffeeshout.global.config.redis;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SubscriberAutoConfiguration {

    private final RedisMessageListenerContainer container;
    private final List<EventSubscriber> subscribers;

    @PostConstruct
    public void registerAllSubscribers() {
        subscribers.forEach(subscriber -> {
            final EventTopicRegistry topicRegistry = subscriber.getTopicRegistry();
            final ChannelTopic topic = topicRegistry.toChannelTopic();
            container.addMessageListener(subscriber, topic);
            log.info("이벤트 구독자 자동 등록: {} -> {}",
                    subscriber.getClass().getSimpleName(),
                    topic.getTopic());
        });

        log.info("총 {} 개의 이벤트 구독자가 등록되었습니다.", subscribers.size());
    }
}
