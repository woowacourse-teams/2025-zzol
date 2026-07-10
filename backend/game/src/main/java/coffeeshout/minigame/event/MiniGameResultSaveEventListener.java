package coffeeshout.minigame.event;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import coffeeshout.gamecommon.RoomSnapshotQuery;
import coffeeshout.gamecommon.RoomSnapshotQuery.PlayerSnapshot;
import coffeeshout.global.lock.RedisLock;
import coffeeshout.minigame.application.GameSessionService;
import coffeeshout.minigame.domain.MiniGameResult;
import coffeeshout.minigame.domain.MiniGameScore;
import coffeeshout.minigame.domain.MiniGameType;
import coffeeshout.minigame.event.dto.MiniGameFinishedEvent;
import coffeeshout.minigame.event.dto.MiniGameStatsRecordedEvent;
import coffeeshout.minigame.event.dto.MiniGameStatsRecordedEvent.PlayerStat;
import coffeeshout.minigame.infra.persistence.MiniGameEntity;
import coffeeshout.minigame.infra.persistence.MiniGameJpaRepository;
import coffeeshout.minigame.infra.persistence.MiniGameResultEntity;
import coffeeshout.minigame.infra.persistence.MiniGameResultJpaRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MiniGameResultSaveEventListener {

    private final RoomSnapshotQuery roomSnapshotQuery;
    private final MiniGameJpaRepository miniGameJpaRepository;
    private final MiniGameResultJpaRepository miniGameResultJpaRepository;
    private final GameSessionService gameSessionService;
    private final ApplicationEventPublisher eventPublisher;

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
        // 방·플레이어 id는 :room이 구현한 포트로 얻는다(ADR-0034 — :game→:room 의존 제거).
        final long roomSessionId = roomSnapshotQuery.resolveRoomSessionId(event.joinCode());
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

        final Map<String, PlayerSnapshot> snapshotMap = roomSnapshotQuery
                .resolvePlayers(roomSessionId, playerNames)
                .stream()
                .collect(Collectors.toMap(
                        PlayerSnapshot::playerName,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));

        final List<MiniGameResultEntity> resultEntities = new ArrayList<>();
        // 회원 플레이어의 승패는 이벤트로 발행 → :user가 구독해 통계 갱신(#1547). 게스트(userId null)는 제외.
        final List<PlayerStat> playerStats = new ArrayList<>();

        for (Map.Entry<Gamer, MiniGameScore> entry : scores.entrySet()) {
            final Gamer gamer = entry.getKey();
            final PlayerSnapshot snapshot = snapshotMap.get(gamer.getName());
            if (snapshot == null) {
                throw new IllegalArgumentException("플레이어가 존재하지 않습니다: " + gamer.getName());
            }

            final Integer rank = result.getPlayerRank(gamer);
            final Long score = entry.getValue().getValue();

            resultEntities.add(new MiniGameResultEntity(miniGameEntity, snapshot.playerId(), rank, score));

            if (snapshot.userId() != null) {
                playerStats.add(new PlayerStat(snapshot.userId(), rank == 1));
            }
        }

        miniGameResultJpaRepository.bulkInsert(resultEntities);

        // 동기 발행이므로 현 트랜잭션 안에서 실행되어 기존 롤백 의미를 보존한다.
        if (!playerStats.isEmpty()) {
            eventPublisher.publishEvent(new MiniGameStatsRecordedEvent(playerStats));
        }

        log.info("미니게임 결과 벌크 저장 완료: joinCode={}, playerCount={}", event.joinCode(), resultEntities.size());
    }
}
