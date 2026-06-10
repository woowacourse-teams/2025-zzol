package coffeeshout.minigame.application;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import coffeeshout.minigame.domain.MiniGameService;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.GameSessionStartedEvent;
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
     * 발행 측으로 전파된다.
     *
     * <p><b>전이 순서가 불변식이다.</b> {@code startGame}으로 GameSession을 {@code PLAYING}으로 전이한 <b>직후</b>,
     * 실패 가능 I/O({@code miniGameService.start}·결과 영속)보다 <b>먼저</b> {@code GameSessionStartedEvent}를 발행해
     * 방을 {@code markPlaying} 시킨다(:room 동기 리스너). 이로써 두 상태 전이가 한 묶음으로 끝나, 이후 I/O가 실패해도
     * GameSession·Room이 모두 PLAYING으로 일관되게 남는다(찢어진 상태 방지). {@code startGame} 자체가 실패하면
     * 전이 이벤트가 발행되지 않아 둘 다 READY로 남고 재전송으로 복구된다. 플레이어 명단은 방이 조회해 이벤트에
     * 실어 주므로 더 이상 {@code RoomQueryService}를 호출하지 않는다.
     */
    @EventListener
    public void onGameStartReady(GameStartReadyEvent event) {
        log.info("게임 시작 준비 이벤트 수신: eventId={}, joinCode={}, hostName={}",
                event.eventId(), event.joinCode(), event.hostName());

        final JoinCode joinCode = new JoinCode(event.joinCode());
        final Playable playable = gameSessionService.startGame(
                joinCode, Gamer.guest(event.hostName()), event.gamers());

        // GameSession PLAYING 전이 직후, 실패 가능 I/O보다 먼저 Room도 PLAYING으로 동기화한다(:room 리스너)
        eventPublisher.publishEvent(new GameSessionStartedEvent(event.joinCode()));

        final MiniGameType miniGameType = playable.getMiniGameType();

        final MiniGameService miniGameService = Optional.ofNullable(miniGameServiceMap.get(miniGameType))
                .orElseThrow(() -> new IllegalStateException("미니게임 서비스가 등록되지 않았습니다: " + miniGameType));
        miniGameService.start(event.joinCode(), event.hostName());

        miniGamePersistenceService.saveGameEntities(event, miniGameType);

        // 모든 작업 성공 후 사용자 대상 시작 브로드캐스트 발행
        eventPublisher.publishEvent(new MiniGameStartedEvent(event.joinCode(), miniGameType.name()));
        log.info("JoinCode[{}] 미니게임 시작됨 - MiniGameType : {}", event.joinCode(), miniGameType);
    }
}
