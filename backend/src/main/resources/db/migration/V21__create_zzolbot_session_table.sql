CREATE TABLE zzolbot_session (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    question       TEXT         NOT NULL,
    answer         TEXT         NOT NULL,
    feedback       VARCHAR(10)  DEFAULT NULL,
    admin_username VARCHAR(100) NOT NULL,
    created_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_zzolbot_session_created_at (created_at DESC),
    INDEX idx_zzolbot_session_feedback (feedback, created_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
