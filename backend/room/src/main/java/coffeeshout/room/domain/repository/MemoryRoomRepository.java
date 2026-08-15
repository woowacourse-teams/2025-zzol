package coffeeshout.room.domain.repository;

import static org.springframework.util.Assert.notNull;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.room.domain.Room;
import coffeeshout.room.domain.RoomState;
import coffeeshout.room.domain.player.Player;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class MemoryRoomRepository implements RoomRepository {

    private final Map<JoinCode, Room> rooms;

    public MemoryRoomRepository() {
        this.rooms = new ConcurrentHashMap<>();
    }

    @Override
    public Optional<Room> findByJoinCode(JoinCode joinCode) {
        return Optional.ofNullable(rooms.get(joinCode));
    }

    @Override
    public Map<Long, Room> findAllByUserIds(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }

        final Set<Long> targets = Set.copyOf(userIds);
        final Map<Long, Room> roomByUserId = new HashMap<>();
        for (Room room : rooms.values()) {
            // 이 스캔은 방을 소유하지 않은 스레드(HTTP·WS)에서 돈다. Players의 내부 리스트를 그대로 순회하면
            // 다른 스레드의 입퇴장과 겹쳐 깨지므로 먼저 복사해 훑는다. 복사 순간의 경합까지 막지는 못하지만
            // (근본 해결은 Players 리스트 자체의 스레드 안전성), 순회 구간이 사라져 창이 크게 좁아진다.
            for (Player player : List.copyOf(room.getPlayers())) {
                final Long userId = player.getUserId();
                if (userId != null && targets.contains(userId)) {
                    roomByUserId.merge(userId, room, MemoryRoomRepository::preferred);
                }
            }
        }
        return roomByUserId;
    }

    /**
     * 한 사용자가 두 방에 속할 수 있다 — 게임이 끝난 방은 플레이어를 제거하지 않고 방 자체가 지연 삭제되기를
     * 기다린다. 그동안 새 방에 들어가면 둘 다 잡히므로, 맵 순회 순서(비결정적)에 맡기지 않고
     * <b>입장 가능한 방</b>을 우선한다. 둘 다 같으면 참여 코드 사전순으로 고정해 호출마다 답이 흔들리지 않게 한다.
     */
    private static Room preferred(Room left, Room right) {
        if (left.isJoinable() != right.isJoinable()) {
            return left.isJoinable() ? left : right;
        }
        return left.getJoinCode().getValue().compareTo(right.getJoinCode().getValue()) <= 0 ? left : right;
    }

    @Override
    public boolean existsByJoinCode(JoinCode joinCode) {
        return rooms.containsKey(joinCode);
    }

    @Override
    public Room save(Room room) {
        rooms.put(room.getJoinCode(), room);
        return rooms.get(room.getJoinCode());
    }

    @Override
    public void deleteByJoinCode(JoinCode joinCode) {
        notNull(joinCode, "JoinCode는 null일 수 없습니다.");

        rooms.remove(joinCode);
    }

    /**
     * 특정 상태의 Room 수를 반환한다.
     */
    public long countByState(RoomState state) {
        return rooms.values().stream()
                .filter(room -> room.getRoomState() == state)
                .count();
    }

    /**
     * 전체 Room 수를 반환한다.
     */
    public long totalCount() {
        return rooms.size();
    }
}
