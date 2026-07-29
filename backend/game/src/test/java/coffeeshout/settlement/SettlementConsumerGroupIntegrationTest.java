package coffeeshout.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import coffeeshout.GameModuleIntegrationTest;
import coffeeshout.global.redis.stream.StreamPublisher;
import coffeeshout.settlement.domain.SeasonKey;
import coffeeshout.settlement.event.SettlementResultEvent;
import coffeeshout.settlement.event.SettlementResultEvent.PlayerResult;
import coffeeshout.settlement.infra.SettlementStreamKey;
import coffeeshout.settlement.infra.persistence.SeasonScoreEntity;
import coffeeshout.settlement.infra.persistence.SeasonScoreJpaRepository;
import coffeeshout.settlement.infra.persistence.SeasonSettlementJpaRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * 정산 파이프라인의 임계 와이어링 검증(ADR-0033) — 발행(Outbox 하류인 StreamPublisher)부터
 * 컨슈머 그룹 소비, 멱등 정산, XACK까지 실제 Redis·MySQL로 관통한다.
 */
class SettlementConsumerGroupIntegrationTest extends GameModuleIntegrationTest {

    private static final long ROOM_SESSION_ID = 7777L;

    @Autowired
    private StreamPublisher streamPublisher;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SeasonSettlementJpaRepository settlementRepository;
    @Autowired
    private SeasonScoreJpaRepository scoreRepository;

    @BeforeEach
    void ensureGroupAfterFlush() {
        // 컨테이너 Redis는 테스트마다 flush되어 그룹이 사라진다. 컨슈머의 NOGROUP 자가 복구와
        // 같은 경로지만, 폴링 주기를 기다리지 않도록 결정적으로 재보장한다.
        try {
            final byte[] rawKey = RedisSerializer.string().serialize(SettlementStreamKey.RESULT.getRedisKey());
            stringRedisTemplate.execute(connection -> {
                connection.streamCommands().xGroupCreate(rawKey, "settlement", ReadOffset.from("0-0"), true);
                return null;
            }, true);
        } catch (RedisSystemException e) {
            // BUSYGROUP — 이미 존재
        }
    }

    @Test
    void 발행된_정산_이벤트를_컨슈머_그룹이_소비해_정산하고_ACK한다() {
        // given
        final SettlementResultEvent event = SettlementResultEvent.of(
                "ZZ9X",
                ROOM_SESSION_ID,
                "BLIND_TIMER",
                List.of(new PlayerResult(1L, "한스", 1, 12L), new PlayerResult(2L, "루키", 2, 40L)),
                List.of(1, 2)
        );
        final String seasonKey = SeasonKey.from(event.timestamp()).value();

        // when
        streamPublisher.publish(SettlementStreamKey.RESULT, event);

        // then: 원장과 누적 성적이 반영된다
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertThat(settlementRepository
                    .existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, "BLIND_TIMER", 1L)).isTrue();
            assertThat(settlementRepository
                    .existsByRoomSessionIdAndMiniGameTypeAndUserId(ROOM_SESSION_ID, "BLIND_TIMER", 2L)).isTrue();

            final SeasonScoreEntity first = scoreRepository.findBySeasonKeyAndUserId(seasonKey, 1L).orElseThrow();
            final SeasonScoreEntity second = scoreRepository.findBySeasonKeyAndUserId(seasonKey, 2L).orElseThrow();
            assertThat(first.getTotalPoints()).isEqualTo(100L);
            assertThat(second.getTotalPoints()).isEqualTo(70L);
        });

        // then: 처리 완료가 ACK로 확정된다 — PEL이 비어야 회수 대상이 남지 않은 것이다
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            final var summary = stringRedisTemplate.opsForStream()
                    .pending(SettlementStreamKey.RESULT.getRedisKey(), "settlement");
            assertThat(summary.getTotalPendingMessages()).isZero();
        });
    }
}
