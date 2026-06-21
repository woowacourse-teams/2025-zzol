package coffeeshout.zzolbot.monitor.domain;

import java.time.Instant;
import java.util.List;

/**
 * 한 주기에 수집한 신호 모음.
 */
public record MonitorSnapshot(List<MonitorSignal> signals, Instant collectedAt) {

    public MonitorSnapshot {
        signals = List.copyOf(signals);
    }

    public boolean hasBreach() {
        return signals.stream().anyMatch(MonitorSignal::breached);
    }
}
