package coffeeshout.room.infra.persistence;

import coffeeshout.gamecommon.PlayerRef;
import coffeeshout.gamecommon.RoomReferencePort;
import coffeeshout.room.domain.RoomState;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * {@link RoomReferencePort}의 {@code :room} 구현. {@code :game} 영속 계층이 RoomEntity/PlayerEntity
 * 구체 타입 없이 식별자만으로 동작하도록, "현재 세션" 판정·PLAYING 전이·플레이어 참조 조회를 :room 안에
 * 가둔다(ADR-0025 FK 영속 책임 분리 후속).
 *
 * <p>{@code markRoomPlaying}은 호출자(:game 영속 서비스)의 {@code @Transactional} 안에서 실행돼
 * 더티 체킹으로 flush된다 — {@link RoomStatusAdapter}의 joinCode 기반 전이와 동일한 방식이다.
 */
@Component
@RequiredArgsConstructor
public class RoomReferenceAdapter implements RoomReferencePort {

    private final RoomJpaRepository roomJpaRepository;
    private final PlayerJpaRepository playerJpaRepository;

    @Override
    public Optional<Long> findCurrentRoomSessionId(String joinCode) {
        return roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .map(RoomEntity::getId);
    }

    @Override
    public void markRoomPlaying(String joinCode) {
        roomJpaRepository.findFirstByJoinCodeOrderByCreatedAtDesc(joinCode)
                .ifPresent(roomEntity -> roomEntity.updateRoomStatus(RoomState.PLAYING));
    }

    @Override
    public List<PlayerRef> findPlayerRefs(Long roomSessionId, List<String> playerNames) {
        return playerJpaRepository.findByRoomSessionIdAndPlayerNameIn(roomSessionId, playerNames).stream()
                .map(player -> new PlayerRef(player.getId(), player.getPlayerName(), player.getUserId()))
                .toList();
    }
}
