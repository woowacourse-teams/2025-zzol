package coffeeshout.settlement.infra.consumer;

import coffeeshout.global.redis.stream.StreamRecordFields;
import coffeeshout.settlement.infra.persistence.SettlementDeadLetterEntity;
import coffeeshout.settlement.infra.persistence.SettlementDeadLetterJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

/**
 * 정산 DLQ 기록. 최대 재전달 횟수를 소진했거나 파싱 불가(포이즌)한 메시지를 원본 스트림에서
 * 격리한다 — 실패를 조용히 버리지 않고, 재처리 루프를 무한히 돌리지도 않는다(#1610).
 * <p>
 * 저장소는 Redis 스트림이 아니라 RDBMS다(리뷰 #1612). 격리 메시지는 장기 보존·사후 분석이
 * 목적이라, 메모리를 점유하고 eviction으로 유실될 수 있는 Redis는 맞지 않다.
 * Outbox의 DEAD_LETTER와 같은 철학이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementDeadLetterPublisher {

    private static final int REASON_MAX_LENGTH = 500;

    private final SettlementDeadLetterJpaRepository deadLetterRepository;

    public void publish(MapRecord<String, String, String> record, String reason) {
        try {
            deadLetterRepository.save(new SettlementDeadLetterEntity(
                    record.getId().getValue(),
                    truncate(reason),
                    record.getValue().getOrDefault(StreamRecordFields.PAYLOAD, "")
            ));
        } catch (DataIntegrityViolationException e) {
            // 격리 후 ACK 실패로 재전달된 메시지 — 이미 행이 있으므로 성공으로 간주해야
            // 호출자가 ACK까지 진행해 재전달 루프가 끊긴다(record_id 유니크 제약)
            log.info("이미 격리된 정산 메시지 — 건너뜀: recordId={}", record.getId());
            return;
        }
        log.error("정산 메시지를 DLQ로 이동: recordId={}, reason={}", record.getId(), reason);
    }

    private String truncate(String reason) {
        if (reason == null) {
            return "";
        }
        return reason.length() <= REASON_MAX_LENGTH ? reason : reason.substring(0, REASON_MAX_LENGTH);
    }
}
