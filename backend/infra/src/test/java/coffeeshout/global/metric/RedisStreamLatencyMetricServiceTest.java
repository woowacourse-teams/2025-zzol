package coffeeshout.global.metric;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.global.redis.config.RedisStreamProperties.CommonSettings;
import coffeeshout.global.redis.config.RedisStreamProperties.StreamConfig;
import coffeeshout.support.StubBaseEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RedisStreamLatencyMetricServiceTest {

    private static final String BROADCAST_STREAM = "room";
    private static final String OTHER_BROADCAST_STREAM = "wormgame";
    private static final String WORK_QUEUE_STREAM = "settlement:result";

    private MeterRegistry meterRegistry;
    private RedisStreamLatencyMetricService latencyMetricService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        latencyMetricService = new RedisStreamLatencyMetricService(meterRegistry, properties());
        latencyMetricService.initializeMetrics();
    }

    @Nested
    @DisplayName("스트림별 기록")
    class 스트림별_기록 {

        @Test
        void 지연_시간이_stream_태그와_함께_기록된다() {
            // when
            latencyMetricService.recordLatency(
                    BROADCAST_STREAM, createEvent(Instant.now().minusMillis(30)));

            // then
            final Timer timer = timerOf(BROADCAST_STREAM);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(timer).isNotNull();
                softly.assertThat(timer.count()).isEqualTo(1);
                softly.assertThat(timer.totalTime(TimeUnit.MILLISECONDS)).isGreaterThan(0);
            });
        }

        @Test
        void 스트림이_다르면_다른_타이머에_쌓인다() {
            // when
            latencyMetricService.recordLatency(
                    BROADCAST_STREAM, createEvent(Instant.now().minusMillis(10)));
            latencyMetricService.recordLatency(
                    BROADCAST_STREAM, createEvent(Instant.now().minusMillis(20)));
            latencyMetricService.recordLatency(
                    OTHER_BROADCAST_STREAM, createEvent(Instant.now().minusMillis(30)));

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(timerOf(BROADCAST_STREAM).count()).isEqualTo(2);
                softly.assertThat(timerOf(OTHER_BROADCAST_STREAM).count()).isEqualTo(1);
            });
        }

        @Test
        void 설정에_없는_스트림도_기록_시점에_타이머를_만든다() {
            // when
            latencyMetricService.recordLatency("동적키", createEvent(Instant.now().minusMillis(10)));

            // then
            assertThat(timerOf("동적키").count()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("기동 시점 사전 등록")
    class 사전_등록 {

        // 이 등록 범위가 RedisStreamConsumptionStalled 룰의 판정 근거다. 소비 경로가 없는 스트림에
        // 시계열이 생기면 "발행은 되는데 소비가 0"으로 읽혀 영구 발화한다(#1744).
        @Test
        void 브로드캐스트_스트림은_소비_전에도_0으로_노출된다() {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(timerOf(BROADCAST_STREAM)).isNotNull();
                softly.assertThat(timerOf(BROADCAST_STREAM).count()).isZero();
                softly.assertThat(timerOf(OTHER_BROADCAST_STREAM)).isNotNull();
            });
        }

        @Test
        void listener가_없는_작업_큐_스트림은_등록하지_않는다() {
            assertThat(timerOf(WORK_QUEUE_STREAM)).isNull();
        }
    }

    @Nested
    @DisplayName("기록하지 않는 경우")
    class 기록하지_않는_경우 {

        @Test
        void timestamp가_null이면_기록하지_않는다() {
            // when
            latencyMetricService.recordLatency(BROADCAST_STREAM, createEvent(null));

            // then
            assertThat(timerOf(BROADCAST_STREAM).count()).isZero();
        }

        @Test
        void 음수_지연은_기록하지_않는다() {
            // given: 미래 시점의 timestamp (clock skew 시뮬레이션)
            // when
            latencyMetricService.recordLatency(
                    BROADCAST_STREAM, createEvent(Instant.now().plusSeconds(10)));

            // then
            assertThat(timerOf(BROADCAST_STREAM).count()).isZero();
        }
    }

    @Nested
    @DisplayName("히스토그램 구성")
    class 히스토그램_구성 {

        // SimpleMeterRegistry는 percentile histogram 버킷을 만들지 않는다. 버킷 수가 곧 시계열 수라
        // 실제 노출 대상인 Prometheus 레지스트리로 잰다.
        @Test
        void 버킷_상한이_5초로_제한된다() {
            // given
            final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            final RedisStreamLatencyMetricService service =
                    new RedisStreamLatencyMetricService(prometheusRegistry, properties());
            service.initializeMetrics();
            service.recordLatency(BROADCAST_STREAM, createEvent(Instant.now().minusMillis(30)));

            // when
            final CountAtBucket[] buckets = prometheusRegistry
                    .find("redis.stream.e2e.latency")
                    .tag("stream", BROADCAST_STREAM)
                    .timer()
                    .takeSnapshot()
                    .histogramCounts();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(buckets).isNotEmpty();
                softly.assertThat(buckets[buckets.length - 1].bucket(TimeUnit.SECONDS))
                        .isLessThanOrEqualTo(5.0);
            });
        }

        // 알림은 버킷에서 분위수를 계산한다. 인스턴스별 클라이언트 분위수는 blue/green을 가로질러
        // 합산되지 않아 쓸 수 없고, 스트림 수만큼 시계열만 늘린다.
        @Test
        void 클라이언트_측_분위수는_등록하지_않는다() {
            // given
            latencyMetricService.recordLatency(
                    BROADCAST_STREAM, createEvent(Instant.now().minusMillis(30)));

            // when & then
            assertThat(timerOf(BROADCAST_STREAM).takeSnapshot().percentileValues())
                    .isEmpty();
        }
    }

    private Timer timerOf(String streamKey) {
        return meterRegistry
                .find("redis.stream.e2e.latency")
                .tag("stream", streamKey)
                .timer();
    }

    private BaseEvent createEvent(Instant timestamp) {
        return new StubBaseEvent("test-event-id", timestamp);
    }

    private RedisStreamProperties properties() {
        final Map<String, StreamConfig> keys = new LinkedHashMap<>();
        keys.put(BROADCAST_STREAM, broadcastStream());
        keys.put(OTHER_BROADCAST_STREAM, broadcastStream());
        keys.put(WORK_QUEUE_STREAM, new StreamConfig(null, null, 10000, null, null, false));
        return new RedisStreamProperties(
                new CommonSettings(100, 10, Duration.ofSeconds(2), Duration.ofSeconds(5)), Map.of(), keys);
    }

    private StreamConfig broadcastStream() {
        return new StreamConfig("concurrent", null, null, null, null, null);
    }
}
