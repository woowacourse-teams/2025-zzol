package coffeeshout.settlement.infra.consumer;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 정산 DLQ. 최대 재전달 횟수를 소진했거나 파싱 불가(포이즌)한 메시지를 원본 스트림에서
 * 격리한다. Outbox의 DEAD_LETTER와 같은 철학이다 — 실패를 조용히 버리지 않고,
 * 재처리 루프를 무한히 돌리지도 않는다(#1610).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementDeadLetterPublisher {

    static final String DLQ_KEY = "settlement:result:dlq";

    private final StringRedisTemplate stringRedisTemplate;

    public void publish(MapRecord<String, String, String> record, String reason) {
        final Map<String, String> fields = Map.of(
                "originalId", record.getId().getValue(),
                "reason", reason,
                "payload", record.getValue().getOrDefault("payload", "")
        );
        stringRedisTemplate.opsForStream()
                .add(StreamRecords.newRecord().in(DLQ_KEY).ofMap(fields));
        log.error("정산 메시지를 DLQ로 이동: originalId={}, reason={}", record.getId(), reason);
    }
}
