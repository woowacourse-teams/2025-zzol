package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.application.FiringAlert;
import coffeeshout.zzolbot.monitor.application.FiringAlertEnricher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@link FiringAlertEnricher}의 골격 구현 — 수신한 firing 알림을 로깅만 한다(ADR-0032).
 *
 * <p>TODO(ADR-0032 후속 PR): 기존 {@code AnomalyAnalyzer}로 LLM 근본원인 분석을 생성하고
 * {@code ZzolBotSlackNotifier}로 enriched 메시지를 Slack에 게시하도록 교체한다. 그 시점에
 * {@code MonitorCollector}/{@code MonitorScheduler}/{@code AnomalyGate}의 자체 폴링·게이팅은 제거한다.
 */
@Slf4j
@Component
public class LoggingFiringAlertEnricher implements FiringAlertEnricher {

    @Override
    public void enrich(FiringAlert alert) {
        log.info("[ZzolBot] firing 알림 수신(골격 — 보강 미배선). alertname={}, severity={}, fingerprint={}, summary={}",
                alert.alertname(), alert.severity(), alert.fingerprint(), alert.summary());
    }
}
