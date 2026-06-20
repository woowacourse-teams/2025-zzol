package coffeeshout.minigame.event;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import coffeeshout.gamecommon.PlayerRef;
import coffeeshout.gamecommon.RoomReferencePort;
import coffeeshout.global.lock.RedisLock;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.minigame.infra.persistence.MiniGameResultEntity;
import coffeeshout.minigame.infra.persistence.MiniGameResultJpaRepository;
import coffeeshout.user.application.service.UserStatsService;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameResultSaveEventListener {

    private final RoomReferencePort roomReferencePort;
    private final MiniGameJpaRepository miniGameJpaRepository;
    private final MiniGameResultJpaRepository miniGameResultJpaRepository;
    private final GameSessionService gameSessionService;
    private final UserStatsService userStatsService;

    // 확률 조정 리스너(MiniGameResultRoomListener, @Order(1)) 이후에 실행한다 —
    // 저장 실패(@RedisLock 경합/DB 오류)가 확률 조정·SCORE_BOARD 전이를 막지 않도록(ADR-0025 결정 5).
    @EventListener
    @Order(2)
    @Transactional
    @RedisLock(
            key = "#event.eventId()",
            lockPrefix = "minigame:result:lock:",
            donePrefix = "minigame:result:done:",
            waitTime = 0,
            leaseTime = 5000
    )
    public void handle(MiniGameFinishedEvent event) {
        final Long roomSessionId = roomReferencePort.findCurrentRoomSessionId(event.joinCode())
                .orElseThrow(() -> new IllegalArgumentException("방이 존재하지 않습니다: " + event.joinCode()));
        final MiniGameType miniGameType = MiniGameType.valueOf(event.miniGameType());

        final MiniGameEntity miniGameEntity = miniGameJpaRepository
                .findByRoomSessionIdAndMiniGameType(roomSessionId, miniGameType)
                .orElseThrow(() -> new IllegalArgumentException("미니게임 엔티티가 존재하지 않습니다: " + event.joinCode()));

        final Playable miniGame = gameSessionService.getSession(new JoinCode(event.joinCode()))
                .findCompletedGame(miniGameType);

        final MiniGameResult result = miniGame.getResult();
        final Map<Gamer, MiniGameScore> scores = miniGame.getScores();

        final List<String> playerNames = scores.keySet().stream()
                .map(Gamer::getName)
                .toList();

        final Map<String, PlayerRef> playerRefMap = roomReferencePort
                .findPlayerRefs(roomSessionId, playerNames)
                .stream()
                .collect(Collectors.toMap(
                        PlayerRef::name,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        final List<MiniGameResultEntity> resultEntities = new ArrayList<>();
        // PlayerEntity 참조가 사라져 저장 후 엔티티에서 userId를 다시 읽을 수 없으므로,
        // 결과 조립 시점에 PlayerRef.userId를 캡처해 stats 갱신 목록을 함께 만든다.
        final List<StatUpdate> statUpdates = new ArrayList<>();

        for (Map.Entry<Gamer, MiniGameScore> entry : scores.entrySet()) {
            final Gamer gamer = entry.getKey();
            final PlayerRef playerRef = playerRefMap.get(gamer.getName());
            if (playerRef == null) {
                throw new IllegalArgumentException("플레이어가 존재하지 않습니다: " + gamer.getName());
            }

            final Integer rank = result.getPlayerRank(gamer);
            final Long score = entry.getValue().getValue();

            resultEntities.add(new MiniGameResultEntity(miniGameEntity, playerRef.id(), rank, score));
            if (playerRef.userId() != null) {
                statUpdates.add(new StatUpdate(playerRef.userId(), rank == 1));
            }
        }

        miniGameResultJpaRepository.bulkInsert(resultEntities);

        statUpdates.forEach(statUpdate -> userStatsService.updateStats(statUpdate.userId(), statUpdate.winner()));

        log.info("미니게임 결과 벌크 저장 완료: joinCode={}, playerCount={}", event.joinCode(), resultEntities.size());
    }

    private record StatUpdate(Long userId, boolean winner) {
    }
}
