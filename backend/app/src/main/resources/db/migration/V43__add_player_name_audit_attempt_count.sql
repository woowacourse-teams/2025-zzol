-- #1759: 검열 호출이 되풀이 실패하는 행을 큐에서 빼기 위한 시도 횟수와 DEAD_LETTER 상태.
-- 배치 하나가 파싱 실패를 내면 그 배치 행들의 attempt_count가 1 오르고, 상한(nickname-audit.max-attempts,
-- 기본 3)에 닿은 행은 DEAD_LETTER가 된다. DEAD_LETTER는 UNAUDITED가 아니므로 스케줄러 스캔 쿼리
-- (status = 'UNAUDITED' AND audited_at IS NULL)에서 저절로 빠진다. outbox_event의 retry_count와 같은 방식이다.
--
-- 인덱스 idx_nickname_audit_status_audited_created (status, audited_at, created_at)는 그대로 유효하다.
-- 스캔 조건과 정렬 컬럼이 바뀌지 않았고, attempt_count는 필터에 쓰지 않는다.
ALTER TABLE player_name_audit
    ADD COLUMN attempt_count INT NOT NULL DEFAULT 0;

ALTER TABLE player_name_audit
    MODIFY COLUMN status
        ENUM('UNAUDITED', 'FLAGGED', 'PENDING', 'CLEAN', 'ALLOWED', 'BLOCKED', 'DEAD_LETTER')
        NOT NULL DEFAULT 'UNAUDITED';
