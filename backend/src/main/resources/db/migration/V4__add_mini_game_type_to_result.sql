-- mini_game_result 테이블에 mini_game_type 컬럼 추가 (비정규화)
-- 용도: findRacingGameTopPlayers 쿼리에서 mini_game_play JOIN 제거

-- 1. 컬럼 추가
ALTER TABLE mini_game_result ADD COLUMN mini_game_type VARCHAR(20);

-- 2. 기존 데이터 업데이트
UPDATE mini_game_result mr
JOIN mini_game_play mg ON mr.mini_game_play_id = mg.id
SET mr.mini_game_type = mg.mini_game_type;

-- 3. NOT NULL 제약 추가
ALTER TABLE mini_game_result MODIFY COLUMN mini_game_type VARCHAR(20) NOT NULL;

-- 4. 복합 인덱스 추가 (game_type + created_at)
CREATE INDEX idx_mini_game_result_type_created ON mini_game_result (mini_game_type, created_at);
