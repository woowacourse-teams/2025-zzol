-- nickname_audit 테이블명 + 컬럼명 변경
ALTER TABLE nickname_audit RENAME TO player_name_audit;
ALTER TABLE player_name_audit RENAME COLUMN nickname TO player_name;

-- nickname_feedback 테이블명 + 컬럼명 변경
ALTER TABLE nickname_feedback RENAME TO player_name_feedback;
ALTER TABLE player_name_feedback RENAME COLUMN nickname TO player_name;
