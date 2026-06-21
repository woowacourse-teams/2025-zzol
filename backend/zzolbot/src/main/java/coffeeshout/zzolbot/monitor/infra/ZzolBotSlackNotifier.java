package coffeeshout.zzolbot.monitor.infra;

import coffeeshout.zzolbot.monitor.domain.AnomalyVerdict;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.domain.MonitorSignal;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 이상 발생 시 Slack으로 알림한다. webhook 미설정 시 조용히 건너뛴다.
 * LLM 분석이 생략/실패해도 결정적 신호는 항상 알린다.
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

    public void notifyAnomaly(MonitorSnapshot snapshot, AnomalyVerdict verdict, MonitorAnalysis analysis) {
        if (!properties.isEnabled()) {
            log.debug("[ZzolBot] Slack webhook 미설정 — 알림 생략. severity={}", verdict.severity());
            return;
        }
        try {
            restClient.post()
                    .uri(properties.webhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildMessage(snapshot, verdict, analysis))
                    .retrieve()
                    .toBodilessEntity();
            log.info("[ZzolBot] 이상 알림 전송 완료. severity={}, fingerprint={}",
                    verdict.severity(), verdict.fingerprint());
        } catch (Exception e) {
            log.error("[ZzolBot] 이상 알림 전송 실패. fingerprint={}", verdict.fingerprint(), e);
        }
    }

    private Map<String, Object> buildMessage(
            MonitorSnapshot snapshot, AnomalyVerdict verdict, MonitorAnalysis analysis) {
        final List<Map<String, Object>> blocks = new ArrayList<>();
        blocks.add(Map.of("type", "header",
                "text", Map.of("type", "plain_text",
                        "text", "🚨 ZzolBot 이상 감지 [" + verdict.severity() + "]")));

        final StringBuilder signalText = new StringBuilder("*초과 신호*\n");
        for (MonitorSignal signal : snapshot.signals()) {
            if (signal.breached()) {
                signalText.append("• `").append(signal.name()).append("`: ")
                        .append(signal.value()).append(" (임계 ").append(signal.threshold()).append(")\n");
            }
        }
        blocks.add(Map.of("type", "section",
                "text", Map.of("type", "mrkdwn", "text", signalText.toString())));

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
}
