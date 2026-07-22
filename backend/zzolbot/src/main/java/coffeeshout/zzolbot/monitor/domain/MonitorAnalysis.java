package coffeeshout.zzolbot.monitor.domain;

import java.util.List;

/**
 * 이상 발생 시 LLM이 생성한 근본원인 분석. 조치(suggestedActions)는 제안만 하며 자동 실행하지 않는다.
 * <p>
 * {@code evidenceFound}는 "이 분석이 실제 로그 근거에 기반했는가"를 나타낸다. 알림과 무관한 로그만
 * 주어졌는데도 그럴듯한 인과를 지어내는 실패를 드러내기 위한 필드로, LLM이 스스로 판정한다(#1594).
 * 판정이 누락되면 보수적으로 false로 본다 — 운영봇에서는 과소 주장이 과대 주장보다 안전하다.
 */
public record MonitorAnalysis(
        String summary,
        String rootCauseHypothesis,
        List<String> suggestedActions,
        boolean evidenceFound
) {

    public MonitorAnalysis {
        suggestedActions = List.copyOf(suggestedActions);
    }

    public static MonitorAnalysis budgetExhausted() {
        return new MonitorAnalysis("LLM 분석 생략 (일일 예산 소진)", "", List.of(), false);
    }

    public static MonitorAnalysis failed() {
        return new MonitorAnalysis("LLM 분석 실패", "", List.of(), false);
    }

    /**
     * 조회 구간에 ERROR 로그가 없어 분석 근거를 확보하지 못한 경우. LLM을 호출하지 않으므로
     * 예산도 소모하지 않는다. 근거 없이 결론을 만드는 대신 근거가 없다는 사실 자체를 알린다.
     */
    public static MonitorAnalysis noEvidence(String environment, int windowMinutes) {
        return new MonitorAnalysis(
                "LLM 분석 생략 — %s 환경의 최근 %d분 구간에서 ERROR 로그를 찾지 못해 알림을 뒷받침할 근거가 없다."
                        .formatted(environment, windowMinutes),
                "",
                List.of(),
                false);
    }
}
