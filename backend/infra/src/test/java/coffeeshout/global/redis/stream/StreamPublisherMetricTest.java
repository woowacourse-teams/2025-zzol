package coffeeshout.global.redis.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.CommonSettings;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 발행 카운터는 컨슈머 정지 알림(RedisStreamConsumptionStalled)의 좌변이다. 스트림 태그가 빠지거나
 * 증가가 누락되면 그 룰이 조용히 죽는다(#1744).
 */
@DisplayName("StreamPublisher 발행 메트릭")
class StreamPublisherMetricTest {

    private static final String STREAM_KEY = "room";
    private static final String OTHER_STREAM_KEY = "wormgame";

    private MeterRegistry meterRegistry;
    private StreamPublisher streamPublisher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        streamPublisher = new StreamPublisher(
                mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS),
                properties(),
                new ObjectMapper(),
                mock(StreamTracePropagator.class),
                meterRegistry);
    }

    @Test
    void 발행하면_stream_태그가_붙은_카운터가_증가한다() {
        // when
        streamPublisher.publish(STREAM_KEY, "{}", null);

        // then
        assertThat(counterOf(STREAM_KEY).count()).isEqualTo(1.0);
    }

    @Test
    void 스트림별로_카운터가_나뉜다() {
        // when
        streamPublisher.publish(STREAM_KEY, "{}", null);
        streamPublisher.publish(STREAM_KEY, "{}", null);
        streamPublisher.publish(OTHER_STREAM_KEY, "{}", null);

        // then
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(counterOf(STREAM_KEY).count()).isEqualTo(2.0);
            softly.assertThat(counterOf(OTHER_STREAM_KEY).count()).isEqualTo(1.0);
        });
    }

    @Test
    void 같은_스트림에_반복_발행해도_미터는_하나만_등록된다() {
        // when
        streamPublisher.publish(STREAM_KEY, "{}", null);
        streamPublisher.publish(STREAM_KEY, "{}", null);

        // then
        assertThat(meterRegistry.find("redis.stream.published").counters()).hasSize(1);
    }

    private Counter counterOf(String streamKey) {
        return meterRegistry
                .find("redis.stream.published")
                .tag("stream", streamKey)
                .counter();
    }

    private RedisStreamProperties properties() {
        final Map<String, StreamConfig> keys = new LinkedHashMap<>();
        keys.put(STREAM_KEY, new StreamConfig("concurrent", null, null, null, null, null));
        keys.put(OTHER_STREAM_KEY, new StreamConfig("concurrent", null, null, null, null, null));
        return new RedisStreamProperties(
                new CommonSettings(100, 10, Duration.ofSeconds(2), Duration.ofSeconds(5)), Map.of(), keys);
    }
}
