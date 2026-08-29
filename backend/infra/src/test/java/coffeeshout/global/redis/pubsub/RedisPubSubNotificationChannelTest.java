package coffeeshout.global.redis.pubsub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coffeeshout.global.redis.stream.StreamTracePropagator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("Redis pub/sub 알림 채널")
class RedisPubSubNotificationChannelTest {

    private static final String CHANNEL = "notification:ws:test";
    private static final String DESTINATION = "/topic/room/ABC123/gameState";
    private static final String TRACEPARENT = "00-0123456789abcdef0123456789abcdef-0123456789abcdef-01";

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private StreamTracePropagator streamTracePropagator;

    private ObjectMapper objectMapper;
    private RedisPubSubNotificationChannel channel;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        channel = new RedisPubSubNotificationChannel(
                stringRedisTemplate,
                objectMapper,
                streamTracePropagator,
                new NotificationChannelProperties(CHANNEL)
        );
    }

    private NotificationEnvelope 발행된_봉투를_읽는다() throws Exception {
        final ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(stringRedisTemplate).convertAndSend(eq(CHANNEL), bodyCaptor.capture());
        return objectMapper.readValue(bodyCaptor.getValue(), NotificationEnvelope.class);
    }

    @Test
    @DisplayName("설정된 채널명으로 봉투를 발행한다")
    void 설정된_채널명으로_봉투를_발행한다() throws Exception {
        // given
        given(streamTracePropagator.currentTraceparent()).willReturn(null);

        // when
        channel.publish(DESTINATION, new PayloadDummy("PLAYING"));

        // then
        final NotificationEnvelope envelope = 발행된_봉투를_읽는다();
        assertThat(envelope.destination()).isEqualTo(DESTINATION);
        assertThat(envelope.payload()).isEqualTo("{\"state\":\"PLAYING\"}");
    }

    @Test
    @DisplayName("활성 스팬이 있으면 봉투에 traceparent가 담긴다")
    void 활성_스팬이_있으면_traceparent가_담긴다() throws Exception {
        // given
        given(streamTracePropagator.currentTraceparent()).willReturn(TRACEPARENT);

        // when
        channel.publish(DESTINATION, new PayloadDummy("PLAYING"));

        // then
        assertThat(발행된_봉투를_읽는다().traceparent()).isEqualTo(TRACEPARENT);
    }

    @Test
    @DisplayName("활성 스팬이 없으면 traceparent가 null이다")
    void 활성_스팬이_없으면_traceparent가_null이다() throws Exception {
        // given
        given(streamTracePropagator.currentTraceparent()).willReturn(null);

        // when
        channel.publish(DESTINATION, new PayloadDummy("PLAYING"));

        // then
        assertThat(발행된_봉투를_읽는다().traceparent()).isNull();
    }

    private record PayloadDummy(String state) {
    }
}
