package coffeeshout.zzolbot.monitor.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.domain.AnomalyVerdict;
import coffeeshout.zzolbot.monitor.domain.MonitorAnalysis;
import coffeeshout.zzolbot.monitor.domain.MonitorSignal;
import coffeeshout.zzolbot.monitor.domain.MonitorSnapshot;
import coffeeshout.zzolbot.monitor.domain.Severity;
import coffeeshout.zzolbot.monitor.infra.AnomalyAnalyzer;
import coffeeshout.zzolbot.monitor.infra.LokiLogClient;
import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
import coffeeshout.zzolbot.monitor.infra.MonitorRunRepository;
import coffeeshout.zzolbot.monitor.infra.ZzolBotSlackNotifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MonitorServiceTest {

    private static final MonitorProperties PROPERTIES =
            new MonitorProperties(true, "0 0 */4 * * *", 10, 10000, 100, 240, 50, 30);

    @Mock
    private MonitorCollector collector;
    @Mock
    private AnomalyGate gate;
    @Mock
    private AnomalyAnalyzer analyzer;
    @Mock
    private LokiLogClient lokiLogClient;
    @Mock
    private ZzolBotSlackNotifier notifier;
    @Mock
    private MonitorRunRepository monitorRunRepository;
    @Mock
    private LlmCallBudget llmCallBudget;

    private MonitorService service;

    @BeforeEach
    void setUp() {
        service = new MonitorService(collector, gate, analyzer, lokiLogClient, notifier, monitorRunRepository,
                llmCallBudget, PROPERTIES, new ObjectMapper(), Clock.systemUTC());
        given(collector.collect()).willReturn(snapshot());
        given(monitorRunRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(monitorRunRepository.findFirstByNotifiedTrueOrderByCreatedAtDesc()).willReturn(Optional.empty());
        given(lokiLogClient.tailErrors(any(), any(), org.mockito.ArgumentMatchers.anyInt())).willReturn(java.util.List.of());
    }

    @Test
    void 정상이면_LLM_분석과_알림_없이_저장한다() {
        given(gate.evaluate(any())).willReturn(AnomalyVerdict.normal());

        final MonitorRunEntity run = service.runOnce();

        verify(analyzer, never()).analyze(any(), any(), any());
        verify(notifier, never()).notifyAnomaly(any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(run.isNotified()).isFalse();
    }

    @Test
    void 이상이고_예산이_있으면_LLM_분석_후_알림한다() {
        given(gate.evaluate(any())).willReturn(anomalous());
        given(llmCallBudget.tryAcquire()).willReturn(true);
        given(analyzer.analyze(any(), any(), any()))
                .willReturn(new MonitorAnalysis("적체 발생", "컨슈머 지연", List.of("스케일 아웃")));

        final MonitorRunEntity run = service.runOnce();

        verify(analyzer).analyze(any(), any(), any());
        verify(notifier).notifyAnomaly(any(), any(), any());
        org.assertj.core.api.Assertions.assertThat(run.isNotified()).isTrue();
    }

    @Test
    void 이상이지만_예산이_소진되면_LLM_분석을_생략하고_알림만_한다() {
        given(gate.evaluate(any())).willReturn(anomalous());
        given(llmCallBudget.tryAcquire()).willReturn(false);

        service.runOnce();

        verify(analyzer, never()).analyze(any(), any(), any());
        final ArgumentCaptor<MonitorAnalysis> captor = ArgumentCaptor.forClass(MonitorAnalysis.class);
        verify(notifier).notifyAnomaly(any(), any(), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().summary()).contains("예산 소진");
    }

    @Test
    void 이상이고_예산이_있지만_분석이_실패하면_결정적_신호만_알린다() {
        given(gate.evaluate(any())).willReturn(anomalous());
        given(llmCallBudget.tryAcquire()).willReturn(true);
        given(analyzer.analyze(any(), any(), any())).willThrow(new RuntimeException("Gemini 5xx"));

        service.runOnce();

        final ArgumentCaptor<MonitorAnalysis> captor = ArgumentCaptor.forClass(MonitorAnalysis.class);
        verify(notifier).notifyAnomaly(any(), any(), captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().summary()).contains("실패");
    }

    @Test
    void 쿨다운_중인_동일_이상은_분석과_알림을_건너뛴다() {
        given(gate.evaluate(any())).willReturn(anomalous());
        final MonitorRunEntity lastNotified = MonitorRunEntity.of(Instant.now(), anomalous(), "[]");
        lastNotified.markNotified();
        given(monitorRunRepository.findFirstByNotifiedTrueOrderByCreatedAtDesc())
                .willReturn(Optional.of(lastNotified));

        service.runOnce();

        verify(analyzer, never()).analyze(any(), any(), any());
        verify(notifier, never()).notifyAnomaly(any(), any(), any());
    }

    private MonitorSnapshot snapshot() {
        return new MonitorSnapshot(List.of(MonitorSignal.of("outbox_dead_letter", 15, 10)), Instant.now());
    }

    private AnomalyVerdict anomalous() {
        return new AnomalyVerdict(true, Severity.WARNING, "outbox_dead_letter");
    }
}
