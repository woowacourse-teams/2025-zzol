package coffeeshout.profanity.application.port;

/**
 * Gemini 검열 호출의 하루 예산.
 *
 * <p>레이트리미터는 분당 속도만 고른다. 무료 티어에는 하루 총량 한도가 따로 있어서, 회차 주기를 줄이면
 * 속도 제한 안에서도 하루 한도를 넘길 수 있다(#1907).
 */
public interface NicknameAuditCallBudget {

    /**
     * 호출 한 번을 오늘 예산에서 뺀다.
     *
     * @return 예산이 남아 호출해도 되면 true, 오늘 상한에 닿았으면 false
     */
    boolean tryAcquire();
}
