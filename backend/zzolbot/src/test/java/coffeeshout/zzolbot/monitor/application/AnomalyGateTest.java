package coffeeshout.zzolbot.monitor.application;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.monitor.domain.AnomalyVerdict;
import coffeeshout.zzolbot.monitor.domain.MonitorSignal;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import coffeeshout.zzolbot.monitor.domain.Severity;
import java.time.Instant;
import java.util.List;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class AnomalyGateTest {

    private final AnomalyGate gate = new AnomalyGate();

    @Test
    void 임계값을_넘지_않으면_정상으로_판정한다() {
        final MonitorSnapshot snapshot = new MonitorSnapshot(
                List.of(MonitorSignal.of("outbox_dead_letter", 3, 10)), Instant.EPOCH);

        final AnomalyVerdict verdict = gate.evaluate(snapshot);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(verdict.anomalous()).isFalse();
            softly.assertThat(verdict.severity()).isEqualTo(Severity.NORMAL);
        });
    }

    @Test
    void 임계값을_넘으면_WARNING으로_판정하고_지문에_신호명을_담는다() {
        final MonitorSnapshot snapshot = new MonitorSnapshot(
                List.of(MonitorSignal.of("outbox_dead_letter", 15, 10)), Instant.EPOCH);

        final AnomalyVerdict verdict = gate.evaluate(snapshot);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(verdict.anomalous()).isTrue();
            softly.assertThat(verdict.severity()).isEqualTo(Severity.WARNING);
            softly.assertThat(verdict.fingerprint()).isEqualTo("outbox_dead_letter");
        });
    }

    @Test
    void 임계값의_2배_이상이면_CRITICAL로_판정한다() {
        final MonitorSnapshot snapshot = new MonitorSnapshot(
                List.of(MonitorSignal.of("redis_stream_backlog", 25000, 10000)), Instant.EPOCH);

        final AnomalyVerdict verdict = gate.evaluate(snapshot);

        assertThat(verdict.severity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void 정확히_임계값의_2배여도_CRITICAL_경계를_포함한다() {
        final MonitorSnapshot snapshot = new MonitorSnapshot(
                List.of(MonitorSignal.of("redis_stream_backlog", 20000, 10000)), Instant.EPOCH);

        assertThat(gate.evaluate(snapshot).severity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void 여러_신호가_초과하면_지문을_정렬해_결합한다() {
        final MonitorSnapshot snapshot = new MonitorSnapshot(List.of(
                MonitorSignal.of("redis_stream_backlog", 20000, 10000),
                MonitorSignal.of("outbox_dead_letter", 15, 10)), Instant.EPOCH);

        final AnomalyVerdict verdict = gate.evaluate(snapshot);

        assertThat(verdict.fingerprint()).isEqualTo("outbox_dead_letter,redis_stream_backlog");
    }
}
