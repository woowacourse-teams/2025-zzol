package coffeeshout.zzolbot.remediation.agent;

import coffeeshout.zzolbot.remediation.domain.DefectType;

/**
 * 코딩 에이전트에 넘기는 결함 컨텍스트. 토큰 절약을 위해 전체 repo가 아니라 특정된 대상 파일 한 개의
 * 소스만 담는다(스코핑이 토큰 최소화의 핵심 — 모델 선택이 아니라).
 */
public record DefectContext(
        DefectType defectType,
        String rootCauseHypothesis,
        String stackTrace,
        DefectLocation location,
        String targetSource) {
}
