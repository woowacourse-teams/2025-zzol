package coffeeshout.zzolbot.monitor.ui;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;

/**
 * Alertmanager webhook(v4) 페이로드의 수신용 부분 스키마.
 * 그룹화된 {@code alerts[]}를 싣고, 그룹 전체와 개별 알림 각각 {@code status}(firing|resolved)를 가진다.
 * 보강은 firing 알림에만 의미가 있으므로 수신기가 {@code alerts[]}를 순회하며 필터링한다.
 *
 * <p>스키마 진화에 견디도록 미지 필드는 무시한다(ADR-0032 골격).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertmanagerWebhookRequest(String version, String status, List<Alert> alerts) {

    public AlertmanagerWebhookRequest {
        alerts = alerts == null ? List.of() : List.copyOf(alerts);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Alert(
            String status,
            Map<String, String> labels,
            Map<String, String> annotations,
            String startsAt,
            String fingerprint) {

        public Alert {
            labels = labels == null ? Map.of() : Map.copyOf(labels);
            annotations = annotations == null ? Map.of() : Map.copyOf(annotations);
        }

        public boolean isFiring() {
            return "firing".equalsIgnoreCase(status);
        }
    }
}
