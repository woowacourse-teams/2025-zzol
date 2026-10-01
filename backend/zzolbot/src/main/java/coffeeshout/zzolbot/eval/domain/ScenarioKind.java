package coffeeshout.zzolbot.eval.domain;

/**
 * 평가 시나리오가 검증하는 대상 경로. 출처({@link ScenarioSource})와 직교한다 —
 * 어디서 온 시나리오인지와 무엇을 채점하는지는 별개의 축이다.
 */
public enum ScenarioKind {

    /** 운영자 질문에 대한 챗봇 진단 답변을 채점하는 시나리오. */
    CHAT,
    /** Alertmanager 알림에 대한 무인 분석(AnomalyAnalyzer) 결과를 채점하는 시나리오. */
    MONITOR
}
