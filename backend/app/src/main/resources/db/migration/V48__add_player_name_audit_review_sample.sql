-- #1829: AI가 CLEAN으로 통과시킨 닉네임 일부를 운영자 검토 표본으로 표시한다.
-- 표본 중 운영자가 차단한 비율로 CLEAN 전체의 미탐률을 추정한다. 표본 목록 조회와 품질 집계는 기존 인덱스
-- idx_nickname_audit_status_audited_created (status, audited_at, created_at)를 타고 review_sample은 거기서 거른다.
ALTER TABLE player_name_audit
    ADD COLUMN review_sample BOOLEAN NOT NULL DEFAULT FALSE;
