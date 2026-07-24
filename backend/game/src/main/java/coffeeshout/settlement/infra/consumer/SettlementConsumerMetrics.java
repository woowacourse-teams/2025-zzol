package coffeeshout.settlement.infra.consumer;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.StreamInfo.XInfoGroup;
import org.springframework.data.redis.connection.stream.StreamInfo.XInfoGroups;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 정산 컨슈머 그룹 메트릭. 브로드캐스트 스트림들은 컨슈머 그룹이 없어 XLEN과 스레드풀 큐
 * 깊이로 lag을 근사하지만({@code RedisStreamLagMetricService}), 이 스트림은 그룹이 있으므로
 * {@code XINFO GROUPS}의 <b>진짜 미처리 상태</b>(pending·컨슈머 수)를 직접 읽는다(#1610).
 * <ul>
 *   <li>redis.stream.group.pending — 전달됐지만 ACK되지 않은 메시지 수. 0이 정상이고,
 *       지속 증가는 컨슈머 사망 또는 반복 실패의 신호다.</li>
 *   <li>redis.stream.group.consumers — 그룹에 등록된 컨슈머 수. blue/green 상태에 따라 1~2.</li>
 *   <li>redis.stream.length(dlq) — 격리된 메시지 수. 0이 아니면 사람이 봐야 한다.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementConsumerMetrics {

    private final StringRedisTemplate stringRedisTemplate;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void initializeMetrics() {
        Gauge.builder("redis.stream.group.pending", () -> readGroupInfo(GroupField.PENDING))
                .description("정산 컨슈머 그룹의 미ACK(pending) 메시지 수")
                .tag("stream", SettlementStreamConsumer.STREAM_KEY)
                .tag("group", SettlementStreamConsumer.GROUP)
                .register(meterRegistry);

        Gauge.builder("redis.stream.group.consumers", () -> readGroupInfo(GroupField.CONSUMERS))
                .description("정산 컨슈머 그룹에 등록된 컨슈머 수")
                .tag("stream", SettlementStreamConsumer.STREAM_KEY)
                .tag("group", SettlementStreamConsumer.GROUP)
                .register(meterRegistry);

        Gauge.builder("redis.stream.length", this::readDlqLength)
                .description("Redis Stream의 현재 메시지 수 (XLEN)")
                .tag("stream", SettlementDeadLetterPublisher.DLQ_KEY)
                .register(meterRegistry);
    }

    private double readGroupInfo(GroupField field) {
        try {
            final XInfoGroups groups = stringRedisTemplate.opsForStream()
                    .groups(SettlementStreamConsumer.STREAM_KEY);
            return groups.stream()
                    .filter(group -> SettlementStreamConsumer.GROUP.equals(group.groupName()))
                    .findFirst()
                    .map(group -> field == GroupField.PENDING
                            ? (double) group.pendingCount()
                            : (double) group.consumerCount())
                    .orElse(Double.NaN);
        } catch (Exception e) {
            // 스트림·그룹 미생성(기동 직후) 또는 Redis 장애 — 게이지는 NaN으로 표시한다
            log.debug("정산 컨슈머 그룹 정보 조회 실패: {}", e.getMessage());
            return Double.NaN;
        }
    }

    private double readDlqLength() {
        try {
            final Long length = stringRedisTemplate.opsForStream().size(SettlementDeadLetterPublisher.DLQ_KEY);
            return length != null ? length.doubleValue() : 0.0;
        } catch (Exception e) {
            log.debug("정산 DLQ 길이 조회 실패: {}", e.getMessage());
            return Double.NaN;
        }
    }

    private enum GroupField {
        PENDING, CONSUMERS
    }
}
