package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * firing 알림 발생 시 Slack으로 알린다. webhook 미설정 시 조용히 건너뛴다.
 * LLM 분석이 생략/실패해도 결정적 알림은 항상 전송한다.
 */
@Slf4j
@Component
public class ZzolBotSlackNotifier {

    private final ZzolBotSlackProperties properties;
    private final RestClient restClient;

    public ZzolBotSlackNotifier(
            ZzolBotSlackProperties properties,
            @Qualifier("zzolBotSlackRestClient") RestClient restClient) {
        this.properties = properties;
        this.restClient = restClient;
    }

    public void notifyAnomaly(FiringAlert alert, MonitorAnalysis analysis) {
        if (!properties.isEnabled()) {
            log.debug("[ZzolBot] Slack webhook 미설정 — 알림 생략. severity={}", alert.severity());
            return;
        }
        try {
            restClient.post()
                    .uri(properties.webhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildMessage(alert, analysis))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[ZzolBot] 이상 알림 전송 완료. severity={}, fingerprint={}",
                    alert.severity(), alert.fingerprint());
        } catch (Exception e) {
            log.error("[ZzolBot] 이상 알림 전송 실패. fingerprint={}", alert.fingerprint(), e);
        }
    }

    private Map<String, Object> buildMessage(FiringAlert alert, MonitorAnalysis analysis) {
        final List<Map<String, Object>> blocks = new ArrayList<>();
        final String severity = alert.severity() == null ? "" : alert.severity().toUpperCase();
        blocks.add(Map.of("type", "header",
                "text", Map.of("type", "plain_text",
                        "text", "🚨 ZzolBot 이상 감지 [" + severity + "]")));

        final String alertText = "*알림*\n• " + alert.alertname() + "\n"
                + nullToEmpty(alert.summary()) + "\n" + nullToEmpty(alert.description());
        blocks.add(Map.of("type", "section",
                "text", Map.of("type", "mrkdwn", "text", alertText)));

        blocks.add(Map.of("type", "section",
                "text", Map.of("type", "mrkdwn", "text", "*분석*\n" + analysis.summary())));

        if (!analysis.suggestedActions().isEmpty()) {
            final String actions = "*제안 조치*\n" + String.join("\n",
                    analysis.suggestedActions().stream().map(a -> "• " + a).toList());
            blocks.add(Map.of("type", "section",
                    "text", Map.of("type", "mrkdwn", "text", actions)));
        }
        blocks.add(Map.of("type", "divider"));
        return Map.of("blocks", blocks);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
