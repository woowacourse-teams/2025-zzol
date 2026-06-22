package coffeeshout.zzolbot.monitor.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;

import coffeeshout.zzolbot.infra.ZzolBotOutboxRepository;
import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.domain.MonitorSignal;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import coffeeshout.zzolbot.monitor.infra.LokiLogClient;
import coffeeshout.zzolbot.monitor.infra.PrometheusMetricClient;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonitorCollectorTest {

    private static final MonitorProperties PROPERTIES =
            new MonitorProperties(true, "0 0 */4 * * *", 10, 512, 100, 300, 240, 50, 30);

    @Mock
    private ZzolBotOutboxRepository outboxRepository;
    @Mock
    private LokiLogClient lokiLogClient;
    @Mock
    private PrometheusMetricClient prometheusMetricClient;

    private MonitorCollector collector;

    @BeforeEach
    void setUp() {
        collector = new MonitorCollector(outboxRepository, lokiLogClient, prometheusMetricClient,
                PROPERTIES, Clock.fixed(Instant.parse("2026-06-21T00:00:00Z"), ZoneOffset.UTC));

        // 기본은 전부 정상(0). 각 테스트가 검증할 신호만 임계 초과로 덮어쓴다.
        given(outboxRepository.countByStatusIn(anyList())).willReturn(0L);
        given(lokiLogClient.countErrors(any(), any())).willReturn(0L);
        given(lokiLogClient.countWarns(any(), any())).willReturn(0L);
        given(prometheusMetricClient.count5xx(any(), any())).willReturn(0L);
        given(prometheusMetricClient.maxConsumerQueueSize(any())).willReturn(0L);
    }

    @Test
    void outbox_DEAD_LETTER가_임계를_넘으면_초과로_수집한다() {
        given(outboxRepository.countByStatusIn(anyList())).willReturn(15L);

        final MonitorSnapshot snapshot = collector.collect();

        final MonitorSignal deadLetter = signal(snapshot, "outbox_dead_letter");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(deadLetter.value()).isEqualTo(15);
            softly.assertThat(deadLetter.breached()).isTrue();
        });
    }

    @Test
    void 컨슈머_스레드풀_큐_깊이가_임계를_넘으면_초과로_수집한다() {
        given(prometheusMetricClient.maxConsumerQueueSize(any())).willReturn(600L);

        final MonitorSnapshot snapshot = collector.collect();

        final MonitorSignal queue = signal(snapshot, "redis_stream_consumer_queue");
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(queue.value()).isEqualTo(600);
            softly.assertThat(queue.breached()).isTrue();   // 600 > 512
        });
    }

    @Test
    void Loki_ERROR_로그_건수가_임계를_넘으면_초과로_수집한다() {
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
