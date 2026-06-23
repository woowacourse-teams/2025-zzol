package coffeeshout.zzolbot.eval.domain;

/**
 * LLM-as-a-judge가 피평가 답변에 매긴 점수.
 *
 * @param accuracy             정확성 (0~5)
 * @param groundedness         근거 충실성 — 도구 결과를 실제로 인용했는가 (0~5)
 * @param hallucinationDetected 없는 테이블·수치를 지어냈는가
 * @param verdict              종합 합격 여부
 * @param rationale            판정 근거
 */
public record JudgeScore(
        int accuracy,
        int groundedness,
        boolean hallucinationDetected,
        EvalVerdict verdict,
        String rationale
) {
}
