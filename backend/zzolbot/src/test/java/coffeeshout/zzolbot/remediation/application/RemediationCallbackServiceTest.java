package coffeeshout.zzolbot.remediation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import coffeeshout.zzolbot.remediation.domain.DefectType;
import coffeeshout.zzolbot.remediation.domain.RemediationStatus;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptEntity;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemediationCallbackServiceTest {

    @Mock
    private RemediationAttemptRepository attemptRepository;

    @InjectMocks
    private RemediationCallbackService service;

    private RemediationAttemptEntity dispatchedAttempt() {
        return RemediationAttemptEntity.dispatched(1L, "fp", DefectType.NULL_POINTER);
    }

    @Test
    void PR_OPENED_콜백은_PR_정보를_반영한다() {
        final RemediationAttemptEntity attempt = dispatchedAttempt();
        given(attemptRepository.findById(7L)).willReturn(Optional.of(attempt));

        service.apply(7L, RemediationStatus.PR_OPENED, "http://pr/1", 1, "be/zzolbot/auto-fix-fp", null);

        assertThat(attempt.getStatus()).isEqualTo(RemediationStatus.PR_OPENED);
        assertThat(attempt.getPrUrl()).isEqualTo("http://pr/1");
        verify(attemptRepository).save(attempt);
    }

    @Test
    void NO_FIX_콜백은_사유를_기록하고_저장한다() {
        final RemediationAttemptEntity attempt = dispatchedAttempt();
        given(attemptRepository.findById(7L)).willReturn(Optional.of(attempt));

        service.apply(7L, RemediationStatus.NO_FIX, null, null, null, "재현 실패");

        assertThat(attempt.getStatus()).isEqualTo(RemediationStatus.NO_FIX);
        assertThat(attempt.getDetail()).isEqualTo("재현 실패");
        verify(attemptRepository).save(attempt);
    }

    @Test
    void FAILED_콜백은_FAILED로_기록한다() {
        final RemediationAttemptEntity attempt = dispatchedAttempt();
        given(attemptRepository.findById(7L)).willReturn(Optional.of(attempt));

        service.apply(7L, RemediationStatus.FAILED, null, null, null, "워커 오류");

        assertThat(attempt.getStatus()).isEqualTo(RemediationStatus.FAILED);
        assertThat(attempt.getDetail()).isEqualTo("워커 오류");
        verify(attemptRepository).save(attempt);
    }

    @Test
    void 허용되지_않는_상태_콜백은_저장하지_않는다() {
        final RemediationAttemptEntity attempt = dispatchedAttempt();
        given(attemptRepository.findById(7L)).willReturn(Optional.of(attempt));

        service.apply(7L, RemediationStatus.DISPATCHED, null, null, null, null);

        assertThat(attempt.getStatus()).isEqualTo(RemediationStatus.DISPATCHED);
        verify(attemptRepository, never()).save(any());
    }

    @Test
    void 대상_시도가_없으면_무시한다() {
        given(attemptRepository.findById(404L)).willReturn(Optional.empty());

        service.apply(404L, RemediationStatus.PR_OPENED, "x", 1, "b", null);

        verify(attemptRepository, never()).save(any());
    }

    @Test
    void status가_null이면_NPE없이_무시한다() {
        service.apply(7L, null, null, null, null, null);

        verify(attemptRepository, never()).findById(any());
        verify(attemptRepository, never()).save(any());
    }
}
