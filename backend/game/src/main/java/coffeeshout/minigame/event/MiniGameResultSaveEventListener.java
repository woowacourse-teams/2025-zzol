package coffeeshout.minigame.event;

import coffeeshout.gamecommon.Gamer;
import coffeeshout.gamecommon.JoinCode;
import coffeeshout.gamecommon.Playable;
import coffeeshout.gamecommon.RoomSnapshotQuery;
import coffeeshout.gamecommon.RoomSnapshotQuery.PlayerSnapshot;
import coffeeshout.global.lock.RedisLock;
import coffeeshout.global.outbox.OutboxEventRecorder;
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
import coffeeshout.settlement.event.SettlementResultEvent;
import coffeeshout.settlement.event.SettlementResultEvent.PlayerResult;
import coffeeshout.settlement.infra.SettlementStreamKey;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    // 시즌 정산 대상 게임. 점수가 방 간 절대 비교 가능한 게임만 넣는다 — BLIND_TIMER는 목표
    // 시간과의 오차(ms)라 전역 랭킹이 성립한다. 파이프라인 검증 후 확장한다(#1610).
    private static final Set<MiniGameType> SETTLEMENT_TARGET_GAMES = Set.of(MiniGameType.BLIND_TIMER);

    private final RoomSnapshotQuery roomSnapshotQuery;
    private final MiniGameJpaRepository miniGameJpaRepository;
    private final MiniGameResultJpaRepository miniGameResultJpaRepository;
    private final GameSessionService gameSessionService;
    private final ApplicationEventPublisher eventPublisher;
    private final OutboxEventRecorder outboxEventRecorder;

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
        // 회원 승패는 직접 호출이 아니라 이벤트로 :user에 전달해 통계 갱신한다(#1547).
        final List<PlayerStat> playerStats = new ArrayList<>();
        final List<PlayerResult> settlementResults = new ArrayList<>();

        for (Map.Entry<Gamer, MiniGameScore> entry : scores.entrySet()) {
            final Gamer gamer = entry.getKey();
            final PlayerSnapshot snapshot = snapshotMap.get(gamer.getName());
            if (snapshot == null) {
                throw new IllegalArgumentException("플레이어가 존재하지 않습니다: " + gamer.getName());
            }

            final Integer rank = result.getPlayerRank(gamer);
            final Long score = entry.getValue().getValue();

            resultEntities.add(new MiniGameResultEntity(miniGameEntity, snapshot.playerId(), rank, score));

            if (gamer.getUserId() != null) {
                playerStats.add(new PlayerStat(gamer.getUserId(), rank == 1));
                settlementResults.add(new PlayerResult(gamer.getUserId(), gamer.getName(), rank, score));
            }
        }

        miniGameResultJpaRepository.bulkInsert(resultEntities);

        // 동기 발행이므로 현 트랜잭션 안에서 실행되어 기존 롤백 의미를 보존한다.
        if (!playerStats.isEmpty()) {
            eventPublisher.publishEvent(new MiniGameStatsRecordedEvent(playerStats));
        }

        // 시즌 정산은 Outbox를 경유해 정산 작업 큐 스트림으로 나간다 — 결과 저장과 같은
        // 트랜잭션이라 원자적이고, 소비는 컨슈머 그룹의 단일 처리 경로로 이뤄진다(#1610).
        // 전달은 at-least-once이며 중복 반영은 정산 원장의 멱등 처리가 막는다.
        // 게임 종료 지점들은 스케줄러 스레드(트랜잭션 밖)라 이 리스너가 유일한 훅 위치다.
        if (SETTLEMENT_TARGET_GAMES.contains(miniGameType) && !settlementResults.isEmpty()) {
            outboxEventRecorder.record(
                    SettlementStreamKey.RESULT,
                    SettlementResultEvent.of(event.joinCode(), roomSessionId, miniGameType.name(), settlementResults)
            );
        }

        log.info("미니게임 결과 벌크 저장 완료: joinCode={}, playerCount={}", event.joinCode(), resultEntities.size());
    }
}
