package coffeeshout.global.metric;

import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.repository.MemoryRoomRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Room 상태별 활성 수를 Gauge로 수집한다.
 *
 * <p>MemoryRoomRepository의 인메모리 Room 데이터를 기반으로
 * RoomState별(READY, PLAYING, SCORE_BOARD, ROULETTE, DONE) 활성 Room 수와
 * 전체 Room 수를 Prometheus에 노출한다.</p>
 *
 * <p>Prometheus 메트릭명:
 * <ul>
 *   <li>room_active_count (tag: state)</li>
 *   <li>room_total_count</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
public class RoomActiveMetricService {

    private final MemoryRoomRepository memoryRoomRepository;
    private final MeterRegistry meterRegistry;

    public RoomActiveMetricService(MemoryRoomRepository memoryRoomRepository, MeterRegistry meterRegistry) {
        this.memoryRoomRepository = memoryRoomRepository;
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initializeMetrics() {
        for (RoomState state : RoomState.values()) {
            Gauge.builder("room.active.count", () -> memoryRoomRepository.countByState(state))
                    .description("상태별 활성 Room 수")
                    .tag("state", state.name())
                    .register(meterRegistry);
        }

        Gauge.builder("room.total.count", memoryRoomRepository::totalCount)
                .description("전체 활성 Room 수")
                .register(meterRegistry);

        log.info("Room 활성 수 메트릭 등록 완료: states={}", RoomState.values().length);
    }
}
