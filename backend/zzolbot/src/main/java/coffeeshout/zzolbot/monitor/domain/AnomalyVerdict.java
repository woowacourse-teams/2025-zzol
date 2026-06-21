package coffeeshout.zzolbot.monitor.domain;

/**
 * 임계값 게이팅 판정. fingerprint는 초과한 신호들의 식별자로, 동일 이상의 반복 알림을 쿨다운으로 억제하는 데 쓴다.
 */
public record AnomalyVerdict(boolean anomalous, Severity severity, String fingerprint) {

    public static AnomalyVerdict normal() {
        return new AnomalyVerdict(false, Severity.NORMAL, "");
    }
}
