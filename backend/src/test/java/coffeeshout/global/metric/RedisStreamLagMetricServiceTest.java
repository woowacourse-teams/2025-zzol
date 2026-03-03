package coffeeshout.global.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.CommonSettings;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

class RedisStreamLagMetricServiceTest {

    private MeterRegistry meterRegistry;
    private StringRedisTemplate stringRedisTemplate;
    private RedisStreamProperties redisStreamProperties;
    private ApplicationContext applicationContext;
    private RedisStreamLagMetricService lagMetricService;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        stringRedisTemplate = mock(StringRedisTemplate.class);
        applicationContext = mock(ApplicationContext.class);

        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        given(stringRedisTemplate.opsForStream()).willReturn(streamOps);
        given(streamOps.size("room")).willReturn(42L);
        given(streamOps.size("racinggame")).willReturn(7L);

        CommonSettings commonSettings = new CommonSettings(100, 10, Duration.ofSeconds(2));
        StreamConfig roomConfig = new StreamConfig("concurrent", null, null, null, null);
        StreamConfig racingConfig = new StreamConfig("concurrent", null, null, null, null);

        redisStreamProperties = new RedisStreamProperties(
                commonSettings,
                null,
                Map.of("room", roomConfig, "racinggame", racingConfig)
        );

        lagMetricService = new RedisStreamLagMetricService(
                stringRedisTemplate, redisStreamProperties, meterRegistry, applicationContext
        );
    }

    @Test
    void 스트림별_길이_게이지가_등록된다() {
        // when
        lagMetricService.initializeMetrics();

        // then
        Gauge roomGauge = meterRegistry.find("redis.stream.length")
                .tag("stream", "room")
                .gauge();
        assertThat(roomGauge).isNotNull();
        assertThat(roomGauge.value()).isEqualTo(42.0);
    }

    @Test
    void 여러_스트림의_게이지가_각각_등록된다() {
        // when
        lagMetricService.initializeMetrics();

        // then
        Gauge roomGauge = meterRegistry.find("redis.stream.length")
                .tag("stream", "room")
                .gauge();
        Gauge racingGauge = meterRegistry.find("redis.stream.length")
                .tag("stream", "racinggame")
                .gauge();

        assertThat(roomGauge).isNotNull();
        assertThat(racingGauge).isNotNull();
        assertThat(roomGauge.value()).isEqualTo(42.0);
        assertThat(racingGauge.value()).isEqualTo(7.0);
    }

    @Test
    void 스레드풀_큐_깊이_게이지가_등록된다() {
        // when
        lagMetricService.initializeMetrics();

        // then: 빈을 못 찾으면 -1.0 반환
        Gauge queueGauge = meterRegistry.find("redis.stream.threadpool.queue.size")
                .tag("stream", "room")
                .gauge();
        assertThat(queueGauge).isNotNull();
        assertThat(queueGauge.value()).isEqualTo(-1.0);
    }

    @Test
    void 스트림_설정이_없으면_게이지를_등록하지_않는다() {
        // given
        RedisStreamProperties emptyProperties = new RedisStreamProperties(null, null, null);
        RedisStreamLagMetricService emptyService = new RedisStreamLagMetricService(
                stringRedisTemplate, emptyProperties, meterRegistry, applicationContext
        );

        // when
        emptyService.initializeMetrics();

        // then
        Gauge gauge = meterRegistry.find("redis.stream.length").gauge();
        assertThat(gauge).isNull();
    }

    @SuppressWarnings("unchecked")
    @Test
    void XLEN_조회_실패_시_음수를_반환한다() {
        // given
        StreamOperations<String, Object, Object> failingOps = mock(StreamOperations.class);
        given(stringRedisTemplate.opsForStream()).willReturn(failingOps);
        given(failingOps.size("room")).willThrow(new RuntimeException("Redis 연결 실패"));

        lagMetricService.initializeMetrics();

        // when
        Gauge gauge = meterRegistry.find("redis.stream.length")
                .tag("stream", "room")
                .gauge();

        // then
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(-1.0);
    }
}
