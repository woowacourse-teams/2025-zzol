-- Dashboard 쿼리 성능 최적화를 위한 인덱스

-- 1. roulette_result.created_at 인덱스
-- 용도: findTopWinnersBetween, findLowestProbabilityWinner 쿼리의 날짜 범위 검색
CREATE INDEX idx_roulette_result_created_at ON roulette_result (created_at);

-- 2. room_session.created_at 인덱스
-- 용도: findGamePlayCountByMonth 쿼리의 날짜 범위 검색
CREATE INDEX idx_room_session_created_at ON room_session (created_at);
