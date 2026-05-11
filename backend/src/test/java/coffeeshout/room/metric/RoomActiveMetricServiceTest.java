package coffeeshout.room.metric;

import static org.assertj.core.api.Assertions.assertThat;

import coffeeshout.room.domain.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.PlayerName;
import coffeeshout.room.domain.repository.MemoryRoomRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoomActiveMetricServiceTest {

    private MeterRegistry meterRegistry;
    private MemoryRoomRepository memoryRoomRepository;
    private RoomActiveMetricService roomActiveMetricService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        memoryRoomRepository = new MemoryRoomRepository();
        roomActiveMetricService = new RoomActiveMetricService(memoryRoomRepository, meterRegistry);
        roomActiveMetricService.initializeMetrics();
    }

    @Test
    void 모든_RoomState에_대해_Gauge가_등록된다() {
        // when & then
        for (RoomState state : RoomState.values()) {
            Gauge gauge = meterRegistry.find("room.active.count")
                    .tag("state", state.name())
                    .gauge();
            assertThat(gauge)
                    .as("state=%s에 대한 Gauge가 등록되어야 한다", state.name())
                    .isNotNull();
            assertThat(gauge.value()).isEqualTo(0.0);
        }

        Gauge totalGauge = meterRegistry.find("room.total.count").gauge();
        assertThat(totalGauge).isNotNull();
        assertThat(totalGauge.value()).isEqualTo(0.0);
    }

    @Test
    void Room_생성_시_READY_상태_Gauge가_증가한다() {
        // given
        Room room = Room.createNewRoom(new JoinCode("ABC4"), new PlayerName("host1"), null, 0.7);
        memoryRoomRepository.save(room);

        // when
        Gauge readyGauge = meterRegistry.find("room.active.count")
                .tag("state", RoomState.READY.name())
                .gauge();
        Gauge totalGauge = meterRegistry.find("room.total.count").gauge();

        // then
        assertThat(readyGauge.value()).isEqualTo(1.0);
        assertThat(totalGauge.value()).isEqualTo(1.0);
    }

    @Test
    void 여러_Room의_상태별_Gauge가_정확히_집계된다() {
        // given
        Room room1 = Room.createNewRoom(new JoinCode("ABC6"), new PlayerName("host1"), null, 0.7);
        Room room2 = Room.createNewRoom(new JoinCode("D7F4"), new PlayerName("host2"), null, 0.7);

        memoryRoomRepository.save(room1);
        memoryRoomRepository.save(room2);

        // when
        Gauge readyGauge = meterRegistry.find("room.active.count")
                .tag("state", RoomState.READY.name())
                .gauge();
        Gauge totalGauge = meterRegistry.find("room.total.count").gauge();

        // then
        assertThat(readyGauge.value()).isEqualTo(2.0);
        assertThat(totalGauge.value()).isEqualTo(2.0);
    }

    @Test
    void Room_삭제_시_Gauge가_감소한다() {
        // given
        JoinCode joinCode = new JoinCode("ABC4");
        Room room = Room.createNewRoom(joinCode, new PlayerName("host1"), null, 0.7);
        memoryRoomRepository.save(room);

        // when
        memoryRoomRepository.deleteByJoinCode(joinCode);

        // then
        Gauge readyGauge = meterRegistry.find("room.active.count")
                .tag("state", RoomState.READY.name())
                .gauge();
        Gauge totalGauge = meterRegistry.find("room.total.count").gauge();

        assertThat(readyGauge.value()).isEqualTo(0.0);
        assertThat(totalGauge.value()).isEqualTo(0.0);
    }
}
