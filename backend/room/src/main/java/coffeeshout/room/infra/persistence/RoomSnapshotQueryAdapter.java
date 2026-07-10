package coffeeshout.room.infra.persistence;

import coffeeshout.gamecommon.RoomSnapshotQuery;
import coffeeshout.room.application.port.PlayerEntityRepository;
import coffeeshout.room.application.port.RoomEntityRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@code :game}의 게임 결과 영속에 필요한 방·플레이어 id를 공급하는 {@link RoomSnapshotQuery} 구현체(ADR-0034).
 *
 * <p>{@code :game}은 미니게임 엔티티를 {@code Long} FK로 저장하므로 room_session/player의 실제 id가 필요하다.
 * 그 id 소유자인 {@code :room}이 이 어댑터로 공급함으로써 {@code :game}은 {@code :room}을 직접 참조하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class RoomSnapshotQueryAdapter implements RoomSnapshotQuery {

    private final RoomEntityRepository roomEntityRepository;
    private final PlayerEntityRepository playerEntityRepository;

    @Override
    public long resolveRoomSessionId(String joinCode) {
        return roomEntityRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다: " + joinCode))
                .getId();
    }

    @Override
    public List<PlayerSnapshot> resolvePlayers(long roomSessionId, List<String> playerNames) {
        return playerEntityRepository.findByRoomSession_IdAndPlayerNameIn(roomSessionId, playerNames).stream()
                .map(player -> new PlayerSnapshot(player.getPlayerName(), player.getId()))
                .toList();
    }
}
