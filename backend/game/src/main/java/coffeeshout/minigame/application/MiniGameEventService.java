package coffeeshout.minigame.application;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.GameStartReadyEvent;
import coffeeshout.minigame.event.MiniGameStartedEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MiniGameEventService {

    private final Map<MiniGameType, MiniGameService> miniGameServiceMap;
    private final GameSessionService gameSessionService;
    private final ApplicationEventPublisher eventPublisher;
    private final MiniGamePersistenceService miniGamePersistenceService;

    public MiniGameEventService(
            GameSessionService gameSessionService,
            List<MiniGameService> miniGameServices,
            ApplicationEventPublisher eventPublisher,
            MiniGamePersistenceService miniGamePersistenceService
    ) {
        this.gameSessionService = gameSessionService;
        this.eventPublisher = eventPublisher;
        this.miniGamePersistenceService = miniGamePersistenceService;
        this.miniGameServiceMap = new EnumMap<>(MiniGameType.class);
        miniGameServices.forEach(miniGameService -> miniGameServiceMap.put(
                miniGameService.getMiniGameType(),
                miniGameService
        ));
    }

    /**
     * 방 검증을 통과한 시작 요청을 받아 GameSession을 시작한다(ADR-0023 결정 4 — 이벤트 분리).
     * {@code :room}의 {@code MiniGameStartConsumer}가 in-process 동기로 발행하므로, 여기서 던지는 예외는
     * 발행 측으로 전파돼 방 {@code markPlaying} 전이를 건너뛰게 한다(검증 → 시작 → PLAYING 전이의 순서 보존).
     * 플레이어 명단은 방이 조회해 이벤트에 실어 주므로 더 이상 {@code RoomQueryService}를 호출하지 않는다.
     */
    @EventListener
    public void onGameStartReady(GameStartReadyEvent event) {
        log.info("게임 시작 준비 이벤트 수신: eventId={}, joinCode={}, hostName={}",
                event.eventId(), event.joinCode(), event.hostName());

        final JoinCode joinCode = new JoinCode(event.joinCode());
        final Playable playable = gameSessionService.startGame(
                joinCode, Gamer.guest(event.hostName()), event.gamers());

        final MiniGameType miniGameType = playable.getMiniGameType();

        final MiniGameService miniGameService = Optional.ofNullable(miniGameServiceMap.get(miniGameType))
                .orElseThrow(() -> new IllegalStateException("미니게임 서비스가 등록되지 않았습니다: " + miniGameType));
        miniGameService.start(event.joinCode(), event.hostName());

        miniGamePersistenceService.saveGameEntities(event, miniGameType);

        // 모든 작업 성공 후 이벤트 발행
        eventPublisher.publishEvent(new MiniGameStartedEvent(event.joinCode(), miniGameType.name()));
        log.info("JoinCode[{}] 미니게임 시작됨 - MiniGameType : {}", event.joinCode(), miniGameType);
    }
}
