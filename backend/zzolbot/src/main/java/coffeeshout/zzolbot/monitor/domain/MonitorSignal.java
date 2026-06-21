package coffeeshout.zzolbot.monitor.domain;

/**
 * 결정적으로 수집한 단일 운영 신호. 임계값 초과 여부({@code breached})를 함께 담는다.
 */
public record MonitorSignal(String name, long value, long threshold, boolean breached) {

    public static MonitorSignal of(String name, long value, long threshold) {
        return new MonitorSignal(name, value, threshold, value > threshold);
    }
}
