-- #1829: AI가 CLEAN으로 통과시킨 닉네임 일부를 운영자 검토 표본으로 표시한다.
-- 표본 중 운영자가 차단한 비율로 CLEAN 전체의 미탐률을 추정한다. 표본 목록 조회와 품질 집계는 기존 인덱스
-- idx_nickname_audit_status_audited_created (status, audited_at, created_at)를 타고 review_sample은 거기서 거른다.
ALTER TABLE player_name_audit
    ADD COLUMN review_sample BOOLEAN NOT NULL DEFAULT FALSE;

-- 오탐·미탐은 이제 감사 행의 review_sample과 상태로 센다. 피드백의 ai_flagged는 더 읽는 곳이 없지만 여기서 지우지 않는다.
-- 블루그린 전환 중 옛 컨테이너가 마이그레이션 뒤에도 최대 300초 동안 요청을 받고, 옛 이미지로 롤백하면
-- ddl-auto: validate가 칼럼이 없다며 기동을 막는다. 새 엔티티는 이 칼럼을 모르므로 INSERT가 통과하게 기본값만 준다.
-- DROP COLUMN은 옛 이미지로 돌아갈 일이 없어진 다음 릴리스에서 한다.
ALTER TABLE player_name_feedback
    ALTER COLUMN ai_flagged SET DEFAULT TRUE;
