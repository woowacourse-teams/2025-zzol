package coffeeshout.nunchi.domain;

/**
 * 눈치게임 최종 순위의 3계층(ADR-0031 결정 2). 좋음→나쁨 순이다.
 *
 * <p>{@link NunchiScore}가 이 계층을 단일 {@code long} 밴드로 인코딩하며, 결과 응답 DTO는
 * 이 값을 노출해 FE가 같은 rank 안에서 충돌·미입력 배지를 구분하게 한다(결정 N7).
 */
public enum NunchiTier {

    /** 정상(단독) 입력 — 충돌 없이 깨끗하게 누른 사람. 누른 순서대로 상위. */
    SOLO,

    /** 충돌 실패 — 동시(윈도우 이내)에 누른 그룹. 발생 순서대로 스택(먼저 충돌이 더 나쁨). */
    COLLISION,

    /** 미입력(타임아웃) 실패 — 끝내 누르지 않은 사람. 제일 꼴등이며 서로 동점. */
    MISS,
}
