package coffeeshout.report.application;

import coffeeshout.report.application.event.ReportSubmittedEvent;
import coffeeshout.report.config.SlackProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(SlackProperties.class)
public class SlackNotifier {

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "BUG", "버그",
            "SUGGESTION", "건의사항",
            "GAME_REQUEST", "게임 요청",
            "OTHER", "기타"
    );

    private static final Map<String, String> GAME_TYPE_LABELS = Map.of(
            "CARD_GAME", "카드게임",
            "RACING_GAME", "레이싱",
            "SPEED_TOUCH", "스피드터치",
            "BLIND_TIMER", "블라인드타이머",
            "BOMB_RELAY", "폭탄릴레이",
            "BLOCK_STACKING", "블록쌓기"
    );

    private final SlackProperties slackProperties;
    private final RestClient restClient = RestClient.create();

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
        final String categoryLabel = CATEGORY_LABELS.getOrDefault(event.category().name(), event.category().name());
        
        final String gameInfo = event.gameType() != null
                ? String.format(" | 게임: `%s`", GAME_TYPE_LABELS.getOrDefault(event.gameType().name(), event.gameType().name()))
                : "";
        final String joinCodeInfo = event.joinCode() != null
                ? String.format(" | 방코드: `%s`", event.joinCode())
                : "";

        final String text = String.format(
                "*[신고 #%d]* `%s`%s%s\n>%s",
                event.reportId(),
                categoryLabel,
                gameInfo,
                joinCodeInfo,
                event.content()
        );

        return Map.of("text", text);
    }
}
