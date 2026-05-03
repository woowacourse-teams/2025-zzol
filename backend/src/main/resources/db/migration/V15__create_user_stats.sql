CREATE TABLE user_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    win_count INT NOT NULL DEFAULT 0,
    survival_streak INT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_user_stats_user_id (user_id),
    CONSTRAINT fk_user_stats_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
