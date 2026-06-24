package coffeeshout.nunchi.domain;

/**
 * 눈치게임 상태기계(ADR-0031 결정 8). {@code DESCRIPTION → READY → PLAYING ↔ COLLISION_COOLDOWN → DONE}.
 */
public enum NunchiState {

    /** 규칙 설명 — 시작 직후 짧게 대기하며 입력을 받지 않는다(다른 미니게임과 동일). */
    DESCRIPTION,

    /** 곧 시작 카운트다운 — 입력을 받지 않고 playStartEpochMs까지 대기, 전원 동시에 PLAYING 진입. */
    READY,

    /** 카운터 활성 — 입력을 수락한다. */
    PLAYING,

    /** 충돌 직후 대기 — 재개 전까지 새 입력을 무시한다(윈도우 안 늦은 입력은 합류). */
    COLLISION_COOLDOWN,

    /** 종료. */
    DONE,
}
