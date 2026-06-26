package coffeeshout.zzolbot.remediation.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.remediation.domain.DefectType;
import coffeeshout.zzolbot.remediation.domain.RemediationStatus;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class RemediationAttemptEntityTest {

    @Test
    void dispatched는_DISPATCHED_상태로_생성된다() {
        final RemediationAttemptEntity attempt = RemediationAttemptEntity.dispatched(5L, "fp", DefectType.NULL_POINTER);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(attempt.getMonitorRunId()).isEqualTo(5L);
            softly.assertThat(attempt.getFingerprint()).isEqualTo("fp");
            softly.assertThat(attempt.getDefectType()).isEqualTo(DefectType.NULL_POINTER);
            softly.assertThat(attempt.getStatus()).isEqualTo(RemediationStatus.DISPATCHED);
            softly.assertThat(attempt.getCreatedAt()).isNotNull();
            softly.assertThat(attempt.getUpdatedAt()).isNotNull();
        });
    }

    @Test
    void markPrOpened는_PR_정보를_채운다() {
        final RemediationAttemptEntity attempt = RemediationAttemptEntity.dispatched(5L, "fp", DefectType.NULL_POINTER);

        attempt.markPrOpened("http://pr/9", 9, "be/zzolbot/auto-fix-fp");

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(attempt.getStatus()).isEqualTo(RemediationStatus.PR_OPENED);
            softly.assertThat(attempt.getPrUrl()).isEqualTo("http://pr/9");
            softly.assertThat(attempt.getPrNumber()).isEqualTo(9);
            softly.assertThat(attempt.getBranchName()).isEqualTo("be/zzolbot/auto-fix-fp");
        });
    }

    @Test
    void markNoFix와_markFailed는_상태와_사유를_기록한다() {
        final RemediationAttemptEntity noFix = RemediationAttemptEntity.dispatched(1L, "fp", DefectType.NULL_POINTER);
        noFix.markNoFix("재현 실패");
        final RemediationAttemptEntity failed = RemediationAttemptEntity.dispatched(1L, "fp", DefectType.NULL_POINTER);
        failed.markFailed("워커 오류");

        assertThat(noFix.getStatus()).isEqualTo(RemediationStatus.NO_FIX);
        assertThat(noFix.getDetail()).isEqualTo("재현 실패");
        assertThat(failed.getStatus()).isEqualTo(RemediationStatus.FAILED);
        assertThat(failed.getDetail()).isEqualTo("워커 오류");
    }
}
