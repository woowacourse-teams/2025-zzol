package coffeeshout.zzolbot.remediation.application;

import coffeeshout.zzolbot.monitor.application.MonitorService;
import coffeeshout.zzolbot.monitor.config.MonitorProperties;
import coffeeshout.zzolbot.monitor.infra.LokiLogClient;
import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
import coffeeshout.zzolbot.remediation.config.RemediationProperties;
import coffeeshout.zzolbot.remediation.domain.DefectType;
import coffeeshout.zzolbot.remediation.domain.RemediationRequest;
import coffeeshout.zzolbot.remediation.infra.GitHubDispatchClient;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptEntity;
import coffeeshout.zzolbot.remediation.infra.RemediationAttemptRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 운영자가 모니터링 알림에서 "수정 시도"를 누르면, 그 알림이 코드 결함인지 분류하고 게이트를 통과한
 * 경우에만 GitHub Actions 워커로 자동 수정 작업을 디스패치한다(ADR-0032의 능동 모니터링을 능동 대응으로 확장).
 *
 * <p>게이트는 네 겹이다: ① 결함 화이트리스트(틀린 PR 차단) ② fingerprint 쿨다운(동일 장애 PR 폭주 차단)
 * ③ 일일 디스패치 예산(오클릭·폭주 차단) ④ (워커측) 재현 테스트 RED→GREEN. 머지 게이트는 끝까지 사람과 CI다.
 *
 * <p>원격 HTTP(Loki 조회·GitHub dispatch)를 트랜잭션으로 감싸지 않으려고 각 저장을 독립 트랜잭션(Spring Data
 * 기본)으로 두고, 디스패치 실패 시 시도를 FAILED로 다시 저장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RemediationTriggerService {

    private static final int STACKTRACE_SAMPLE_LIMIT = 20;
    private static final String APP_FRAME_MARKER = "coffeeshout";

    private final RemediationProperties properties;
    private final MonitorProperties monitorProperties;
    private final MonitorService monitorService;
    private final DefectClassifier classifier;
    private final RemediationBudget budget;
    private final RemediationAttemptRepository attemptRepository;
    private final LokiLogClient lokiLogClient;
    private final GitHubDispatchClient dispatchClient;
    private final Clock clock;

    public RemediationDecision requestFix(Long runId) {
        if (!properties.enabled()) {
            return RemediationDecision.of(RemediationDecision.Outcome.DISABLED, "자동 수정이 비활성 상태입니다.");
        }
        final MonitorRunEntity run = monitorService.findRun(runId).orElse(null);
        if (run == null) {
            return RemediationDecision.of(RemediationDecision.Outcome.RUN_NOT_FOUND, "알림을 찾을 수 없습니다.");
        }

        final DefectType defectType = classifier.classify(run.getRootCauseHypothesis(), run.getSignalsJson());
        if (!properties.isWhitelisted(defectType)) {
            return RemediationDecision.of(RemediationDecision.Outcome.NOT_A_CODE_DEFECT,
                    "자동 수정 대상 코드 결함으로 분류되지 않았습니다(제안만 제공).");
        }
        if (recentlyAttempted(run.getFingerprint())) {
            return RemediationDecision.of(RemediationDecision.Outcome.COOLDOWN,
                    "같은 장애에 대해 최근 수정 시도가 있었습니다(쿨다운).");
        }
        if (!budget.tryAcquire()) {
            return RemediationDecision.of(RemediationDecision.Outcome.BUDGET_EXHAUSTED,
                    "일일 수정 디스패치 예산을 모두 사용했습니다.");
        }

        final String stackTrace = fetchStackTrace(run);
        final RemediationAttemptEntity attempt = attemptRepository.save(
                RemediationAttemptEntity.dispatched(run.getId(), run.getFingerprint(), defectType));
        try {
            dispatchClient.dispatch(toRequest(attempt.getId(), run, defectType, stackTrace));
            log.info("[ZzolBot] 자동 수정 디스패치. attemptId={}, runId={}, defectType={}",
                    attempt.getId(), run.getId(), defectType);
            return RemediationDecision.dispatched(attempt.getId(), defectType);
        } catch (Exception e) {
            log.warn("[ZzolBot] 자동 수정 디스패치 실패. attemptId={}", attempt.getId(), e);
            attempt.markFailed("디스패치 실패: " + e.getMessage());
            attemptRepository.save(attempt);
            return RemediationDecision.of(RemediationDecision.Outcome.DISPATCH_FAILED,
                    "워크플로우 디스패치에 실패했습니다: " + e.getMessage());
        }
    }

    private boolean recentlyAttempted(String fingerprint) {
        final Duration cooldown = properties.cooldown();
        if (cooldown.isZero() || fingerprint == null || fingerprint.isBlank()) {
            return false;
        }
        return attemptRepository.existsByFingerprintAndCreatedAtAfter(fingerprint, clock.instant().minus(cooldown));
    }

    /**
     * 알림 시점 윈도우의 ERROR 로그에서 {@code coffeeshout} 프레임을 담은 첫 항목을 스택트레이스로 고른다.
     * Alloy 멀티라인 병합 덕에 이 한 항목이 file:line 프레임을 온전히 포함한다. 없으면 빈 문자열.
     */
    private String fetchStackTrace(MonitorRunEntity run) {
        final List<String> errors = lokiLogClient.tailErrors(
                run.getCollectedAt(), monitorProperties.window(), STACKTRACE_SAMPLE_LIMIT);
        return errors.stream()
                .filter(line -> line != null && line.contains(APP_FRAME_MARKER))
                .findFirst()
                .orElse("");
    }

    private RemediationRequest toRequest(Long attemptId, MonitorRunEntity run, DefectType defectType, String stackTrace) {
        return new RemediationRequest(
                attemptId,
                run.getId(),
                run.getFingerprint(),
                run.getSeverity().name(),
                defectType,
                run.getRootCauseHypothesis(),
                List.of(),
                stackTrace);
    }
}
