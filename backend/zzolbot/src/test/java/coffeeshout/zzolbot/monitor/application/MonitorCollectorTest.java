package coffeeshout.zzolbot.monitor.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;

import static org.mockito.ArgumentMatchers.any;

import coffeeshout.global.redis.config.RedisStreamProperties;
import coffeeshout.zzolbot.infra.ZzolBotOutboxRepository;
import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.domain.MonitorSignal;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import coffeeshout.zzolbot.monitor.infra.LokiLogClient;
import coffeeshout.zzolbot.monitor.infra.PrometheusMetricClient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonitorCollectorTest {

    private static final MonitorProperties PROPERTIES =
            new MonitorProperties(true, "0 0 */4 * * *", 10, 10000, 100, 300, 240, 50, 30);

    @Mock
    private ZzolBotOutboxRepository outboxRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    @SuppressWarnings("rawtypes")
    private StreamOperations streamOperations;
    @Mock
    private RedisStreamProperties redisStreamProperties;
    @Mock
    private LokiLogClient lokiLogClient;
    @Mock
    private PrometheusMetricClient prometheusMetricClient;

    private MonitorCollector collector;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        collector = new MonitorCollector(outboxRepository, redisTemplate, redisStreamProperties, lokiLogClient,
                prometheusMetricClient, PROPERTIES, Clock.fixed(Instant.parse("2026-06-21T00:00:00Z"), ZoneOffset.UTC));

        final Map<String, RedisStreamProperties.StreamConfig> keys = new LinkedHashMap<>();
        keys.put("game-stream", null);
        given(redisStreamProperties.keys()).willReturn(keys);
        given(redisTemplate.opsForStream()).willReturn(streamOperations);
        given(lokiLogClient.countErrors(any(), any())).willReturn(0L);
        given(lokiLogClient.countWarns(any(), any())).willReturn(0L);
        given(prometheusMetricClient.count5xx(any(), any())).willReturn(0L);
    }

    @Test
    void outbox_DEAD_LETTER가_임계를_넘으면_초과로_수집한다() {
        given(outboxRepository.countByStatusIn(anyList())).willReturn(15L);
        given(streamOperations.size(anyString())).willReturn(120L);

        final MonitorSnapshot snapshot = collector.collect();

        final MonitorSignal deadLetter = signal(snapshot, "outbox_dead_letter");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(deadLetter.value()).isEqualTo(15);
            softly.assertThat(deadLetter.breached()).isTrue();
        });
    }

    @Test
    void redis_stream_최대_적재량을_수집한다() {
        given(outboxRepository.countByStatusIn(anyList())).willReturn(0L);
        given(streamOperations.size(anyString())).willReturn(48213L);

        final MonitorSnapshot snapshot = collector.collect();

        final MonitorSignal backlog = signal(snapshot, "redis_stream_backlog");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(backlog.value()).isEqualTo(48213);
            softly.assertThat(backlog.breached()).isTrue();
        });
    }

    @Test
    void Loki_ERROR_로그_건수가_임계를_넘으면_초과로_수집한다() {
        given(outboxRepository.countByStatusIn(anyList())).willReturn(0L);
        given(streamOperations.size(anyString())).willReturn(120L);
        given(lokiLogClient.countErrors(any(), any())).willReturn(250L);

        final MonitorSnapshot snapshot = collector.collect();

        final MonitorSignal logs = signal(snapshot, "loki_error_logs");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(logs.value()).isEqualTo(250);
            softly.assertThat(logs.breached()).isTrue();
        });
    }

    @Test
    void WARN_로그는_ERROR와_별도_신호로_수집된다() {
        given(outboxRepository.countByStatusIn(anyList())).willReturn(0L);
        given(streamOperations.size(anyString())).willReturn(120L);
        given(lokiLogClient.countErrors(any(), any())).willReturn(5L);
        given(lokiLogClient.countWarns(any(), any())).willReturn(400L);

        final MonitorSnapshot snapshot = collector.collect();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(signal(snapshot, "loki_error_logs").breached()).isFalse();   // 5 < 100
            softly.assertThat(signal(snapshot, "loki_warn_logs").value()).isEqualTo(400);
            softly.assertThat(signal(snapshot, "loki_warn_logs").breached()).isTrue();     // 400 > 300
        });
    }

    @Test
    void HTTP_5xx_응답_수가_임계를_넘으면_초과로_수집한다() {
        given(outboxRepository.countByStatusIn(anyList())).willReturn(0L);
        given(streamOperations.size(anyString())).willReturn(120L);
        given(prometheusMetricClient.count5xx(any(), any())).willReturn(80L);

        final MonitorSnapshot snapshot = collector.collect();

        final MonitorSignal http5xx = signal(snapshot, "http_5xx");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(http5xx.value()).isEqualTo(80);
            softly.assertThat(http5xx.breached()).isTrue();
        });
    }

    private MonitorSignal signal(MonitorSnapshot snapshot, String name) {
        return snapshot.signals().stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
