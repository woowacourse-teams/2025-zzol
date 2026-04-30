-- 회원 테이블 (MySQL 예약어 user 회피 → app_user)
CREATE TABLE app_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_code CHAR(5) NOT NULL,
    nickname VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_app_user_user_code (user_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- OAuth 계정 연동 테이블
CREATE TABLE oauth_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    linked_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_oauth_provider_user (provider, provider_user_id),
    CONSTRAINT fk_oauth_account_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 기존 player 테이블에 회원 식별자 추가 (NULLABLE: 익명 참여 허용)
ALTER TABLE player
    ADD COLUMN user_id BIGINT NULL,
    ADD CONSTRAINT fk_player_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE SET NULL;

-- 기존 report 테이블에 회원 식별자 추가 (NULLABLE: 익명 신고 허용)
ALTER TABLE report
    ADD COLUMN user_id BIGINT NULL,
    ADD COLUMN user_code CHAR(5) NULL,
    ADD CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES app_user(id) ON DELETE SET NULL;
