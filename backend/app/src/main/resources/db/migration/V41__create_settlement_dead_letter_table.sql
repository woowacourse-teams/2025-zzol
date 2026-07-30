-- 정산 DLQ 테이블 (#1612 리뷰 반영)
--
-- 격리 메시지는 재처리가 아니라 장기 보존·사후 분석이 목적이다. Redis 스트림에 두면
-- 메모리를 점유하고 eviction 정책에 따라 유실될 수 있어, outbox의 DEAD_LETTER와 같이
-- RDBMS에 보관한다.

CREATE TABLE settlement_dead_letter (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    record_id  VARCHAR(64)  NOT NULL,
    reason     VARCHAR(500) NOT NULL,
    payload    TEXT,
    created_at TIMESTAMP(6) NOT NULL,
    -- DLQ 기록 후 ACK 실패로 같은 메시지가 재전달되어도 격리 행은 하나만 남는다(멱등)
    UNIQUE KEY uk_settlement_dead_letter_record (record_id)
);
