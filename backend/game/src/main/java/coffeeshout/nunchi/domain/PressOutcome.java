package coffeeshout.nunchi.domain;

/**
 * {@link NunchiGame#press}의 판정 결과. 호출자(Flow/서비스)가 이걸 보고 브로드캐스트한다.
 */
public enum PressOutcome {

    /** 정상 일어서기(낙관적·즉시). 해당 번호의 첫 입력. */
    STOOD,

    /** 충돌 — 윈도우 안 동시 입력. 그룹 전원 OUT, 쿨다운 진입. */
    COLLIDED,

    /** 무시 — 이미 입력했거나, 쿨다운 중 윈도우 밖이거나, 종료 상태(warn 로그만, ADR 결정 1). */
    IGNORED,
}
