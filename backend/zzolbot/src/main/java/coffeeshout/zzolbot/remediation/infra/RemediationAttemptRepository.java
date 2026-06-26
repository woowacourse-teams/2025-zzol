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
     * 주어진 실행들의 수정 시도 <b>전체</b>를 created_at DESC로 한 번에 조회한다(목록 화면 N+1 방지).
     * 실행별 최신 1건은 호출측이 DESC 순서에서 실행별 첫 항목으로 선택한다(이력 전체를 내려준다는 점에 유의).
     */
    List<RemediationAttemptEntity> findByMonitorRunIdInOrderByCreatedAtDesc(List<Long> monitorRunIds);

    /**
     * 같은 fingerprint로 쿨다운 윈도우 안에 이미 수정 시도가 있는지. 동일 장애에 대한 PR 폭주를 막는 가드로,
     * {@code idx_zzolbot_remediation_attempt_cooldown(fingerprint, created_at DESC)} 인덱스가 받친다.
     */
    boolean existsByFingerprintAndCreatedAtAfter(String fingerprint, Instant threshold);
}
