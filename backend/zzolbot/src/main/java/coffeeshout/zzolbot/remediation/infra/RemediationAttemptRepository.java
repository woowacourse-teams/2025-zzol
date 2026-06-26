package coffeeshout.zzolbot.remediation.infra;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RemediationAttemptRepository extends JpaRepository<RemediationAttemptEntity, Long> {

    /**
     * 모니터링 실행별 최신 수정 시도. 어드민 Monitor 탭이 알림 행에 인라인 상태/PR 링크로 보여준다.
     */
    Optional<RemediationAttemptEntity> findTopByMonitorRunIdOrderByCreatedAtDesc(Long monitorRunId);

    /**
     * 여러 실행의 최신 시도를 한 번에 조회(목록 화면 N+1 방지)할 때 쓴다.
     */
    List<RemediationAttemptEntity> findByMonitorRunIdInOrderByCreatedAtDesc(List<Long> monitorRunIds);

    /**
     * 같은 fingerprint로 쿨다운 윈도우 안에 이미 수정 시도가 있는지. 동일 장애에 대한 PR 폭주를 막는 가드로,
     * {@code idx_zzolbot_remediation_attempt_cooldown(fingerprint, created_at DESC)} 인덱스가 받친다.
     */
    boolean existsByFingerprintAndCreatedAtAfter(String fingerprint, Instant threshold);
}
