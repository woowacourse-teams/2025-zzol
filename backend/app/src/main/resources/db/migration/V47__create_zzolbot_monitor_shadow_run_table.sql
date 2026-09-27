CREATE TABLE zzolbot_monitor_shadow_run
(
    id                           BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_at                   TIMESTAMP(6) NOT NULL,
    fingerprint                  VARCHAR(200) NULL,
    alertname                    VARCHAR(200) NULL,
    log_sample_count             INT          NOT NULL,
    authoritative_evidence_found BOOLEAN      NOT NULL COMMENT '권위 모델 판정(인용 검증 후)',
    authoritative_summary        TEXT         NULL,
    shadow_evidence_found        BOOLEAN      NOT NULL COMMENT '자체 모델 판정(인용 검증 후)',
    shadow_claimed_evidence      BOOLEAN      NOT NULL COMMENT '자체 모델 원 주장(인용 검증 전)',
    shadow_summary               TEXT         NULL,
    shadow_root_cause            TEXT         NULL,
    shadow_evidence_line         TEXT         NULL,
    shadow_latency_ms            BIGINT       NOT NULL,
    shadow_failed                BOOLEAN      NOT NULL DEFAULT FALSE,
    shadow_error                 VARCHAR(500) NULL,
    INDEX idx_zzolbot_shadow_run_created_at (created_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '알림별 권위 모델과 자체 모델 분석 비교 기록(관측 전용, 운영 판정 미사용)';
