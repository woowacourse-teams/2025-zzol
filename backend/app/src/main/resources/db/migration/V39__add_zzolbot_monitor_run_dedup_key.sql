-- 모니터링 실행 이력에 중복 판정 키(dedup_key) 추가 (#1598)
--
-- 기존에는 Alertmanager fingerprint로 중복 분석을 막았다. 그런데 하나의 인시던트가
-- 서로 다른 알림 2건으로 발화하면(예: MassIpBlockingSpike critical + IpBanRateSpike warning)
-- fingerprint가 달라 가드를 통과해, 같은 장애에 LLM을 두 번 태웠다.
--
-- dedup_key는 알림의 incident_group 라벨(있으면)이고 없으면 fingerprint다. 같은 인시던트를
-- 가리키는 알림들이 하나의 키를 공유하므로 LLM 호출이 인시던트당 1회로 묶인다.
-- fingerprint 컬럼은 개별 알림 식별자로 그대로 남겨 어드민 표시에 쓴다.

ALTER TABLE zzolbot_monitor_run
    ADD COLUMN dedup_key VARCHAR(200) NULL AFTER fingerprint;

-- 기존 행은 fingerprint가 곧 중복 판정 키였다.
UPDATE zzolbot_monitor_run
SET dedup_key = fingerprint
WHERE dedup_key IS NULL;

CREATE INDEX idx_zzolbot_monitor_run_dedup
    ON zzolbot_monitor_run (dedup_key, notified, created_at DESC);

-- V37이 만든 fingerprint 기반 쿨다운 인덱스는 이제 쓰이지 않는다. 쿨다운 조회가
-- dedup_key로 옮겨갔으므로, 쓰기 비용만 남는 인덱스를 제거한다.
DROP INDEX idx_zzolbot_monitor_run_cooldown ON zzolbot_monitor_run;
