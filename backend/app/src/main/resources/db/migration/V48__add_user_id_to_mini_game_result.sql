-- #1794: 내 기록 조회가 회원의 미니게임 결과를 player 조인 없이 집계하기 위한 비정규화 컬럼.
-- :game은 :room의 player 테이블을 조인하지 않으므로(ADR-0034) user_id를 결과 행에 직접 둔다.
-- FK 제약은 두지 않는다. mini_game_type 비정규화(V4)와 같은 방식이다.
--
-- #1841: 이 마이그레이션은 앱 기동 경로에서 돈다. 블루그린 배포의 readiness 150초 안에 끝나야 한다.
--   - 컬럼과 인덱스를 한 ALTER로 묶으면 INSTANT를 못 써 테이블 전체를 다시 쓴다. dev 120만 행에서는 같은 둘을 되돌리는 DROP도 55초 걸렸다.
--     그래서 둘을 나누고 ALGORITHM을 명시한다. 명시한 방식을 쓸 수 없으면 MySQL이 느린 방식으로 바꾸지 않고 에러를 낸다.
--   - 기존 행 백필은 여기서 하지 않는다. 배포 뒤 backend/docs/postmortem/0005-v48-migration-readiness-timeout.md 의 백필 절차로 나눠 실행한다.
--     백필 전까지 과거 결과의 user_id는 NULL이고, 새 결과는 저장 시점에 채워진다.

-- 블루가 이 테이블에 트랜잭션을 열어 두면 ALTER가 메타데이터 락을 기다린다. 기본값(1년) 대신 10초에서 포기한다.
-- 대기 중인 ALTER 뒤로 이 테이블의 모든 쿼리가 줄을 서기 때문이다.
SET SESSION lock_wait_timeout = 10;

ALTER TABLE mini_game_result
    ADD COLUMN user_id BIGINT NULL,
    ALGORITHM = INSTANT;

ALTER TABLE mini_game_result
    ADD INDEX idx_mini_game_result_user_type (user_id, mini_game_type),
    ALGORITHM = INPLACE, LOCK = NONE;

-- Flyway는 앱 커넥션 풀의 연결을 쓴다. 풀로 돌아가기 전에 세션 값을 서버 기본값으로 되돌린다.
SET SESSION lock_wait_timeout = DEFAULT;
