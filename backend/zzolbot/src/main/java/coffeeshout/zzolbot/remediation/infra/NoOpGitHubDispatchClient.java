package coffeeshout.zzolbot.remediation.infra;

import coffeeshout.zzolbot.remediation.domain.RemediationRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * 테스트 환경에서 실제 GitHub 호출 없이 디스패치를 로그로만 남긴다.
 */
@Slf4j
@Component
@Profile("test")
public class NoOpGitHubDispatchClient implements GitHubDispatchClient {

    @Override
    public void dispatch(RemediationRequest request) {
        log.info("[ZzolBot] NoOp 디스패치 — attemptId={}, defectType={}", request.attemptId(), request.defectType());
    }
}
