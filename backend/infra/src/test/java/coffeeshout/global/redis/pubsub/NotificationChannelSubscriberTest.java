package coffeeshout.global.redis.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.global.notify.NotificationSink;
import coffeeshout.global.redis.stream.StreamRecordFields;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@ExtendWith(MockitoExtension.class)
@DisplayName("알림 채널 구독자")
class NotificationChannelSubscriberTest {

    private static final String CHANNEL = "notification:ws:test";
    private static final String DESTINATION = "/topic/room/ABC123/gameState";
    private static final String PAYLOAD_JSON = "{\"success\":true,\"data\":{\"state\":\"PLAYING\"}}";
    private static final String TRACEPARENT = "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01";

    @Mock
    private RedisMessageListenerContainer container;

    @Mock
    private ObjectProvider<NotificationSink> sinkProvider;

    @Mock
    private StreamTracePropagator streamTracePropagator;

    private ObjectMapper objectMapper;
    private NotificationSinkFake sinkFake;
    private NotificationChannelSubscriber subscriber;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        sinkFake = new NotificationSinkFake();
    }

    // sink는 생성자에서 해결되므로 스텁을 생성 전에 건다
    private NotificationChannelSubscriber 구독자를_생성한다(NotificationSink sink) {
        given(sinkProvider.getIfAvailable()).willReturn(sink);
        return new NotificationChannelSubscriber(
                container,
                sinkProvider,
                objectMapper,
                streamTracePropagator,
                new NotificationChannelProperties(CHANNEL));
    }

    private void sink이_있는_상태로_등록한다() {
        subscriber = 구독자를_생성한다(sinkFake);
        subscriber.register();
    }

    private void consumer_스코프가_task를_실행하도록_설정한다(AtomicReference<Map<String, String>> 캐리어) {
        willAnswer(invocation -> {
                    캐리어.set(invocation.getArgument(0));
                    invocation.getArgument(2, Runnable.class).run();
                    return null;
                })
                .given(streamTracePropagator)
                .runInConsumerScope(any(), any(), any());
    }

    private Message 봉투_메시지(NotificationEnvelope envelope) throws Exception {
        return new DefaultMessage(
                CHANNEL.getBytes(StandardCharsets.UTF_8),
                objectMapper.writeValueAsString(envelope).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("봉투를 수신하면 sink가 destination과 payload를 그대로 받는다")
    void 봉투를_수신하면_sink가_원본_인자를_받는다() throws Exception {
        // given
        sink이_있는_상태로_등록한다();
        consumer_스코프가_task를_실행하도록_설정한다(new AtomicReference<>());

        // when
        subscriber.onMessage(봉투_메시지(new NotificationEnvelope(DESTINATION, PAYLOAD_JSON, null)), null);

        // then
        assertThat(sinkFake.deliveries()).singleElement().satisfies(delivered -> {
            assertThat(delivered.destination()).isEqualTo(DESTINATION);
            assertThat(delivered.payloadJson()).isEqualTo(PAYLOAD_JSON);
        });
    }

    @Test
    @DisplayName("봉투에 traceparent가 있으면 캐리어에 담아 consumer 스코프를 연다")
    void traceparent가_있으면_캐리어에_담긴다() throws Exception {
        // given
        sink이_있는_상태로_등록한다();
        final AtomicReference<Map<String, String>> 캐리어 = new AtomicReference<>();
        consumer_스코프가_task를_실행하도록_설정한다(캐리어);

        // when
        subscriber.onMessage(봉투_메시지(new NotificationEnvelope(DESTINATION, PAYLOAD_JSON, TRACEPARENT)), null);

        // then
        assertThat(캐리어.get()).containsEntry(StreamRecordFields.TRACEPARENT, TRACEPARENT);
    }

    @Test
    @DisplayName("봉투에 traceparent가 없으면 빈 캐리어로 실행한다")
    void traceparent가_없으면_빈_캐리어로_실행한다() throws Exception {
        // given
        sink이_있는_상태로_등록한다();
        final AtomicReference<Map<String, String>> 캐리어 = new AtomicReference<>();
        consumer_스코프가_task를_실행하도록_설정한다(캐리어);

        // when
        subscriber.onMessage(봉투_메시지(new NotificationEnvelope(DESTINATION, PAYLOAD_JSON, null)), null);

        // then
        assertThat(캐리어.get()).isEmpty();
    }

    @Test
    @DisplayName("sink가 없으면 구독을 등록하지 않는다")
    void sink가_없으면_구독을_등록하지_않는다() {
        // given
        subscriber = 구독자를_생성한다(null);

        // when
        subscriber.register();

        // then
        verify(container, never()).addMessageListener(any(), any(ChannelTopic.class));
    }

    @Test
    @DisplayName("sink가 있으면 설정된 채널을 구독한다")
    void sink가_있으면_설정된_채널을_구독한다() {
        // given & when
        sink이_있는_상태로_등록한다();

        // then
        verify(container).addMessageListener(subscriber, new ChannelTopic(CHANNEL));
    }

    @Test
    @DisplayName("본문이 봉투가 아니어도 예외가 리스너 스레드로 새지 않는다")
    void 본문이_봉투가_아니어도_예외가_새지_않는다() {
        // given
        sink이_있는_상태로_등록한다();
        final Message 깨진_메시지 = new DefaultMessage(
                CHANNEL.getBytes(StandardCharsets.UTF_8), "not-json".getBytes(StandardCharsets.UTF_8));

        // when & then
        assertThatCode(() -> subscriber.onMessage(깨진_메시지, null)).doesNotThrowAnyException();
        assertThat(sinkFake.deliveries()).isEmpty();
    }
}
