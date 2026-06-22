package coffeeshout.zzolbot.monitor.infra;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorRunRepository extends JpaRepository<MonitorRunEntity, Long> {

    List<MonitorRunEntity> findTop50ByOrderByCreatedAtDesc();
}
