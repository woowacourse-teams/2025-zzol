-- 자동 수정 봇(zzolbot remediation)이 한 모니터링 실행에서 시작한 수정 시도의 이력.
-- 앱이 GitHub Actions 워커로 작업을 넘긴 순간 DISPATCHED로 저장되고, 워커의 내부 콜백이
-- PR_OPENED(+pr_url)·NO_FIX·FAILED로 갱신한다. 게이트에서 막힌 건(코드 결함 아님·예산·쿨다운)은
-- 시도를 만들지 않으므로 이 테이블에는 실제로 워커에 넘긴 시도만 쌓인다.
CREATE TABLE zzolbot_remediation_attempt (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    monitor_run_id BIGINT       NOT NULL,
    fingerprint    VARCHAR(200) DEFAULT NULL,
    defect_type    VARCHAR(40)  NOT NULL,
    status         VARCHAR(20)  NOT NULL,
    pr_url         VARCHAR(500) DEFAULT NULL,
    pr_number      INT          DEFAULT NULL,
    branch_name    VARCHAR(200) DEFAULT NULL,
    detail         TEXT         DEFAULT NULL,
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_zzolbot_remediation_attempt_run (monitor_run_id, created_at DESC),
    INDEX idx_zzolbot_remediation_attempt_cooldown (fingerprint, created_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
