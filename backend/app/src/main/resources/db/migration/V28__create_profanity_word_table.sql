CREATE TABLE profanity_word (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    word        VARCHAR(200) NOT NULL,
    language    VARCHAR(10)  NOT NULL,
    source      VARCHAR(20)  NOT NULL,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    match_count BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_at  TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_profanity_word (word)
);

-- custom_profanity → profanity_word 데이터 이관
INSERT INTO profanity_word (word, language, source, is_active, match_count, created_at, updated_at)
SELECT
    word,
    'KOREAN',
    CASE source
        WHEN 'AI_AUDIT'       THEN 'AI_FLAGGED'
        WHEN 'OPERATOR_MANUAL' THEN 'MANUAL'
        ELSE 'MANUAL'
    END,
    TRUE,
    0,
    created_at,
    created_at
FROM custom_profanity
ON DUPLICATE KEY UPDATE word = word;
