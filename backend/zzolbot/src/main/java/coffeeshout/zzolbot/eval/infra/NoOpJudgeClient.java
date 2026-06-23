package coffeeshout.zzolbot.eval.infra;

import coffeeshout.zzolbot.eval.domain.EvalVerdict;
import coffeeshout.zzolbot.eval.domain.JudgeScore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트 환경에서 Gemini 호출 없이 고정 점수를 반환한다.
 */
@Component
@Profile("test")
public class NoOpJudgeClient implements JudgeClient {

    @Override
    public JudgeScore evaluate(String question, String rubric, String answer) {
        return new JudgeScore(5, 5, false, EvalVerdict.PASS, "NoOp judge");
    }
}
