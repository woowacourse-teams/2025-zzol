package coffeeshout.zzolbot.monitor.infra;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorRunRepository extends JpaRepository<MonitorRunEntity, Long> {

    List<MonitorRunEntity> findTop50ByOrderByCreatedAtDesc();

    /**
     * 중복 억제 윈도우 안에 같은 중복 판정 키로 이미 알림한 실행이 있는지. firing 웹훅 멱등 가드용으로,
     * {@code idx_zzolbot_monitor_run_dedup(dedup_key, notified, created_at DESC)} 인덱스가 받친다.
     * <p>
     * fingerprint가 아니라 dedup_key로 본다 — 한 인시던트가 알림 2건으로 발화하면 fingerprint는
     * 서로 달라 가드를 통과해, 같은 장애에 LLM을 두 번 태웠다(#1598).
     */
    boolean existsByDedupKeyAndNotifiedTrueAndCreatedAtAfter(String dedupKey, Instant threshold);
}
