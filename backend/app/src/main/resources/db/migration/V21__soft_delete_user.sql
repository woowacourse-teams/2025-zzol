-- app_user에 soft delete 타임스탬프 추가
ALTER TABLE app_user
    ADD COLUMN deleted_at TIMESTAMP(6) NULL DEFAULT NULL;

-- oauth_account FK에서 ON DELETE CASCADE 제거 (탈퇴 시 명시적 삭제로 전환)
ALTER TABLE oauth_account
    DROP FOREIGN KEY fk_oauth_account_user;

ALTER TABLE oauth_account
    ADD CONSTRAINT fk_oauth_account_user FOREIGN KEY (user_id) REFERENCES app_user (id);
