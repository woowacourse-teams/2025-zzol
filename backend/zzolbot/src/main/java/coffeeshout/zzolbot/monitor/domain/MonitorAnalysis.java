package coffeeshout.zzolbot.monitor.domain;

import java.util.List;

/**
 * 이상 발생 시 LLM이 생성한 근본원인 분석. 조치(suggestedActions)는 제안만 하며 자동 실행하지 않는다.
 */
public record MonitorAnalysis(String summary, String rootCauseHypothesis, List<String> suggestedActions) {

    public MonitorAnalysis {
        suggestedActions = List.copyOf(suggestedActions);
    }

    public static MonitorAnalysis budgetExhausted() {
        return new MonitorAnalysis("LLM 분석 생략 (일일 예산 소진)", "", List.of());
    }

    public static MonitorAnalysis failed() {
        return new MonitorAnalysis("LLM 분석 실패", "", List.of());
    }
}
