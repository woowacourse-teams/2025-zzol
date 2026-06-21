package coffeeshout.zzolbot.monitor.ui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import coffeeshout.zzolbot.monitor.application.MonitorService;
import coffeeshout.zzolbot.monitor.domain.AnomalyVerdict;
import coffeeshout.zzolbot.monitor.domain.Severity;
import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
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

    private ZzolBotMonitorController controller;

    @BeforeEach
    void setUp() {
        controller = new ZzolBotMonitorController(monitorService, Clock.systemDefaultZone());
    }

    @Test
    void run_은_수동_점검을_실행하고_결과를_반환한다() {
        given(monitorService.runOnce()).willReturn(run(true, Severity.WARNING));

        final ZzolBotMonitorController.AlertResponse response = controller.run();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(response.anomalous()).isTrue();
            softly.assertThat(response.severity()).isEqualTo("WARNING");
        });
        verify(monitorService).runOnce();
    }

    @Test
    void alerts_는_최근_실행을_AlertResponse로_변환한다() {
        given(monitorService.recentRuns()).willReturn(List.of(run(false, Severity.NORMAL)));

        final List<ZzolBotMonitorController.AlertResponse> result = controller.alerts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).anomalous()).isFalse();
    }

    @Test
    void 존재하지_않는_alert는_404를_반환한다() {
        given(monitorService.findRun(999L)).willReturn(Optional.empty());

        assertThat(controller.alert(999L).getStatusCode().value()).isEqualTo(404);
    }

    private MonitorRunEntity run(boolean anomalous, Severity severity) {
        final MonitorRunEntity entity = MonitorRunEntity.of(
                Instant.now(), new AnomalyVerdict(anomalous, severity, "fp"), "[]");
        ReflectionTestUtils.setField(entity, "id", 1L);
        return entity;
    }
}
