-- #1841: V48의 user_id로 내 기록을 조회하는 인덱스. V48과 파일을 나눈 이유는 V48 머리 주석에 있다.
-- V49는 다른 브랜치(#1830)가 쓰고 있어 48과 49 사이의 48.1로 둔다.
-- INPLACE는 테이블을 다시 쓰지 않고 인덱스만 만든다. LOCK=NONE이라 그동안 읽기·쓰기를 막지 않는다.

SET SESSION lock_wait_timeout = 10;

ALTER TABLE mini_game_result
    ADD INDEX idx_mini_game_result_user_type (user_id, mini_game_type),
    ALGORITHM = INPLACE, LOCK = NONE;

SET SESSION lock_wait_timeout = DEFAULT;
