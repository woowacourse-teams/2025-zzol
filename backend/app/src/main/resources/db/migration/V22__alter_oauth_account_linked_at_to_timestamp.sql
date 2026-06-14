-- oauth_account.linked_at: DATETIME → TIMESTAMP(6) (Instant 타입 매핑 일관성)
ALTER TABLE oauth_account
    MODIFY COLUMN linked_at TIMESTAMP(6) NOT NULL;
