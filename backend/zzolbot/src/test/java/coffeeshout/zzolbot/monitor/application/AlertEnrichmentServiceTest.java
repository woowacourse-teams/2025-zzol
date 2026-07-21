package coffeeshout.zzolbot.monitor.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.domain.FiringAlert;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.domain.Severity;
import coffeeshout.zzolbot.monitor.infra.AnomalyAnalyzer;
import coffeeshout.zzolbot.monitor.infra.LokiLogClient;
import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
import coffeeshout.zzolbot.monitor.infra.MonitorRunRepository;
import coffeeshout.zzolbot.monitor.infra.ZzolBotSlackNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlertEnrichmentServiceTest {

    private static final MonitorProperties PROPERTIES = new MonitorProperties(true, 240, 240);

    @Mock
    private LlmCallBudget llmCallBudget;
    @Mock
    private LokiLogClient lokiLogClient;
    @Mock
    private AnomalyAnalyzer analyzer;
    @Mock
    private ZzolBotSlackNotifier notifier;
    @Mock
    private MonitorRunRepository monitorRunRepository;

    private AlertEnrichmentService service;

    @BeforeEach
    void setUp() {
        service = new AlertEnrichmentService(llmCallBudget, lokiLogClient, analyzer, notifier,
                monitorRunRepository, PROPERTIES, new ObjectMapper(), Clock.systemUTC());
        given(monitorRunRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(lokiLogClient.tailErrors(any(), any(), anyInt())).willReturn(List.of());
    }

    @Test
    void 예산이_있으면_로그_샘플로_분석하고_알림_후_2회_저장한다() {
        given(llmCallBudget.tryAcquire()).willReturn(true);
        given(analyzer.analyze(any(), any()))
                .willReturn(new MonitorAnalysis("적체 발생", "컨슈머 지연", List.of("스케일 아웃")));

        service.enrich(warningAlert());

        verify(lokiLogClient).tailErrors(any(), any(), anyInt());
        verify(analyzer).analyze(any(), any());
        verify(notifier).notifyAnomaly(any(), any());
        final ArgumentCaptor<MonitorRunEntity> captor = ArgumentCaptor.forClass(MonitorRunEntity.class);
        verify(monitorRunRepository, times(2)).save(captor.capture());
        final MonitorRunEntity saved = captor.getValue();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(saved.isNotified()).isTrue();
            softly.assertThat(saved.getAnalysisSummary()).isEqualTo("적체 발생");
            softly.assertThat(saved.getSuggestedActionsJson()).contains("스케일 아웃");
        });
    }

    @Test
    void 예산이_소진되면_로그_분석을_건너뛰고_예산소진_분석으로_알림한다() {
        given(llmCallBudget.tryAcquire()).willReturn(false);

        service.enrich(warningAlert());

        verify(lokiLogClient, never()).tailErrors(any(), any(), anyInt());
        verify(analyzer, never()).analyze(any(), any());
        final ArgumentCaptor<MonitorAnalysis> captor = ArgumentCaptor.forClass(MonitorAnalysis.class);
        verify(notifier).notifyAnomaly(any(), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().summary()).contains("예산 소진");
    }

    @Test
    void 간격_내_재배달은_LLM_분석을_생략하되_이력은_남긴다() {
        given(monitorRunRepository.existsByDedupKeyAndNotifiedTrueAndCreatedAtAfter(any(), any()))
                .willReturn(true);

        service.enrich(warningAlert());

        verify(llmCallBudget, never()).tryAcquire();
        verify(notifier, never()).notifyAnomaly(any(), any());
        final ArgumentCaptor<MonitorRunEntity> captor = ArgumentCaptor.forClass(MonitorRunEntity.class);
        verify(monitorRunRepository).save(captor.capture());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(captor.getValue().getAnalysisSummary()).contains("이미 분석됨");
            // notified=false여야 이 중복 기록이 다시 쿨다운 기준점이 되어 창을 연장하지 않는다.
            softly.assertThat(captor.getValue().isNotified()).isFalse();
        });
    }

    @Test
    void 간격이_0이면_가드를_건너뛰고_분석한다() {
        final AlertEnrichmentService noDedup = new AlertEnrichmentService(llmCallBudget, lokiLogClient, analyzer,
                notifier, monitorRunRepository, new MonitorProperties(true, 240, 0), new ObjectMapper(),
                Clock.systemUTC());
        given(llmCallBudget.tryAcquire()).willReturn(true);
        given(analyzer.analyze(any(), any())).willReturn(new MonitorAnalysis("요약", "", List.of()));

        noDedup.enrich(warningAlert());

        verify(monitorRunRepository, never())
                .existsByDedupKeyAndNotifiedTrueAndCreatedAtAfter(any(), any());
        verify(notifier).notifyAnomaly(any(), any());
    }

    @Test
    void 모니터링이_비활성이면_아무것도_하지_않는다() {
        final AlertEnrichmentService disabled = new AlertEnrichmentService(llmCallBudget, lokiLogClient, analyzer,
                notifier, monitorRunRepository, new MonitorProperties(false, 240, 240), new ObjectMapper(),
                Clock.systemUTC());

        disabled.enrich(warningAlert());

        verify(monitorRunRepository, never()).save(any());
        verify(notifier, never()).notifyAnomaly(any(), any());
    }

    @Test
    void 분석이_실패해도_실패_분석으로_결정적_알림을_보낸다() {
        given(llmCallBudget.tryAcquire()).willReturn(true);
        given(analyzer.analyze(any(), any())).willThrow(new RuntimeException("Gemini 5xx"));

        service.enrich(warningAlert());

        final ArgumentCaptor<MonitorAnalysis> captor = ArgumentCaptor.forClass(MonitorAnalysis.class);
        verify(notifier).notifyAnomaly(any(), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().summary()).contains("실패");
    }

    @Test
    void severity_문자열을_심각도로_매핑한다() {
        given(llmCallBudget.tryAcquire()).willReturn(true);
        given(analyzer.analyze(any(), any())).willReturn(new MonitorAnalysis("요약", "", List.of()));

        service.enrich(alert("critical"));
        service.enrich(alert("warning"));

        final ArgumentCaptor<MonitorRunEntity> captor = ArgumentCaptor.forClass(MonitorRunEntity.class);
        verify(monitorRunRepository, times(4)).save(captor.capture());
        final List<MonitorRunEntity> saved = captor.getAllValues();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(saved.get(0).getSeverity()).isEqualTo(Severity.CRITICAL);
            softly.assertThat(saved.get(2).getSeverity()).isEqualTo(Severity.WARNING);
        });
    }

    @Nested
    @DisplayName("인시던트 단위 중복 판정")
    class IncidentDedup {

        @Test
        void 같은_incident_group_알림_쌍은_LLM을_한_번만_호출한다() {
            given(llmCallBudget.tryAcquire()).willReturn(true);
            given(analyzer.analyze(any(), any())).willReturn(new MonitorAnalysis("요약", "", List.of()));
            // 첫 알림(warning)이 분석된 뒤, 같은 인시던트의 critical이 뒤따르는 상황
            given(monitorRunRepository.existsByDedupKeyAndNotifiedTrueAndCreatedAtAfter(eq("ip-blocking"), any()))
                    .willReturn(false, true);

            service.enrich(groupedAlert("warning"));
            service.enrich(groupedAlert("critical"));

            verify(analyzer, times(1)).analyze(any(), any());
            verify(llmCallBudget, times(1)).tryAcquire();
        }

        @Test
        void incident_group이_있으면_그것을_중복_판정_키로_저장한다() {
            given(llmCallBudget.tryAcquire()).willReturn(true);
            given(analyzer.analyze(any(), any())).willReturn(new MonitorAnalysis("요약", "", List.of()));

            service.enrich(groupedAlert("critical"));

            final ArgumentCaptor<MonitorRunEntity> captor = ArgumentCaptor.forClass(MonitorRunEntity.class);
            verify(monitorRunRepository, times(2)).save(captor.capture());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(captor.getValue().getDedupKey()).isEqualTo("ip-blocking");
                // fingerprint는 개별 알림 식별자로 그대로 남는다.
                softly.assertThat(captor.getValue().getFingerprint()).isEqualTo("fp-1");
            });
        }

        @Test
        void incident_group이_없으면_fingerprint로_판정한다() {
            given(llmCallBudget.tryAcquire()).willReturn(true);
            given(analyzer.analyze(any(), any())).willReturn(new MonitorAnalysis("요약", "", List.of()));

            service.enrich(warningAlert());

            verify(monitorRunRepository).existsByDedupKeyAndNotifiedTrueAndCreatedAtAfter(eq("fp-1"), any());
        }

        @Test
        void 같은_그룹이라도_쿨다운이_지났으면_다시_분석한다() {
            given(llmCallBudget.tryAcquire()).willReturn(true);
            given(analyzer.analyze(any(), any())).willReturn(new MonitorAnalysis("요약", "", List.of()));
            given(monitorRunRepository.existsByDedupKeyAndNotifiedTrueAndCreatedAtAfter(any(), any()))
                    .willReturn(false);

            service.enrich(groupedAlert("warning"));
            service.enrich(groupedAlert("critical"));

            verify(analyzer, times(2)).analyze(any(), any());
        }
    }

    private FiringAlert warningAlert() {
        return alert("warning");
    }

    private FiringAlert groupedAlert(String severity) {
        return new FiringAlert("MassIpBlockingSpike", severity, "fp-1", "IP 차단 급증", "임계 초과",
                Map.of("alertname", "MassIpBlockingSpike", "severity", severity,
                        "incident_group", "ip-blocking"));
    }

    private FiringAlert alert(String severity) {
        return new FiringAlert("AppErrorLogSpike", severity, "fp-1", "ERROR 급증", "임계 초과",
                Map.of("alertname", "AppErrorLogSpike", "severity", severity));
    }
}
