-- 이메일 암호화 저장 대비
-- email: 평문(varchar 255) → AES-256-GCM 암호문(Base64). 길이 확대.
ALTER TABLE oauth_account MODIFY email VARCHAR(512);

-- email_hash: 검색용 블라인드 인덱스(HMAC-SHA256 hex 64자). 동등 조회 전용.
-- 이메일은 provider 간 중복 가능하므로 non-unique 인덱스.
ALTER TABLE oauth_account ADD COLUMN email_hash VARCHAR(64) NULL;
CREATE INDEX idx_oauth_email_hash ON oauth_account (email_hash);
