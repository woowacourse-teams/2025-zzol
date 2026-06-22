package coffeeshout.zzolbot.monitor.infra;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorRunRepository extends JpaRepository<MonitorRunEntity, Long> {

    List<MonitorRunEntity> findTop50ByOrderByCreatedAtDesc();

    /**
     * 중복 억제 윈도우 안에 같은 fingerprint로 이미 알림한 실행이 있는지. firing 웹훅 멱등 가드용으로,
     * {@code idx_zzolbot_monitor_run_cooldown(fingerprint, notified, created_at DESC)} 인덱스가 받친다.
     */
    boolean existsByFingerprintAndNotifiedTrueAndCreatedAtAfter(String fingerprint, Instant threshold);
}
