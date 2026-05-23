-- Dashboard 쿼리 성능 최적화를 위한 인덱스

-- 1. roulette_result.created_at 인덱스
-- 용도: findTopWinnersBetween, findLowestProbabilityWinner
-- WHERE roulette_result.created_at BETWEEN ? AND ?
CREATE INDEX idx_roulette_result_created_at ON roulette_result (created_at);

-- 2. room_session.created_at 인덱스
-- 용도: findGamePlayCountByMonth
-- WHERE room_session.created_at BETWEEN ? AND ?
CREATE INDEX idx_room_session_created_at ON room_session (created_at);
