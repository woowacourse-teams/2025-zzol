package coffeeshout.zzolbot.remediation.agent;

/**
 * 결함 컨텍스트를 받아 수정안(재현 테스트 + 최소 수정)을 제안하는 포트. 구현을 교체하면(예: Gemini→다른 모델)
 * 오케스트레이션은 그대로 둘 수 있다. GitHub Actions 워커에서 CLI로 실행되며 Spring 컨텍스트에 의존하지 않는다.
 */
public interface CodingAgent {

    PatchProposal propose(DefectContext context);
}
