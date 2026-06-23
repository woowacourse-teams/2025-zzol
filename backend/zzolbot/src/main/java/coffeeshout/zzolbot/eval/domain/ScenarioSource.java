package coffeeshout.zzolbot.eval.domain;

/**
 * 평가 시나리오(골든 데이터셋)의 출처.
 */
public enum ScenarioSource {

    /** 운영 진단 세션을 라이브 1회 실행해 도구 결과를 녹화한 시나리오. */
    RECORDED,
    /** 도구 결과를 수기로 작성한 시나리오. */
    MANUAL,
    /** 포스트모템 장애 사례를 변환한 시나리오. */
    POSTMORTEM
}
