package coffeeshout.zzolbot.monitor.application;

import java.util.Map;

/**
 * Alertmanager가 발화한 단일 firing 알림의 도메인 뷰. 웹훅 DTO를 보강 포트로 넘기기 위한 경계 객체로,
 * 보강기가 web 계층 DTO에 의존하지 않게 한다(ADR-0032).
 */
public record FiringAlert(
        String alertname,
        String severity,
        String fingerprint,
        String summary,
        String description,
        Map<String, String> labels) {

    public FiringAlert {
        labels = labels == null ? Map.of() : Map.copyOf(labels);
    }
}
