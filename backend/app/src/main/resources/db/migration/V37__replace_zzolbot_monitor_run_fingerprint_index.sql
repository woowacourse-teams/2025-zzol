-- firing 재배달 멱등 가드(fingerprint = ? AND notified = true AND created_at > ?)를 위한 복합 인덱스로 교체한다.
-- 기존 단일 fingerprint 인덱스는 이 복합 인덱스의 leftmost prefix라 중복이므로 함께 제거한다.
ALTER TABLE zzolbot_monitor_run
    DROP INDEX idx_zzolbot_monitor_run_fingerprint,
    ADD INDEX idx_zzolbot_monitor_run_cooldown (fingerprint, notified, created_at DESC);
