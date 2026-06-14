-- 랭킹 닉네임 정제 스케줄러: BLOCKED 닉네임을 보유한 PlayerEntity 조회 최적화
CREATE INDEX idx_player_player_name ON player (player_name);
