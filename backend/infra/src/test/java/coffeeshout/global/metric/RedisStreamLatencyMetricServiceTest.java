package coffeeshout.global.metric;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.support.StubBaseEvent;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.distribution.CountAtBucket;
import io.micrometer.core.instrument.distribution.HistogramSnapshot;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RedisStreamLatencyMetricServiceTest {

    private static final String BROADCAST_STREAM = "room";
    private static final String OTHER_BROADCAST_STREAM = "wormgame";

    private MeterRegistry meterRegistry;
    private RedisStreamLatencyMetricService latencyMetricService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        latencyMetricService = new RedisStreamLatencyMetricService(meterRegistry);
    }

    @Nested
    @DisplayName("스트림별 기록")
    class 스트림별_기록 {

        @Test
        void 스트림별로_다른_타이머에_stream_태그와_함께_쌓인다() {
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
                softly.assertThat(timerOf(BROADCAST_STREAM).totalTime(TimeUnit.MILLISECONDS))
                        .isGreaterThan(0);
                softly.assertThat(timerOf(OTHER_BROADCAST_STREAM).count()).isEqualTo(1);
            });
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
            assertThat(timerOf(BROADCAST_STREAM)).isNull();
        }

        @Test
        void 음수_지연은_기록하지_않는다() {
            // given: 미래 시점의 timestamp (clock skew 시뮬레이션)
            // when
            latencyMetricService.recordLatency(
                    BROADCAST_STREAM, createEvent(Instant.now().plusSeconds(10)));

            // then
            assertThat(timerOf(BROADCAST_STREAM)).isNull();
        }
    }

    @Nested
    @DisplayName("히스토그램 구성")
    class 히스토그램_구성 {

        // 버킷 수가 곧 시계열 수인데 SimpleMeterRegistry는 버킷을 만들지 않는다. 실제 노출 대상인
        // Prometheus 레지스트리로 잰다.
        // 분위수는 알림이 버킷에서 계산한다. 인스턴스별 클라이언트 분위수는 blue/green을 가로질러
        // 합산되지 않아 쓸 수 없고, 스트림 수만큼 시계열만 늘린다.
        @Test
        void 버킷은_5초까지만_두고_클라이언트_분위수는_등록하지_않는다() {
            // given
            final PrometheusMeterRegistry prometheusRegistry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            final RedisStreamLatencyMetricService service = new RedisStreamLatencyMetricService(prometheusRegistry);
            service.recordLatency(BROADCAST_STREAM, createEvent(Instant.now().minusMillis(30)));

            // when
            final HistogramSnapshot snapshot = prometheusRegistry
                    .find("redis.stream.e2e.latency")
                    .tag("stream", BROADCAST_STREAM)
                    .timer()
                    .takeSnapshot();
            final CountAtBucket[] buckets = snapshot.histogramCounts();

            // then
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(buckets).isNotEmpty();
                softly.assertThat(buckets[buckets.length - 1].bucket(TimeUnit.SECONDS))
                        .isLessThanOrEqualTo(5.0);
                softly.assertThat(snapshot.percentileValues()).isEmpty();
            });
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
}
