package coffeeshout.zzolbot.eval.infra;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.zzolbot.eval.domain.EvalRunStatus;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

class EvalRunEntityTest {

    @Test
    void start_시_RUNNING_상태로_생성된다() {
        final EvalRunEntity run = EvalRunEntity.start("baseline", "gemini-2.5-flash", "v1", 5);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(run.getStatus()).isEqualTo(EvalRunStatus.RUNNING);
            softly.assertThat(run.getScenarioCount()).isEqualTo(5);
            softly.assertThat(run.getPassCount()).isZero();
            softly.assertThat(run.getStartedAt()).isNotNull();
            softly.assertThat(run.getFinishedAt()).isNull();
        });
    }

    @Test
    void complete_시_합격_수와_종료_시각이_기록된다() {
        final EvalRunEntity run = EvalRunEntity.start("baseline", "gemini-2.5-flash", "v1", 5);

        run.complete(4);

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(run.getStatus()).isEqualTo(EvalRunStatus.COMPLETED);
            softly.assertThat(run.getPassCount()).isEqualTo(4);
            softly.assertThat(run.getFinishedAt()).isNotNull();
        });
    }

    @Test
    void fail_시_FAILED_상태가_된다() {
        final EvalRunEntity run = EvalRunEntity.start("baseline", "gemini-2.5-flash", "v1", 5);

        run.fail();

        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(run.getStatus()).isEqualTo(EvalRunStatus.FAILED);
            softly.assertThat(run.getFinishedAt()).isNotNull();
        });
    }
}
