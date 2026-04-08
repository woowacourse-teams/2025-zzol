CREATE TABLE report (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    category   VARCHAR(20)  NOT NULL,
    game_type  VARCHAR(20),
    join_code  VARCHAR(10),
    content    VARCHAR(200) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
