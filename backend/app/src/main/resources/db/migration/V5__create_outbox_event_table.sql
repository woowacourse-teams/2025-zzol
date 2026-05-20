CREATE TABLE outbox_event (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    stream_key  VARCHAR(50)     NOT NULL,
    payload     TEXT            NOT NULL,
    status      VARCHAR(20)     NOT NULL,
    retry_count INT             NOT NULL DEFAULT 0,
    created_at  DATETIME(6)     NOT NULL,
    updated_at  DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_outbox_status_id (status, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
