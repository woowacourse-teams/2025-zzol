package coffeeshout.global.redis.pubsub;

import coffeeshout.global.notify.GameNotificationChannel;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 알림 채널의 Redis pub/sub 어댑터.
 * <p>
 * 페이로드를 JSON으로 굳혀 봉투에 담고, 현재 트레이스 컨텍스트를 traceparent로 함께 실어 발행한다
 * ({@code StreamPublisher}가 Stream 경계에서 하는 것과 같은 일 — ADR-0021의 경계 패턴).
 * <p>
 * 값 직렬화기가 붙은 {@code RedisTemplate<String, Object>}이 아니라 {@link StringRedisTemplate}을 쓴다.
 * 전자의 value serializer는 JSON이라 이미 JSON 문자열인 봉투를 한 번 더 인코딩해
 * {@code "\"{...}\""} 꼴로 이중 이스케이프한다. {@code StreamPublisher}·{@code WsRecoveryService}도
 * 같은 이유로 {@link StringRedisTemplate}을 쓴다.
 */
@Slf4j
@Component
public class RedisPubSubNotificationChannel implements GameNotificationChannel {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final StreamTracePropagator streamTracePropagator;
    private final NotificationChannelProperties properties;

    public RedisPubSubNotificationChannel(
            StringRedisTemplate stringRedisTemplate,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            StreamTracePropagator streamTracePropagator,
            NotificationChannelProperties properties
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
        this.streamTracePropagator = streamTracePropagator;
        this.properties = properties;
    }

    @Override
    public void publish(String destination, Object payload) {
        final String channel = properties.channel();
        try {
            final NotificationEnvelope envelope = new NotificationEnvelope(
                    destination,
                    objectMapper.writeValueAsString(payload),
                    streamTracePropagator.currentTraceparent()
            );
            stringRedisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(envelope));
            log.debug("알림 채널 발행 — channel: {}, destination: {}", channel, destination);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("알림 페이로드 직렬화 실패: destination=" + destination, e);
        }
    }
}
