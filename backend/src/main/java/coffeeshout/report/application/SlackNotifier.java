package coffeeshout.report.application;

import coffeeshout.report.application.event.ReportSubmittedEvent;
import coffeeshout.report.config.SlackProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class SlackNotifier {

    private final SlackProperties slackProperties;
    private final RestClient restClient;

    public SlackNotifier(SlackProperties slackProperties, @Qualifier("slackRestClient") RestClient restClient) {
        this.slackProperties = slackProperties;
        this.restClient = restClient;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReportSubmitted(ReportSubmittedEvent event) {
        if (!slackProperties.isEnabled()) {
            log.debug("Slack webhook URL이 설정되지 않아 알림을 건너뜁니다. reportId={}", event.reportId());
            return;
        }

        try {
            restClient.post()
                    .uri(slackProperties.webhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(buildMessage(event))
                    .retrieve()
                    .toBodilessEntity();

            log.info("Slack 신고 알림 전송 완료: reportId={}", event.reportId());
        } catch (Exception e) {
            log.error("Slack 신고 알림 전송 실패: reportId={}", event.reportId(), e);
        }
    }

    private Map<String, Object> buildMessage(ReportSubmittedEvent event) {
        final List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(mrkdwnField("*카테고리*", event.category().label));
        if (event.gameType() != null) {
            fields.add(mrkdwnField("*게임*", event.gameType().label));
        }
        if (event.joinCode() != null) {
            fields.add(mrkdwnField("*방코드*", "`" + event.joinCode() + "`"));
        }

        return Map.of("blocks", List.of(
                Map.of("type", "header",
                        "text", Map.of("type", "plain_text", "text", "📋 신고 접수 #" + event.reportId())),
                Map.of("type", "section", "fields", fields),
                Map.of("type", "section",
                        "text", Map.of("type", "plain_text", "text", event.content())),
                Map.of("type", "divider")
        ));
    }

    private Map<String, Object> mrkdwnField(String title, String value) {
        return Map.of("type", "mrkdwn", "text", title + "\n" + value);
    }
}
