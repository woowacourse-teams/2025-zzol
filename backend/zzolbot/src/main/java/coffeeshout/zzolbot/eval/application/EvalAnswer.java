package coffeeshout.zzolbot.eval.application;

/**
 * 평가기가 산출한 봇 출력. answer는 judge 채점 대상 텍스트이고,
 * missingToolCalls는 replay 중 스냅샷에 없어 실패 처리된 도구 호출 수다(도구가 없는 경로는 0).
 */
public record EvalAnswer(String answer, int missingToolCalls) {}
