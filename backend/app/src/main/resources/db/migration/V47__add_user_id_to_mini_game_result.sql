-- #1794: 내 기록 조회가 회원의 미니게임 결과를 player 조인 없이 집계하기 위한 비정규화 컬럼.
-- :game은 :room의 player 테이블을 조인하지 않으므로(ADR-0034) user_id를 결과 행에 직접 둔다.
-- FK 제약은 두지 않는다. mini_game_type 비정규화(V4)와 같은 방식이다.

-- 1. 컬럼·인덱스 추가
ALTER TABLE mini_game_result
    ADD COLUMN user_id BIGINT NULL,
    ADD INDEX idx_mini_game_result_user_type (user_id, mini_game_type);

-- 2. 기존 행 백필 (게스트 플레이어는 NULL로 남는다)
UPDATE mini_game_result r
JOIN player p ON p.id = r.player_id
SET r.user_id = p.user_id;
