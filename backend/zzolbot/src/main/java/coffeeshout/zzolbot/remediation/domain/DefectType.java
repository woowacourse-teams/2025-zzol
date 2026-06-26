package coffeeshout.zzolbot.remediation.domain;

/**
 * 자동 수정 봇이 다루는 결함 유형. 분류기({@code DefectClassifier})가 근본원인 가설·신호에서 추정한다.
 *
 * <p>MVP는 {@link #NULL_POINTER} 한 종류만 화이트리스트로 둔다. 국소적이고 검증 가능한(재현 테스트로
 * RED→GREEN을 만들 수 있는) 결함만 자동 수정 대상으로 삼는다 — "LLM이 안전하게 고칠 수 있는 범위"를
 * 좁히는 게 핵심 안전장치다. 화이트리스트 밖({@link #UNKNOWN})은 수정 시도하지 않고 제안에 머문다.
 */
public enum DefectType {

    /**
     * NPE 계열 — {@code Optional.orElseThrow()}/{@code get()}/널 역참조로 발생한 NullPointerException.
     */
    NULL_POINTER,

    /**
     * 자동 수정 대상이 아닌 결함(인프라·외부 의존·설정 등) 또는 분류 불가.
     */
    UNKNOWN
}
