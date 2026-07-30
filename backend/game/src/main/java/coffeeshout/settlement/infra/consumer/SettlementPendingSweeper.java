package coffeeshout.settlement.infra.consumer;

import coffeeshout.settlement.infra.consumer.SettlementMessageProcessor.PoisonMessageException;
import java.time.Duration;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * PEL(pending) 회수자. ACK 없이 죽은 컨슈머의 메시지를 다른 인스턴스가 이어받는 경로로,
 * 컨슈머 그룹의 처리 보장을 완성하는 두 번째 절반이다(#1610).
 * <p>
 * blue/green 교체로 사라진 인스턴스(예: 종료된 green)의 미처리 메시지가 여기서 회수된다.
 * XCLAIM은 전달 횟수를 증가시키므로, 계속 실패하는 메시지는 회수를 반복하다
 * MAX_DELIVERIES에 도달해 DLQ로 격리된다 — 무한 재처리 루프가 구조적으로 끊긴다.
 */
@Slf4j
@Component
public class SettlementPendingSweeper {

    // 정상 처리(수 초)보다 충분히 길게 — 살아있는 컨슈머가 처리 중인 메시지를 가로채지 않는다
    static final Duration MIN_IDLE = Duration.ofSeconds(60);
    static final long MAX_DELIVERIES = 5;
    private static final int SWEEP_BATCH = 50;

    private final StringRedisTemplate stringRedisTemplate;
    private final SettlementMessageProcessor processor;
    private final SettlementDeadLetterPublisher deadLetterPublisher;
    private final String consumerName;

    public SettlementPendingSweeper(
            StringRedisTemplate stringRedisTemplate,
            SettlementMessageProcessor processor,
            SettlementDeadLetterPublisher deadLetterPublisher,
            @Qualifier("settlementConsumerName") String consumerName
    ) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.processor = processor;
        this.deadLetterPublisher = deadLetterPublisher;
        this.consumerName = consumerName;
    }

    // 공유 스케줄러 스레드를 오래 붙잡지 않도록 배치 크기를 제한한다(ADR-0032의 폴링 기아 교훈).
    // 회수 처리 자체는 짧고(정산 1건 수 ms), 실패 메시지는 다음 주기로 미룬다.
    @Scheduled(fixedDelay = 30_000, initialDelay = 60_000)
    public void sweep() {
        final PendingMessages pending = stringRedisTemplate.opsForStream().pending(
                SettlementStreamConsumer.STREAM_KEY,
                SettlementStreamConsumer.GROUP,
                Range.unbounded(),
                SWEEP_BATCH
        );
        if (pending == null || pending.isEmpty()) {
            return;
        }

        for (PendingMessage message : pending) {
            if (message.getElapsedTimeSinceLastDelivery().compareTo(MIN_IDLE) < 0) {
                continue;
            }
            reclaim(message);
        }
    }

    private void reclaim(PendingMessage message) {
        // XCLAIM min-idle 조건부라 두 인스턴스의 스위퍼가 동시에 돌아도 한쪽만 소유권을 가져간다
        final List<MapRecord<String, String, String>> claimed = claim(message.getId());
        if (claimed.isEmpty()) {
            return;
        }

        for (MapRecord<String, String, String> record : claimed) {
            // totalDeliveryCount는 XCLAIM 이전까지의 전달 횟수다. idle 60초를 넘겨 pending에
            // 남았다는 것 자체가 직전 전달의 처리 실패를 뜻하므로, N이면 이미 N회 시도가
            // 소진된 상태다 — MAX_DELIVERIES회 시도 후 격리라는 계약에 >=가 부합한다.
            if (message.getTotalDeliveryCount() >= MAX_DELIVERIES) {
                deadLetterPublisher.publish(record,
                        "최대 재전달 횟수 소진: deliveries=" + message.getTotalDeliveryCount());
                acknowledge(record.getId());
                continue;
            }
            try {
                processor.process(record);
                acknowledge(record.getId());
                log.info("pending 정산 메시지 회수 처리 완료: recordId={}, deliveries={}",
                        record.getId(), message.getTotalDeliveryCount());
            } catch (PoisonMessageException e) {
                deadLetterPublisher.publish(record, e.getMessage());
                acknowledge(record.getId());
            } catch (Exception e) {
                // ACK하지 않는다 — 다음 스위프에서 전달 횟수가 누적되어 결국 DLQ로 수렴한다
                log.error("pending 정산 메시지 재처리 실패: recordId={}", record.getId(), e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<MapRecord<String, String, String>> claim(RecordId recordId) {
        return stringRedisTemplate.opsForStream()
                .claim(
                        SettlementStreamConsumer.STREAM_KEY,
                        SettlementStreamConsumer.GROUP,
                        consumerName,
                        org.springframework.data.redis.connection.RedisStreamCommands.XClaimOptions
                                .minIdle(MIN_IDLE)
                                .ids(recordId)
                )
                .stream()
                .map(record -> (MapRecord<String, String, String>) (MapRecord<String, ?, ?>) record)
                .toList();
    }

    private void acknowledge(RecordId recordId) {
        stringRedisTemplate.opsForStream()
                .acknowledge(SettlementStreamConsumer.STREAM_KEY, SettlementStreamConsumer.GROUP, recordId);
    }
}
