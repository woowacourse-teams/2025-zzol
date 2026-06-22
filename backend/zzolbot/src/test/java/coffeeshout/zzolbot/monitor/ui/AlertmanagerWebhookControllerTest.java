package coffeeshout.zzolbot.monitor.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.zzolbot.monitor.application.FiringAlertEnricher;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.ui.AlertmanagerWebhookRequest.Alert;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlertmanagerWebhookControllerTest {

    @Mock
    private FiringAlertEnricher enricher;

    @Captor
    private ArgumentCaptor<FiringAlert> captor;

    private AlertmanagerWebhookController controller;

    @BeforeEach
    void setUp() {
        controller = new AlertmanagerWebhookController(enricher);
    }

    @Test
    void firing_알림은_라벨_어노테이션을_매핑해_보강기로_넘긴다() {
        final Alert firing = new Alert(
                "firing",
                Map.of("alertname", "AppErrorLogSpike", "severity", "warning"),
                Map.of("summary", "ERROR 급증", "description", "임계 초과"),
                "2026-06-22T00:00:00Z",
                "fp-1");

        controller.receive(new AlertmanagerWebhookRequest("4", "firing", List.of(firing)));

        verify(enricher).enrich(captor.capture());
        final FiringAlert captured = captor.getValue();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(captured.alertname()).isEqualTo("AppErrorLogSpike");
            softly.assertThat(captured.severity()).isEqualTo("warning");
            softly.assertThat(captured.fingerprint()).isEqualTo("fp-1");
            softly.assertThat(captured.summary()).isEqualTo("ERROR 급증");
            softly.assertThat(captured.description()).isEqualTo("임계 초과");
        });
    }

    @Test
    void resolved_알림은_보강하지_않는다() {
        final Alert resolved = new Alert(
                "resolved", Map.of("alertname", "AppErrorLogSpike"), Map.of(), "2026-06-22T00:00:00Z", "fp-2");

        controller.receive(new AlertmanagerWebhookRequest("4", "resolved", List.of(resolved)));

        verify(enricher, never()).enrich(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void alerts가_null이어도_빈_목록으로_안전하게_처리한다() {
        final var response = controller.receive(new AlertmanagerWebhookRequest("4", "firing", null));

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(enricher, never()).enrich(org.mockito.ArgumentMatchers.any());
    }
}
