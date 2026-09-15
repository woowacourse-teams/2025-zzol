-- 관리자 조치 감사 로그. append-only.
-- 단일 공유 계정 시절에는 "누가 이 IP를 차단했는가"에 답할 수 없었다.
-- 계정을 이메일 단위로 나눈 것만으로는 부족하고, 조치가 기록으로 남아야 추적이 성립한다.
CREATE TABLE admin_audit_log
(
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    actor_email VARCHAR(255) NOT NULL,
    action      VARCHAR(100) NOT NULL,
    target_type VARCHAR(50)  DEFAULT NULL,
    target_id   VARCHAR(255) DEFAULT NULL,
    detail      TEXT         DEFAULT NULL,
    result      VARCHAR(20)  NOT NULL,
    created_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_admin_audit_log_created_at (created_at DESC),
    INDEX idx_admin_audit_log_actor (actor_email, created_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
