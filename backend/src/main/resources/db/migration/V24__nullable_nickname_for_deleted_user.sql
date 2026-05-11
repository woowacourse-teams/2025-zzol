ALTER TABLE app_user MODIFY COLUMN nickname VARCHAR(10) NULL;
UPDATE app_user SET nickname = NULL WHERE deleted_at IS NOT NULL AND nickname IS NOT NULL;
