-- 관리자 허용목록의 DB 절반.
-- 나머지 절반은 환경변수 ADMIN_EMAILS(부트스트랩)이며, 최종 판정은 두 집합의 합집합이다.
-- 부트스트랩 계정을 DB에 넣지 않는 이유는 UI로 지울 수 없어야 하기 때문이다(전원 잠금 방지).
CREATE TABLE admin_account
(
    id               BIGINT       NOT NULL AUTO_INCREMENT,
    email            VARCHAR(255) NOT NULL,
    created_by_email VARCHAR(255) DEFAULT NULL,
    created_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_admin_account_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
