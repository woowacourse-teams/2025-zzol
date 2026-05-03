CREATE TABLE patch_note
(
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    category   VARCHAR(20)  NOT NULL COMMENT 'NOTICE/EVENT/UPDATE/MAINTENANCE',
    title      VARCHAR(100) NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    INDEX idx_patch_note_created_at (created_at DESC)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT = '운영자가 작성하는 패치노트';
