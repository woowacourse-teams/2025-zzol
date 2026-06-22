package coffeeshout.zzolbot.monitor.application;

import coffeeshout.zzolbot.monitor.infra.MonitorRunEntity;
import coffeeshout.zzolbot.monitor.infra.MonitorRunRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 모니터링 실행 이력 조회 전용 서비스. 보강·영속은 {@link AlertEnrichmentService}가 담당하고,
 * 어드민 대시보드는 이 서비스로 최근 알림 목록·단건을 조회한다.
 */
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final MonitorRunRepository monitorRunRepository;

    public List<MonitorRunEntity> recentRuns() {
        return monitorRunRepository.findTop50ByOrderByCreatedAtDesc();
    }

    public Optional<MonitorRunEntity> findRun(Long id) {
        return monitorRunRepository.findById(id);
    }
}
