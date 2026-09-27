package coffeeshout.zzolbot.monitor.infra;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MonitorShadowRunRepository extends JpaRepository<MonitorShadowRunEntity, Long> {

    List<MonitorShadowRunEntity> findTop50ByOrderByCreatedAtDesc();
}
