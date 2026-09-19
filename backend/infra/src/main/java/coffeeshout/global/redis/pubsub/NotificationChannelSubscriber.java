package coffeeshout.global.redis.pubsub;

import coffeeshout.global.notify.NotificationSink;
import coffeeshout.global.redis.stream.StreamRecordFields;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * 알림 채널 구독자. 봉투를 풀어 traceparent 스코프 안에서 로컬 전달 지점({@link NotificationSink})에 넘긴다.
 * <p>
 * sink를 {@link ObjectProvider}로 받는다. 전달 구현은 {@code :websocket}에 있어 WS 계층이 없는
 * 컨텍스트(인프라 단독 테스트 등)에는 존재하지 않는데, 필수 주입이면 그런 컨텍스트가 기동 자체를 실패한다.
 * 의미상으로도 옳다 — 로컬로 전달할 곳이 없는 인스턴스는 구독할 이유가 없으므로 등록을 생략한다.
 */
@Slf4j
@Component
public class NotificationChannelSubscriber implements MessageListener {

    private static final String CONSUMER_SPAN_NAME = "notification.channel.receive";

    private final RedisMessageListenerContainer container;
    private final ObjectMapper objectMapper;
    private final StreamTracePropagator streamTracePropagator;
    private final NotificationChannelProperties properties;
    private final NotificationSink sink;

    public NotificationChannelSubscriber(
            RedisMessageListenerContainer container,
            ObjectProvider<NotificationSink> sinkProvider,
            @Qualifier("redisObjectMapper") ObjectMapper objectMapper,
            StreamTracePropagator streamTracePropagator,
            NotificationChannelProperties properties) {
        this.container = container;
        this.sink = sinkProvider.getIfAvailable();
        this.objectMapper = objectMapper;
        this.streamTracePropagator = streamTracePropagator;
        this.properties = properties;
    }

    @PostConstruct
    public void register() {
        if (sink == null) {
            log.info("알림 전달 지점(NotificationSink)이 없어 알림 채널 구독을 생략한다 — channel: {}", properties.channel());
            return;
        }
        container.addMessageListener(this, new ChannelTopic(properties.channel()));
        log.info("알림 채널 구독 등록 완료 — channel: {}", properties.channel());
    }

    @Override
    public void onMessage(@NonNull Message message, byte[] pattern) {
        try {
            final String body = new String(message.getBody(), StandardCharsets.UTF_8);
            final NotificationEnvelope envelope = objectMapper.readValue(body, NotificationEnvelope.class);
            streamTracePropagator.runInConsumerScope(carrierOf(envelope), CONSUMER_SPAN_NAME, () -> {
                if (!sink.deliver(envelope.destination(), envelope.payload())) {
                    log.warn(
                            "알림 로컬 전달 실패 — channel: {}, destination: {}", properties.channel(), envelope.destination());
                }
            });
        } catch (Exception e) {
            // 리스너 스레드를 보호한다 — 여기서 예외가 새면 이후 메시지 수신이 끊긴다(Profanity 구독자 선례).
            log.error("알림 채널 메시지 처리 실패 — channel: {}", properties.channel(), e);
        }
    }

    private Map<String, String> carrierOf(NotificationEnvelope envelope) {
        final String traceparent = envelope.traceparent();
        if (traceparent == null || traceparent.isBlank()) {
            return Map.of();
        }
        return Map.of(StreamRecordFields.TRACEPARENT, traceparent);
    }
}
