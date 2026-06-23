CREATE TABLE zzolbot_eval_scenario (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(200) NOT NULL,
    question      TEXT         NOT NULL,
    snapshot_json LONGTEXT     NOT NULL,
    rubric        TEXT         NOT NULL,
    source_type   VARCHAR(20)  NOT NULL,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_zzolbot_eval_scenario_name (name),
    INDEX idx_zzolbot_eval_scenario_created_at (created_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE zzolbot_eval_run (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    label          VARCHAR(200) NOT NULL,
    model          VARCHAR(100) NOT NULL,
    prompt_version VARCHAR(100) DEFAULT NULL,
    status         VARCHAR(20)  NOT NULL,
    scenario_count INT          NOT NULL,
    pass_count     INT          NOT NULL DEFAULT 0,
    started_at     DATETIME(6)  NOT NULL,
    finished_at    DATETIME(6)  DEFAULT NULL,
    PRIMARY KEY (id),
    INDEX idx_zzolbot_eval_run_started_at (started_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE TABLE zzolbot_eval_result (
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    run_id             BIGINT      NOT NULL,
    scenario_id        BIGINT      NOT NULL,
    answer             TEXT        NOT NULL,
    accuracy           INT         NOT NULL,
    groundedness       INT         NOT NULL,
    hallucination      BOOLEAN     NOT NULL,
    verdict            VARCHAR(10) NOT NULL,
    rationale          TEXT        NOT NULL,
    latency_ms         BIGINT      NOT NULL,
    missing_tool_calls INT         NOT NULL,
    created_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_zzolbot_eval_result_run (run_id),
    INDEX idx_zzolbot_eval_result_scenario (scenario_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
