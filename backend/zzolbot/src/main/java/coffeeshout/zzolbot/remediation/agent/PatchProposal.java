package coffeeshout.zzolbot.remediation.agent;

/**
 * 코딩 에이전트가 제안한 수정. 적용 안정성을 위해 unified diff 대신 대상 파일의 "전체 새 내용"과
 * 재현 테스트 "전체 소스"를 담는다(diff 적용 fuzz를 피한다). 오케스트레이터가 그대로 파일에 쓴다.
 *
 * <p>재현 테스트는 수정 전 RED, 수정 후 GREEN이어야 게이트를 통과한다. 컴파일조차 안 되면 NO_FIX로 떨어지고
 * PR은 열리지 않는다 — 테스트 게이트가 환각 수정을 막는 핵심 안전장치다.
 */
public record PatchProposal(
        String targetPath,
        String modifiedSource,
        String reproTestPath,
        String reproTestSource,
        String rationale) {
}
