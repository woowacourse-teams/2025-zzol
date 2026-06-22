package coffeeshout.zzolbot.monitor.ui;

import coffeeshout.zzolbot.monitor.application.FiringAlertEnricher;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.ui.AlertmanagerWebhookRequest.Alert;
import java.util.List;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alertmanager가 발화시킨 알림을 받아 zzol-bot LLM 분석으로 넘기는 웹훅 수신기(ADR-0032).
 * zzol-bot은 더 이상 자체 탐지/폴링을 하지 않고, 단일 알림 엔진(Alertmanager)의 firing 알림만 분석한다.
 *
 * <p>분석(Loki 조회 + LLM 분석 + Slack)은 수 초가 걸려, 요청 스레드에서 동기로 처리하면 Alertmanager
 * 웹훅 타임아웃을 넘겨 재시도(중복 배달)·Tomcat 스레드 점유를 유발한다. 그래서 firing 파싱만 요청
 * 스레드에서 하고 실제 분석은 가상 스레드 풀로 넘긴 뒤 즉시 200으로 ack한다.
 *
 * <p><b>내부 전용.</b> nginx 내부 리스너로만 도달하고 Spring Security가 베어러 토큰으로 막는다.
 */
@Slf4j
@RestController
@RequestMapping("/internal/zzolbot/alerts")
public class AlertmanagerWebhookController {

    private final FiringAlertEnricher enricher;
    private final ExecutorService virtualThreadExecutor;

    public AlertmanagerWebhookController(
            FiringAlertEnricher enricher,
            @Qualifier("virtualThreadExecutor") ExecutorService virtualThreadExecutor) {
        this.enricher = enricher;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receive(@RequestBody AlertmanagerWebhookRequest request) {
        final List<FiringAlert> firingAlerts = request.alerts().stream()
                .filter(Alert::isFiring)
                .map(AlertmanagerWebhookController::toFiringAlert)
                .toList();
        // 분석은 가상 스레드로 넘기고 즉시 ack — Alertmanager 타임아웃·재시도와 Tomcat 스레드 점유를 막는다.
        firingAlerts.forEach(alert -> virtualThreadExecutor.execute(() -> enrichSafely(alert)));
        return ResponseEntity.ok().build();
    }

    private void enrichSafely(FiringAlert alert) {
        try {
            enricher.enrich(alert);
        } catch (Exception e) {
            log.warn("[ZzolBot] 알림 분석 실패. fingerprint={}", alert.fingerprint(), e);
        }
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
