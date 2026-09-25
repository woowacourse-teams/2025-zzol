-- #1829: AI가 CLEAN으로 통과시킨 닉네임 일부를 운영자 검토 표본으로 표시한다.
-- 표본 중 운영자가 차단한 비율로 CLEAN 전체의 미탐률을 추정한다. 표본 목록 조회와 품질 집계는 기존 인덱스
-- idx_nickname_audit_status_audited_created (status, audited_at, created_at)를 타고 review_sample은 거기서 거른다.
ALTER TABLE player_name_audit
    ADD COLUMN review_sample BOOLEAN NOT NULL DEFAULT FALSE;

-- 오탐·미탐은 이제 감사 행의 review_sample과 상태로 센다. 피드백의 ai_flagged는 허용·차단 경로가 늘 true로
-- 박아 넣어 미탐을 한 번도 세지 못했고, 더 읽는 곳이 없다.
ALTER TABLE player_name_feedback
    DROP COLUMN ai_flagged;
