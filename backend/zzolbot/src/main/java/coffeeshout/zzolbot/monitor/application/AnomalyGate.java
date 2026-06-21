package coffeeshout.zzolbot.monitor.application;

import coffeeshout.zzolbot.monitor.domain.AnomalyVerdict;
import coffeeshout.zzolbot.monitor.domain.MonitorSignal;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import coffeeshout.zzolbot.monitor.domain.Severity;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 수집 스냅샷을 임계값 기준으로 판정한다. LLM 호출은 이 게이트가 anomalous를 반환할 때만 일어난다(비용·소음 제어).
 * 임계값의 2배를 넘는 신호가 있으면 CRITICAL, 그 외 초과는 WARNING.
 */
@Component
public class AnomalyGate {

    public AnomalyVerdict evaluate(MonitorSnapshot snapshot) {
        final List<MonitorSignal> breached = snapshot.signals().stream()
                .filter(MonitorSignal::breached)
                .toList();
        if (breached.isEmpty()) {
            return AnomalyVerdict.normal();
        }

        final boolean critical = breached.stream().anyMatch(s -> s.value() >= s.threshold() * 2);
        final Severity severity = critical ? Severity.CRITICAL : Severity.WARNING;
        final String fingerprint = breached.stream()
                .map(MonitorSignal::name)
                .sorted()
                .collect(Collectors.joining(","));
        return new AnomalyVerdict(true, severity, fingerprint);
    }
}
