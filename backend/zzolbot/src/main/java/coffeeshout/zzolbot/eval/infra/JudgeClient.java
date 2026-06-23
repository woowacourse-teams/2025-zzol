package coffeeshout.zzolbot.eval.infra;

import coffeeshout.zzolbot.eval.domain.JudgeScore;

/**
 * LLM-as-a-judge 계약. 피평가 답변을 채점 기준(rubric)에 비추어 점수화한다.
 * 운영(Gemini)·테스트(NoOp) 구현을 프로파일로 교체한다.
 */
public interface JudgeClient {

    JudgeScore evaluate(String question, String rubric, String answer);
}
