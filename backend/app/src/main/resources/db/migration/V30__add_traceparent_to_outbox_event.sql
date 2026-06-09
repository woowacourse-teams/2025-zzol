-- W3C traceparent 헤더를 Outbox 기록 시점에 저장한다.
-- 재시도 릴레이는 스케줄러 스레드라 원본 트레이스 컨텍스트가 없으므로 기록 시점 캡처가 필요하다.
ALTER TABLE outbox_event ADD COLUMN traceparent VARCHAR(64) NULL;
