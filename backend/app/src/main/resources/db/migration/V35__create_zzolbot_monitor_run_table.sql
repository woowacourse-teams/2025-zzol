CREATE TABLE zzolbot_monitor_run (
    id                     BIGINT      NOT NULL AUTO_INCREMENT,
    collected_at           DATETIME(6) NOT NULL,
    anomalous              TINYINT(1)  NOT NULL,
    severity               VARCHAR(20) NOT NULL,
    signals_json           TEXT        NOT NULL,
    fingerprint            VARCHAR(200) DEFAULT NULL,
    analysis_summary       TEXT         DEFAULT NULL,
    suggested_actions_json TEXT         DEFAULT NULL,
    notified               TINYINT(1)  NOT NULL,
    created_at             DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_zzolbot_monitor_run_created_at (created_at DESC),
    INDEX idx_zzolbot_monitor_run_fingerprint (fingerprint)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
