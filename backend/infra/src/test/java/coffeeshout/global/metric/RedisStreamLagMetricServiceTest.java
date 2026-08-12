package coffeeshout.global.metric;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

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

        CommonSettings commonSettings = new CommonSettings(100, 10, Duration.ofSeconds(2), Duration.ofSeconds(5));
        StreamConfig roomConfig = new StreamConfig("concurrent", null, null, null, null, null);
        StreamConfig racingConfig = new StreamConfig("concurrent", null, null, null, null, null);

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

        // then: 빈을 못 찾으면 Nan 반환
        Gauge queueGauge = meterRegistry.find("redis.stream.threadpool.queue.size")
                .tag("stream", "room")
                .gauge();
        assertThat(queueGauge).isNotNull();
        assertThat(queueGauge.value()).isNaN();
    }

    @SuppressWarnings("unchecked")
    @Test
    void 리스너_미생성_스트림은_길이_게이지만_등록하고_스레드풀_게이지는_등록하지_않는다() {
        // given: 컨슈머 그룹 전용 스트림 — 소비 스레드풀이 없다(#1610)
        StreamOperations<String, Object, Object> streamOps = mock(StreamOperations.class);
        given(stringRedisTemplate.opsForStream()).willReturn(streamOps);
        given(streamOps.size("settlement:result")).willReturn(3L);

        StreamConfig workQueueConfig = new StreamConfig(null, null, 10000, null, null, false);
        RedisStreamProperties workQueueProperties = new RedisStreamProperties(
                new CommonSettings(100, 10, Duration.ofSeconds(2), Duration.ofSeconds(5)),
                null,
                Map.of("settlement:result", workQueueConfig)
        );
        RedisStreamLagMetricService service = new RedisStreamLagMetricService(
                stringRedisTemplate, workQueueProperties, meterRegistry, applicationContext
        );

        // when
        service.initializeMetrics();

        // then
        Gauge lengthGauge = meterRegistry.find("redis.stream.length")
                .tag("stream", "settlement:result")
                .gauge();
        Gauge queueGauge = meterRegistry.find("redis.stream.threadpool.queue.size")
                .tag("stream", "settlement:result")
                .gauge();
        assertThat(lengthGauge).isNotNull();
        assertThat(lengthGauge.value()).isEqualTo(3.0);
        assertThat(queueGauge).isNull();
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

    @Test
    void 종료_신호를_받으면_게이지가_Redis를_조회하지_않고_Nan를_반환한다() {
        // given: 종료 전에는 정상 조회된다
        lagMetricService.initializeMetrics();
        Gauge gauge = meterRegistry.find("redis.stream.length")
                .tag("stream", "room")
                .gauge();
        assertThat(gauge).isNotNull();
        assertThat(gauge.value()).isEqualTo(42.0);

        // when: 종료 신호 (컨텍스트 pause 또는 close 의 lifecycle stop 단계)
        clearInvocations(stringRedisTemplate);
        lagMetricService.stop();

        // then: 이후 게이지가 평가돼도 이미 닫힌 커넥션을 건드리지 않는다 (#1642)
        assertThat(gauge.value()).isNaN();
        assertThat(lagMetricService.isRunning()).isFalse();
        then(stringRedisTemplate).should(never()).opsForStream();
    }

    @Test
    void 게이지는_폴러_정지_뒤_커넥션_팩토리_정지_전에_멈춘다() {
        // 종료는 phase 내림차순이다. RedisStreamContainerRegistry(1024)가 폴러를 멈춘 뒤에도
        // 백로그는 관측 대상이고, LettuceConnectionFactory(0)가 멈춘 뒤에는 조회가 불가능하다
        assertThat(lagMetricService.getPhase())
                .isGreaterThan(0)
                .isLessThan(1024);
    }

    @SuppressWarnings("unchecked")
    @Test
    void XLEN_조회_실패_시_Nan를_반환한다() {
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
        assertThat(gauge.value()).isNaN();
    }
}
