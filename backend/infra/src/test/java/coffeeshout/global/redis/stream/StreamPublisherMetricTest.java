package coffeeshout.global.redis.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import coffeeshout.fixture.BaseEventDummy;
import coffeeshout.global.metric.RedisStreamPublishMetricService;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.CommonSettings;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.context.ContextSnapshotFactory;
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
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisStreamCommands.XAddOptions;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import reactor.core.publisher.Mono;

/**
 * 발행 카운터는 컨슈머 정지 알림(RedisStreamConsumptionStalled)의 좌변이다. 스트림 태그가 빠지거나
 * 증가가 누락되면 그 룰이 조용히 죽는다(#1744).
 */
@DisplayName("StreamPublisher 발행 메트릭")
class StreamPublisherMetricTest {

    private static final String STREAM_KEY = "room";
    private static final String OTHER_STREAM_KEY = "wormgame";

    @SuppressWarnings("unchecked")
    private final ReactiveStreamOperations<String, Object, Object> reactiveStreamOperations =
            mock(ReactiveStreamOperations.class);

    private MeterRegistry meterRegistry;
    private StreamPublisher streamPublisher;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        streamPublisher = publisherWith(mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS));
    }

    // 첫 발행 때 시계열이 생기면 Prometheus rate()가 그 증가를 못 세서, 발행이 한 번뿐인 스트림에서
    // 정지 룰이 침묵한다. 기동 시점 0 등록이 그걸 막는다.
    @Test
    void 발행_전에도_설정된_모든_스트림의_카운터가_0으로_등록된다() {
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(counterOf(STREAM_KEY)).isNotNull();
            softly.assertThat(counterOf(STREAM_KEY).count()).isZero();
            softly.assertThat(counterOf(OTHER_STREAM_KEY)).isNotNull();
        });
    }

    @Test
    void 발행하면_해당_스트림의_카운터만_증가한다() {
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

    // 카운터는 XADD 뒤에 올린다. 발행에 실패한 건을 세면 정지 룰의 좌변이 부풀어 오탐이 된다.
    @Test
    void 발행에_실패하면_카운터가_증가하지_않는다() {
        // given
        final StringRedisTemplate failingTemplate = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
        given(failingTemplate.opsForStream().add(any(MapRecord.class), any(XAddOptions.class)))
                .willThrow(new RedisConnectionFailureException("연결 실패"));
        final StreamPublisher publisher = publisherWith(failingTemplate);

        // when & then
        assertThatThrownBy(() -> publisher.publish(STREAM_KEY, "{}", null))
                .isInstanceOf(RedisConnectionFailureException.class);
        assertThat(counterOf(STREAM_KEY).count()).isZero();
    }

    @Test
    void 비동기_발행도_XADD가_끝나면_카운터가_증가한다() {
        // given
        given(reactiveStreamOperations.add(any(MapRecord.class), any(XAddOptions.class)))
                .willReturn(Mono.just(RecordId.of("1-0")));

        // when
        streamPublisher
                .publishAsync(() -> STREAM_KEY, BaseEventDummy.페이로드("비동기 발행"))
                .join();

        // then
        assertThat(counterOf(STREAM_KEY).count()).isEqualTo(1.0);
    }

    @Test
    void 비동기_발행이_실패하면_카운터가_증가하지_않는다() {
        // given
        given(reactiveStreamOperations.add(any(MapRecord.class), any(XAddOptions.class)))
                .willReturn(Mono.error(new RedisConnectionFailureException("연결 실패")));

        // when
        streamPublisher
                .publishAsync(() -> STREAM_KEY, BaseEventDummy.페이로드("비동기 발행"))
                .exceptionally(error -> null)
                .join();

        // then
        assertThat(counterOf(STREAM_KEY).count()).isZero();
    }

    private StreamPublisher publisherWith(StringRedisTemplate stringRedisTemplate) {
        final ReactiveStringRedisTemplate reactiveTemplate = mock(ReactiveStringRedisTemplate.class);
        doReturn(reactiveStreamOperations).when(reactiveTemplate).opsForStream();
        final RedisStreamPublishMetricService metricService = new RedisStreamPublishMetricService(meterRegistry);
        final StreamPublisher publisher = new StreamPublisher(
                stringRedisTemplate,
                reactiveTemplate,
                properties(),
                new ObjectMapper().findAndRegisterModules(),
                mock(StreamTracePropagator.class),
                meterRegistry,
                metricService,
                ContextSnapshotFactory.builder().build());
        publisher.initializeCounters();
        return publisher;
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
