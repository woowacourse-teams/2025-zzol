package coffeeshout.minigame.application;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.RoomReferencePort;
import coffeeshout.global.lock.RedisLock;
import coffeeshout.minigame.application.port.MiniGameEntityRepository;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.GameStartReadyEvent;
import coffeeshout.minigame.event.PlayerSnapshotRequiredEvent;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MiniGamePersistenceService {

    private final GameSessionService gameSessionService;
    private final RoomReferencePort roomReferencePort;
    private final MiniGameEntityRepository miniGameEntityRepository;
    private final ApplicationEventPublisher eventPublisher;

    @RedisLock(
            key = "#event.eventId()",
            lockPrefix = "event:lock:",
            donePrefix = "event:done:",
            waitTime = 0,
            leaseTime = 5000
    )
    @Transactional
    public void saveGameEntities(GameStartReadyEvent event, MiniGameType miniGameType) {
        final JoinCode roomJoinCode = new JoinCode(event.joinCode());

        // 현재 RoomSession ID로 환원해 FK를 ID 참조로 들고, 방 영속 상태를 PLAYING으로 전이한다(:room이 소유).
        final Long roomSessionId = roomReferencePort.findCurrentRoomSessionId(event.joinCode())
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다: " + event.joinCode()));
        roomReferencePort.markRoomPlaying(event.joinCode());

        final MiniGameEntity miniGameEntity = new MiniGameEntity(roomSessionId, miniGameType);
        miniGameEntityRepository.save(miniGameEntity);

        // 첫 게임 시작 여부는 게임 수 상태를 소유한 GameSession이 판정한다(ADR-0025 Step 5).
        // PlayerEntity 스냅샷 생성은 PlayerType 등 Room 도메인에 접근해야 하므로 :room이 소유한다 —
        // 이벤트를 발행만 하고 PlayerSnapshotListener가 동기 수신해 저장한다(생성 책임 이관).
        if (gameSessionService.getSession(roomJoinCode).isFirstGameStarted()) {
            eventPublisher.publishEvent(new PlayerSnapshotRequiredEvent(event.joinCode()));
        }
    }
}
