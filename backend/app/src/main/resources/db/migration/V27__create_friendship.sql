CREATE TABLE friendship
(
    id           BIGINT      AUTO_INCREMENT PRIMARY KEY,
    requester_id BIGINT      NOT NULL,
    addressee_id BIGINT      NOT NULL,
    status       VARCHAR(20) NOT NULL COMMENT 'PENDING/ACCEPTED',
    created_at   TIMESTAMP(6) NOT NULL,
    updated_at   TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_friendship_requester FOREIGN KEY (requester_id) REFERENCES app_user (id) ON DELETE CASCADE,
    CONSTRAINT fk_friendship_addressee FOREIGN KEY (addressee_id) REFERENCES app_user (id) ON DELETE CASCADE,
    UNIQUE KEY uk_friendship_pair (requester_id, addressee_id),
    INDEX idx_friendship_addressee_status (addressee_id, status),
    INDEX idx_friendship_requester_status (requester_id, status)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '사용자 간 친구 관계';
