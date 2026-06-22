package coffeeshout.zzolbot.monitor.ui;

import coffeeshout.zzolbot.monitor.application.FiringAlert;
import coffeeshout.zzolbot.monitor.application.FiringAlertEnricher;
import coffeeshout.zzolbot.monitor.ui.AlertmanagerWebhookRequest.Alert;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alertmanager가 발화시킨 알림을 받아 zzol-bot LLM 보강으로 넘기는 웹훅 수신기(ADR-0032).
 * zzol-bot은 더 이상 자체 탐지/폴링을 하지 않고, 단일 알림 엔진(Alertmanager)의 firing 알림만 보강한다.
 *
 * <p><b>내부 전용.</b> 이 경로는 nginx로 외부 노출하면 안 된다(임의 알림 주입 방지). 후속 PR에서
 * {@code /internal/**}를 nginx 차단 + Spring Security로 제한한다(ADR-0032 트레이드오프).
 */
@RestController
@RequestMapping("/internal/zzolbot/alerts")
public class AlertmanagerWebhookController {

    private final FiringAlertEnricher enricher;

    public AlertmanagerWebhookController(FiringAlertEnricher enricher) {
        this.enricher = enricher;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(@RequestBody AlertmanagerWebhookRequest request) {
        request.alerts().stream()
                .filter(Alert::isFiring)
                .map(AlertmanagerWebhookController::toFiringAlert)
                .forEach(enricher::enrich);
        return ResponseEntity.ok().build();
    }

    private static FiringAlert toFiringAlert(Alert alert) {
        return new FiringAlert(
                alert.labels().get("alertname"),
                alert.labels().get("severity"),
                alert.fingerprint(),
                alert.annotations().get("summary"),
                alert.annotations().get("description"),
                alert.labels());
    }
}
