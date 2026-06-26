package coffeeshout.zzolbot.remediation.infra;

import coffeeshout.zzolbot.remediation.domain.RemediationRequest;

/**
 * GitHub repository_dispatch로 자동 수정 워크플로우를 트리거하는 포트. 앱은 탐지·분류·디스패치만 하고,
 * 실제 코드 수정·빌드·테스트·PR 발행은 앱 컨테이너 밖 GitHub Actions 워커가 수행한다(앱은 git/gradle/gh 미보유).
 */
public interface GitHubDispatchClient {

    void dispatch(RemediationRequest request);
}
