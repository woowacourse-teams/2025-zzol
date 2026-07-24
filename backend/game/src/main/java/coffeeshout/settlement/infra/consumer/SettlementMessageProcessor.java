package coffeeshout.settlement.infra.consumer;

import coffeeshout.global.redis.BaseEvent;
import coffeeshout.global.redis.EventTypeName;
import coffeeshout.global.redis.stream.StreamRecordFields;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.global.redis.stream.StreamTracePropagator;
import coffeeshout.minigame.infra.MinigameStreamKey;
import coffeeshout.settlement.application.SeasonLeaderboardService;
import coffeeshout.settlement.application.SeasonLeaderboardService.LeaderboardEntry;
import coffeeshout.settlement.application.SettlementService;
import coffeeshout.settlement.application.SettlementService.SettledScore;
import coffeeshout.settlement.event.SeasonRankUpdatedEvent;
import coffeeshout.settlement.event.SeasonRankUpdatedEvent.RankEntry;
import coffeeshout.settlement.event.SettlementResultEvent;
import coffeeshout.settlement.event.SettlementResultEvent.PlayerResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

/**
 * 정산 스트림 레코드 한 건의 처리 본문. 최초 소비(컨슈머)와 pending 회수(스위퍼)가
 * 같은 경로를 쓰도록 분리했다 — 회수된 메시지라고 다른 로직을 타면 안 된다(#1610).
 */
@Slf4j
@Component
public class SettlementMessageProcessor {

    private final ObjectMapper redisObjectMapper;
    private final StreamTracePropagator streamTracePropagator;
    private final SettlementService settlementService;
    private final SeasonLeaderboardService leaderboardService;
    private final StreamPublisher streamPublisher;

    public SettlementMessageProcessor(
            @Qualifier("redisObjectMapper") ObjectMapper redisObjectMapper,
            StreamTracePropagator streamTracePropagator,
            SettlementService settlementService,
            SeasonLeaderboardService leaderboardService,
            StreamPublisher streamPublisher
    ) {
        this.redisObjectMapper = redisObjectMapper;
        this.streamTracePropagator = streamTracePropagator;
        this.settlementService = settlementService;
        this.leaderboardService = leaderboardService;
        this.streamPublisher = streamPublisher;
    }

    /**
     * 정산을 수행한다. DB 정산(트랜잭션)이 커밋된 뒤 리더보드를 절대값으로 갱신하므로,
     * 이 메서드가 어느 지점에서 중단되고 재실행되어도 결과가 부풀지 않는다.
     *
     * @throws PoisonMessageException 파싱 불가·타입 불일치 — 재시도해도 영원히 실패하는 메시지
     * @throws RuntimeException       일시 실패 — ACK하지 않고 재전달로 수렴시킨다
     */
    public void process(MapRecord<String, String, String> record) {
        final Map<String, String> fields = record.getValue();
        final String payload = fields.get(StreamRecordFields.PAYLOAD);
        if (payload == null) {
            throw new PoisonMessageException("payload 필드가 없는 레코드: " + record.getId());
        }

        final SettlementResultEvent event = parse(payload);
        streamTracePropagator.runInConsumerScope(fields, EventTypeName.of(event), () -> {
            final List<SettledScore> settled = settlementService.settle(event);
            settled.forEach(leaderboardService::updateScore);
            notifyRankUpdated(event, settled);
            log.info("시즌 정산 완료: eventId={}, settledCount={}", event.eventId(), settled.size());
        });
    }

    /**
     * 순위 변동을 방으로 되돌려 보낸다 — 작업 큐(정산)에서 브로드캐스트(알림)로의 복귀 지점.
     * 재전달로 정산이 스킵되면(settled 비어 있음) 알림도 내보내지 않아 중복 알림이 없다.
     */
    private void notifyRankUpdated(SettlementResultEvent event, List<SettledScore> settled) {
        if (settled.isEmpty()) {
            return;
        }
        final Map<Long, String> namesByUserId = event.results().stream()
                .collect(Collectors.toMap(PlayerResult::userId, PlayerResult::playerName, (a, b) -> a));

        final String seasonKey = settled.getFirst().seasonKey();
        final List<RankEntry> entries = new ArrayList<>();
        for (SettledScore score : settled) {
            final int seasonRank = leaderboardService.rankOf(seasonKey, score.userId())
                    .map(LeaderboardEntry::rank)
                    .orElse(0);
            entries.add(new RankEntry(
                    namesByUserId.getOrDefault(score.userId(), "unknown"),
                    score.totalPoints(),
                    score.tier().name(),
                    seasonRank
            ));
        }
        streamPublisher.publish(MinigameStreamKey.EVENTS,
                SeasonRankUpdatedEvent.of(event.joinCode(), seasonKey, entries));
    }

    private SettlementResultEvent parse(String payload) {
        final BaseEvent event;
        try {
            event = redisObjectMapper.readValue(payload, BaseEvent.class);
        } catch (JsonProcessingException e) {
            throw new PoisonMessageException("정산 이벤트 파싱 실패: " + e.getMessage(), e);
        }
        if (!(event instanceof SettlementResultEvent settlementEvent)) {
            throw new PoisonMessageException("정산 스트림에 예상 밖 이벤트 타입: " + event.getClass().getSimpleName());
        }
        return settlementEvent;
    }

    /** 재시도해도 영원히 실패하는 메시지 — DLQ로 격리하고 ACK해야 한다. */
    public static class PoisonMessageException extends RuntimeException {
        public PoisonMessageException(String message) {
            super(message);
        }

        public PoisonMessageException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
