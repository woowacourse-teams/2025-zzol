package coffeeshout.zzolbot.remediation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import coffeeshout.zzolbot.monitor.application.MonitorService;
import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.infra.LokiLogClient;
import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
import coffeeshout.zzolbot.remediation.application.RemediationDecision.Outcome;
import coffeeshout.zzolbot.remediation.config.RemediationProperties;
import coffeeshout.zzolbot.remediation.domain.DefectType;
import coffeeshout.zzolbot.remediation.domain.RemediationRequest;
import coffeeshout.zzolbot.remediation.infra.GitHubDispatchClient;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptEntity;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptRepository;
import coffeeshout.zzolbot.monitor.domain.Severity;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
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
class RemediationTriggerServiceTest {

    private static final RemediationProperties ENABLED = new RemediationProperties(
            true, "token", "owner", "repo", 5, 240, List.of(DefectType.NULL_POINTER));
    private static final MonitorProperties MONITOR = new MonitorProperties(true, 240, 240);
    private static final String NPE_SIGNALS = "{\"summary\":\"NullPointerException on orderId\"}";

    @Mock
    private MonitorService monitorService;
    @Mock
    private RemediationBudget budget;
    @Mock
    private RemediationAttemptRepository attemptRepository;
    @Mock
    private LokiLogClient lokiLogClient;
    @Mock
    private GitHubDispatchClient dispatchClient;

    private RemediationTriggerService service;

    @BeforeEach
    void setUp() {
        service = newService(ENABLED);
        given(budget.tryAcquire()).willReturn(true);
        given(lokiLogClient.tailErrors(any(), any(), anyInt()))
                .willReturn(List.of("\tat coffeeshout.room.application.RoomService.find(RoomService.java:42)"));
        given(attemptRepository.save(any())).willAnswer(inv -> {
            final RemediationAttemptEntity e = inv.getArgument(0);
            if (e.getId() == null) {
                ReflectionTestUtils.setField(e, "id", 100L);
            }
            return e;
        });
    }

    private RemediationTriggerService newService(RemediationProperties properties) {
        return new RemediationTriggerService(properties, MONITOR, monitorService, new DefectClassifier(),
                budget, attemptRepository, lokiLogClient, dispatchClient, Clock.systemUTC());
    }

    private MonitorRunEntity run(String signalsJson) {
        final MonitorRunEntity entity = MonitorRunEntity.of(Instant.now(), Severity.WARNING, "fp-1", signalsJson);
        ReflectionTestUtils.setField(entity, "id", 1L);
        return entity;
    }

    @Test
    void 코드결함이면_디스패치한다() {
        given(monitorService.findRun(1L)).willReturn(Optional.of(run(NPE_SIGNALS)));

        final RemediationDecision decision = service.requestFix(1L);

        assertThat(decision.outcome()).isEqualTo(Outcome.DISPATCHED);
        assertThat(decision.attemptId()).isEqualTo(100L);
        verify(dispatchClient, times(1)).dispatch(any(RemediationRequest.class));
    }

    @Test
    void 비활성이면_디스패치하지_않는다() {
        final RemediationDecision decision = newService(new RemediationProperties(
                false, "token", "owner", "repo", 5, 240, List.of(DefectType.NULL_POINTER))).requestFix(1L);

        assertThat(decision.outcome()).isEqualTo(Outcome.DISABLED);
        verify(monitorService, never()).findRun(any());
    }

    @Test
    void 알림이_없으면_RUN_NOT_FOUND() {
        given(monitorService.findRun(99L)).willReturn(Optional.empty());

        assertThat(service.requestFix(99L).outcome()).isEqualTo(Outcome.RUN_NOT_FOUND);
    }

    @Test
    void 코드결함이_아니면_NOT_A_CODE_DEFECT() {
        given(monitorService.findRun(1L)).willReturn(Optional.of(run("{\"summary\":\"HikariCP 풀 고갈\"}")));

        final RemediationDecision decision = service.requestFix(1L);

        assertThat(decision.outcome()).isEqualTo(Outcome.NOT_A_CODE_DEFECT);
        verify(dispatchClient, never()).dispatch(any());
    }

    @Test
    void 쿨다운_중이면_COOLDOWN() {
        given(monitorService.findRun(1L)).willReturn(Optional.of(run(NPE_SIGNALS)));
        given(attemptRepository.existsByFingerprintAndCreatedAtAfter(any(), any())).willReturn(true);

        final RemediationDecision decision = service.requestFix(1L);

        assertThat(decision.outcome()).isEqualTo(Outcome.COOLDOWN);
        verify(budget, never()).tryAcquire();
        verify(dispatchClient, never()).dispatch(any());
    }

    @Test
    void 예산_소진이면_BUDGET_EXHAUSTED() {
        given(monitorService.findRun(1L)).willReturn(Optional.of(run(NPE_SIGNALS)));
        given(budget.tryAcquire()).willReturn(false);

        final RemediationDecision decision = service.requestFix(1L);

        assertThat(decision.outcome()).isEqualTo(Outcome.BUDGET_EXHAUSTED);
        verify(dispatchClient, never()).dispatch(any());
    }

    @Test
    void 디스패치_실패면_DISPATCH_FAILED이고_시도를_FAILED로_저장한다() {
        given(monitorService.findRun(1L)).willReturn(Optional.of(run(NPE_SIGNALS)));
        willThrow(new RuntimeException("boom")).given(dispatchClient).dispatch(any());

        final RemediationDecision decision = service.requestFix(1L);

        assertThat(decision.outcome()).isEqualTo(Outcome.DISPATCH_FAILED);
        verify(attemptRepository, times(2)).save(any());
    }
}
