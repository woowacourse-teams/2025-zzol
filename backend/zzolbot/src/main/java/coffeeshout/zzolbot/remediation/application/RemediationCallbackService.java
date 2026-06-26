package coffeeshout.zzolbot.remediation.application;

import coffeeshout.zzolbot.remediation.domain.RemediationStatus;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptEntity;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GitHub Actions 워커가 작업을 끝낸 뒤 결과를 보고하면 해당 수정 시도 상태를 갱신한다.
 * 워커는 PR을 열었으면 {@link RemediationStatus#PR_OPENED}(+pr_url), 통과하는 수정을 못 만들었으면
 * {@link RemediationStatus#NO_FIX}, 실행 중 오류면 {@link RemediationStatus#FAILED}를 보고한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemediationCallbackService {

    private final RemediationAttemptRepository attemptRepository;

    @Transactional
    public void apply(Long attemptId, RemediationStatus status, String prUrl, Integer prNumber,
                      String branchName, String detail) {
        if (status == null) {
            // 공개+토큰 경로라 토큰을 가진 잘못된 호출이 status 없이 올 수 있다. NPE→500 대신 무시한다.
            log.warn("[ZzolBot] 콜백 status 누락 — 무시. attemptId={}", attemptId);
            return;
        }
        final RemediationAttemptEntity attempt = attemptRepository.findById(attemptId).orElse(null);
        if (attempt == null) {
            log.warn("[ZzolBot] 콜백 대상 수정 시도 없음. attemptId={}", attemptId);
            return;
        }
        switch (status) {
            case PR_OPENED -> attempt.markPrOpened(prUrl, prNumber, branchName);
            case NO_FIX -> attempt.markNoFix(detail);
            case FAILED -> attempt.markFailed(detail);
            default -> {
                log.warn("[ZzolBot] 콜백에 허용되지 않는 상태. attemptId={}, status={}", attemptId, status);
                return;
            }
        }
        attemptRepository.save(attempt);
        log.info("[ZzolBot] 수정 시도 갱신. attemptId={}, status={}", attemptId, status);
    }
}
