package coffeeshout.zzolbot.monitor.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.assertj.core.api.Assertions;
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

    private static final MonitorProperties PROPERTIES = new MonitorProperties(true, 30, 240);
    private static final List<String> LOG_SAMPLES = List.of("[ERROR] 컨슈머 처리 실패");

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
        given(lokiLogClient.tailErrors(any(), any(), anyInt(), anyString())).willReturn(LOG_SAMPLES);
        given(lokiLogClient.defaultEnvironment()).willReturn("prod");
    }

    @Test
    void 예산이_있으면_로그_샘플로_분석하고_알림_후_2회_저장한다() {
        given(llmCallBudget.tryAcquire()).willReturn(true);
        given(analyzer.analyze(any(), any(), anyString()))
                .willReturn(new MonitorAnalysis("적체 발생", "컨슈머 지연", List.of("스케일 아웃"), true));

        service.enrich(warningAlert());

        verify(lokiLogClient).tailErrors(any(), any(), anyInt(), anyString());
        verify(analyzer).analyze(any(), any(), anyString());
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
    void 간격_내_동일_fingerprint_재배달은_분석을_생략한다() {
        given(monitorRunRepository.existsByFingerprintAndNotifiedTrueAndCreatedAtAfter(any(), any()))
                .willReturn(true);

        service.enrich(warningAlert());

        verify(monitorRunRepository, never()).save(any());
        verify(llmCallBudget, never()).tryAcquire();
        verify(notifier, never()).notifyAnomaly(any(), any());
    }

    @Test
    void 간격이_0이면_가드를_건너뛰고_분석한다() {
        final AlertEnrichmentService noDedup = new AlertEnrichmentService(llmCallBudget, lokiLogClient, analyzer,
                notifier, monitorRunRepository, new MonitorProperties(true, 30, 0), new ObjectMapper(),
                Clock.systemUTC());
        given(llmCallBudget.tryAcquire()).willReturn(true);
        given(analyzer.analyze(any(), any(), anyString()))
                .willReturn(new MonitorAnalysis("요약", "", List.of(), true));

        noDedup.enrich(warningAlert());

        verify(monitorRunRepository, never())
                .existsByFingerprintAndNotifiedTrueAndCreatedAtAfter(any(), any());
        verify(notifier).notifyAnomaly(any(), any());
    }

    @Test
    void 모니터링이_비활성이면_아무것도_하지_않는다() {
        final AlertEnrichmentService disabled = new AlertEnrichmentService(llmCallBudget, lokiLogClient, analyzer,
                notifier, monitorRunRepository, new MonitorProperties(false, 30, 240), new ObjectMapper(),
                Clock.systemUTC());

        disabled.enrich(warningAlert());

        verify(monitorRunRepository, never()).save(any());
        verify(notifier, never()).notifyAnomaly(any(), any());
    }

    @Test
    void 분석이_실패해도_실패_분석으로_결정적_알림을_보낸다() {
        given(llmCallBudget.tryAcquire()).willReturn(true);
        given(analyzer.analyze(any(), any(), anyString())).willThrow(new RuntimeException("Gemini 5xx"));

        service.enrich(warningAlert());

        final ArgumentCaptor<MonitorAnalysis> captor = ArgumentCaptor.forClass(MonitorAnalysis.class);
        verify(notifier).notifyAnomaly(any(), captor.capture());
        Assertions.assertThat(captor.getValue().summary()).contains("실패");
    }

    @Test
    void severity_문자열을_심각도로_매핑한다() {
        given(llmCallBudget.tryAcquire()).willReturn(true);
        given(analyzer.analyze(any(), any(), anyString()))
                .willReturn(new MonitorAnalysis("요약", "", List.of(), true));

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
    @DisplayName("조회 환경 판별")
    class ResolveEnvironment {

        @Test
        void job_라벨에서_환경을_유도해_해당_환경_로그를_조회한다() {
            given(llmCallBudget.tryAcquire()).willReturn(true);
            given(analyzer.analyze(any(), any(), anyString()))
                    .willReturn(new MonitorAnalysis("요약", "", List.of(), true));

            service.enrich(alertWithJob("dev-app"));

            verify(lokiLogClient).tailErrors(any(), any(), anyInt(), eq("dev"));
            verify(analyzer).analyze(any(), any(), eq("dev"));
        }

        @Test
        void job_라벨이_없으면_실행_환경으로_폴백한다() {
            given(llmCallBudget.tryAcquire()).willReturn(true);
            given(analyzer.analyze(any(), any(), anyString()))
                    .willReturn(new MonitorAnalysis("요약", "", List.of(), true));

            service.enrich(warningAlert());

            verify(lokiLogClient).tailErrors(any(), any(), anyInt(), eq("prod"));
        }

        @Test
        void 접미사가_없는_job_라벨은_그대로_환경으로_쓴다() {
            given(llmCallBudget.tryAcquire()).willReturn(true);
            given(analyzer.analyze(any(), any(), anyString()))
                    .willReturn(new MonitorAnalysis("요약", "", List.of(), true));

            service.enrich(alertWithJob("staging"));

            verify(lokiLogClient).tailErrors(any(), any(), anyInt(), eq("staging"));
        }
    }

    @Nested
    @DisplayName("근거 없는 분석 방지")
    class NoEvidenceGuard {

        @Test
        void 로그가_없으면_LLM을_호출하지_않고_예산도_소모하지_않는다() {
            given(lokiLogClient.tailErrors(any(), any(), anyInt(), anyString())).willReturn(List.of());

            service.enrich(warningAlert());

            verify(analyzer, never()).analyze(any(), any(), anyString());
            verify(llmCallBudget, never()).tryAcquire();
        }

        @Test
        void 로그가_없으면_근거_없음을_명시해_알린다() {
            given(lokiLogClient.tailErrors(any(), any(), anyInt(), anyString())).willReturn(List.of());

            service.enrich(warningAlert());

            final ArgumentCaptor<MonitorAnalysis> captor = ArgumentCaptor.forClass(MonitorAnalysis.class);
            verify(notifier).notifyAnomaly(any(), captor.capture());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(captor.getValue().summary()).contains("근거가 없다");
                softly.assertThat(captor.getValue().evidenceFound()).isFalse();
            });
        }

        @Test
        void 예산이_소진되면_LLM_분석을_건너뛰고_예산소진_분석으로_알린다() {
            given(llmCallBudget.tryAcquire()).willReturn(false);

            service.enrich(warningAlert());

            verify(analyzer, never()).analyze(any(), any(), anyString());
            final ArgumentCaptor<MonitorAnalysis> captor = ArgumentCaptor.forClass(MonitorAnalysis.class);
            verify(notifier).notifyAnomaly(any(), captor.capture());
            Assertions.assertThat(captor.getValue().summary()).contains("예산 소진");
        }
    }

    @Nested
    @DisplayName("조회 근거 기록")
    class AnalysisContextRecording {

        @Test
        void 무엇을_근거로_분석했는지_저장한다() {
            given(llmCallBudget.tryAcquire()).willReturn(true);
            given(analyzer.analyze(any(), any(), anyString()))
                    .willReturn(new MonitorAnalysis("적체 발생", "컨슈머 지연", List.of(), true));

            service.enrich(alertWithJob("prod-app"));

            final ArgumentCaptor<MonitorRunEntity> captor = ArgumentCaptor.forClass(MonitorRunEntity.class);
            verify(monitorRunRepository, times(2)).save(captor.capture());
            final String signals = captor.getValue().getSignalsJson();
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(signals).contains("\"logEnvironment\":\"prod\"");
                softly.assertThat(signals).contains("\"windowMinutes\":30");
                softly.assertThat(signals).contains("\"logSampleCount\":1");
                softly.assertThat(signals).contains("\"evidenceFound\":true");
                softly.assertThat(signals).contains("컨슈머 지연");
            });
        }

        @Test
        void 근거가_없다는_판정도_그대로_기록한다() {
            given(llmCallBudget.tryAcquire()).willReturn(true);
            given(analyzer.analyze(any(), any(), anyString()))
                    .willReturn(new MonitorAnalysis("무관한 로그뿐", "", List.of(), false));

            service.enrich(warningAlert());

            final ArgumentCaptor<MonitorRunEntity> captor = ArgumentCaptor.forClass(MonitorRunEntity.class);
            verify(monitorRunRepository, times(2)).save(captor.capture());
            Assertions.assertThat(captor.getValue().getSignalsJson()).contains("\"evidenceFound\":false");
        }
    }

    private FiringAlert warningAlert() {
        return alert("warning");
    }

    private FiringAlert alert(String severity) {
        return new FiringAlert("AppErrorLogSpike", severity, "fp-1", "ERROR 급증", "임계 초과",
                Map.of("alertname", "AppErrorLogSpike", "severity", severity));
    }

    private FiringAlert alertWithJob(String job) {
        return new FiringAlert("AppErrorLogSpike", "warning", "fp-1", "ERROR 급증", "임계 초과",
                Map.of("alertname", "AppErrorLogSpike", "severity", "warning", "job", job));
    }
}
