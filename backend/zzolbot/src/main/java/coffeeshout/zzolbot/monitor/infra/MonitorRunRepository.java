package coffeeshout.zzolbot.monitor.infra;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorRunRepository extends JpaRepository<MonitorRunEntity, Long> {

    List<MonitorRunEntity> findTop50ByOrderByCreatedAtDesc();

    Optional<MonitorRunEntity> findFirstByFingerprintAndNotifiedTrueOrderByCreatedAtDesc(String fingerprint);
}
