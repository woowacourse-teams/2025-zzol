package coffeeshout.zzolbot.remediation.application;

/**
 * 일일 자동 수정 디스패치 예산. 사람이 트리거하더라도 오클릭·스크립트성 폭주를 막는 하드 캡이다.
 * fingerprint 쿨다운이 같은 장애의 반복을 막는다면, 이 예산은 전체 디스패치 수의 일일 상한을 건다.
 */
public interface RemediationBudget {

    /**
     * 디스패치 1건을 예산에서 차감 시도한다. 잔여가 있으면 true, 소진됐으면 false.
     */
    boolean tryAcquire();

    /**
     * 오늘 남은 디스패치 수.
     */
    long remaining();
}
