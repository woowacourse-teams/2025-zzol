package coffeeshout.zzolbot.monitor.application;

/**
 * 일일 LLM 호출 예산. 스케줄 작업이 무료 티어/할당량을 초과하지 못하도록 하드 캡을 건다.
 * rate limiter(RPM 평탄화)와 달리 하루 총 호출 수를 제한한다.
 */
public interface LlmCallBudget {

    /**
     * 호출 1건을 예산에서 차감 시도한다. 잔여 예산이 있으면 true, 소진됐으면 false.
     */
    boolean tryAcquire();

    /**
     * 오늘 남은 호출 수.
     */
    long remaining();
}
