package coffeeshout.room.domain.repository;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.room.domain.Room;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface RoomRepository {

    Optional<Room> findByJoinCode(JoinCode joinCode);

    /**
     * 주어진 사용자들이 참여 중인 방을 찾는다. 어느 방에도 없는 사용자는 결과에 담기지 않는다.
     *
     * <p>방을 userId로 역인덱싱하지 않고 매번 스캔한다 — 인덱스를 따로 두면 입퇴장 경로마다 동기화가 필요해
     * 누락 시 조용히 틀린 값을 내기 때문이다. 방 목록이 인메모리라 스캔 비용이 무시할 수준이다.
     */
    Map<Long, Room> findAllByUserIds(Collection<Long> userIds);

    boolean existsByJoinCode(JoinCode joinCode);

    Room save(Room room);

    void deleteByJoinCode(JoinCode joinCode);
}
