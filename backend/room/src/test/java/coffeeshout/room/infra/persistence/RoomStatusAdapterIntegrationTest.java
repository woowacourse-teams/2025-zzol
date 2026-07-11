package coffeeshout.room.infra.persistence;

import coffeeshout.RoomModuleIntegrationTest;
import coffeeshout.room.application.port.RoomStatusPort;
import coffeeshout.room.domain.RoomState;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * {@link RoomStatusAdapter#updateStatus(String, RoomState)}가 <b>비트랜잭션 호출</b>에서도
 * room_status를 실제 DB에 커밋하는지 검증한다(ADR-0034).
 *
 * <p>이 메서드는 스트림 컨슈머 스레드의 {@code RoomGameStartListener}(주변 트랜잭션 없음)에서 호출된다.
 * 어댑터에 {@code @Transactional}이 없으면 {@code findFirst}가 반환한 엔티티가 detached라 dirty checking
 * flush가 일어나지 않아 전이가 유실된다. 이 테스트가 그 회귀를 막는다.
 */
class RoomStatusAdapterIntegrationTest extends RoomModuleIntegrationTest {

    private static final String JOIN_CODE = "T3P8";

    @Autowired
    private RoomStatusPort roomStatusPort;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Test
    @DisplayName("주변 트랜잭션 없이 updateStatus를 호출해도 room_status가 PLAYING으로 커밋된다")
    void 비트랜잭션_호출도_영속_상태전이가_커밋된다() {
        roomJpaRepository.save(new RoomEntity(JOIN_CODE)); // 기본값 READY로 커밋

        roomStatusPort.updateStatus(JOIN_CODE, RoomState.PLAYING); // 주변 트랜잭션 없음

        final RoomEntity reloaded = roomJpaRepository
                .findFirstByJoinCodeOrderByCreatedAtDesc(JOIN_CODE)
                .orElseThrow();
        Assertions.assertThat(reloaded.getRoomStatus()).isEqualTo(RoomState.PLAYING);
    }
}
