package coffeeshout.room.application.event;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.minigame.event.PlayerSnapshotRequiredEvent;
import coffeeshout.room.application.port.PlayerEntityRepository;
import coffeeshout.room.application.port.RoomEntityRepository;
import coffeeshout.room.application.service.RoomQueryService;
import coffeeshout.room.domain.Room;
import coffeeshout.room.infra.persistence.PlayerEntity;
import coffeeshout.room.infra.persistence.RoomEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 첫 게임 시작 시 방 플레이어 스냅샷({@code PlayerEntity})을 영속화한다(ADR-0023 — PlayerEntity 영속 책임 분리).
 *
 * <p>{@code PlayerEntity} 생성은 {@code Player} 도메인의 {@code PlayerType}·{@code userId}에 접근해야 하므로
 * {@code :game}이 아닌 {@code :room}이 소유한다. {@code :game}의 {@code MiniGamePersistenceService}가 발행하는
 * {@link PlayerSnapshotRequiredEvent}를 <b>in-process 동기</b>로 수신하므로, 발행자의
 * {@code @Transactional}+{@code @RedisLock} 안에서 실행된다 — 기존 직접 생성과 동일한 순서·멱등·실패 전파를 보장한다.
 * {@code RoomGameStartListener}·{@code MiniGameResultRoomListener}와 함께 {@code :room}의 게임 생명주기 in-process
 * 리스너다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PlayerSnapshotListener {

    private final RoomQueryService roomQueryService;
    private final RoomEntityRepository roomEntityRepository;
    private final PlayerEntityRepository playerEntityRepository;

    @EventListener
    public void handle(PlayerSnapshotRequiredEvent event) {
        final RoomEntity roomEntity = roomEntityRepository.findFirstByJoinCodeOrderByCreatedAtDesc(event.joinCode())
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다: " + event.joinCode()));
        final Room room = roomQueryService.getByJoinCode(new JoinCode(event.joinCode()));

        room.getPlayers().forEach(player -> playerEntityRepository.save(new PlayerEntity(
                roomEntity,
                player.getName().value(),
                player.getPlayerType(),
                player.getUserId()
        )));

        log.debug("플레이어 스냅샷 저장 완료: joinCode={}, playerCount={}",
                event.joinCode(), room.getPlayers().size());
    }
}
