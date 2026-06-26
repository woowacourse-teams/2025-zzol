package coffeeshout.zzolbot.monitor.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import coffeeshout.zzolbot.monitor.application.MonitorService;
import coffeeshout.zzolbot.monitor.domain.Severity;
import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ZzolBotMonitorControllerTest {

    @Mock
    private MonitorService monitorService;

    @Mock
    private RemediationAttemptRepository attemptRepository;

    private ZzolBotMonitorController controller;

    @BeforeEach
    void setUp() {
        controller = new ZzolBotMonitorController(monitorService, attemptRepository, Clock.systemDefaultZone());
    }

    @Test
    void alerts_는_최근_실행을_AlertResponse로_변환한다() {
        given(monitorService.recentRuns()).willReturn(List.of(run(Severity.WARNING)));
        given(attemptRepository.findByMonitorRunIdInOrderByCreatedAtDesc(any())).willReturn(List.of());

        final List<ZzolBotMonitorController.AlertResponse> result = controller.alerts();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(result).hasSize(1);
            softly.assertThat(result.get(0).anomalous()).isTrue();
            softly.assertThat(result.get(0).severity()).isEqualTo("WARNING");
        });
    }

    @Test
    void 존재하지_않는_alert는_404를_반환한다() {
        given(monitorService.findRun(999L)).willReturn(Optional.empty());

        assertThat(controller.alert(999L).getStatusCode().value()).isEqualTo(404);
    }

    private MonitorRunEntity run(Severity severity) {
        final MonitorRunEntity entity = MonitorRunEntity.of(Instant.now(), severity, "fp", "{}");
        ReflectionTestUtils.setField(entity, "id", 1L);
        return entity;
    }
}
