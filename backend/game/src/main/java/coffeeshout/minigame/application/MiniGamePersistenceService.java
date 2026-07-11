package coffeeshout.minigame.application;

import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.RoomSnapshotQuery;
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
    private final RoomSnapshotQuery roomSnapshotQuery;
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

        // 방 PLAYING 전이는 :room이 GameSessionStartedEvent 수신 시 수행한다(ADR-0034 — 상태전이 이관).
        // :game은 자기 미니게임 엔티티만 저장하며, room_session id는 :room이 구현한 포트로 얻는다.
        final Long roomSessionId = roomSnapshotQuery.resolveRoomSessionId(event.joinCode());
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
