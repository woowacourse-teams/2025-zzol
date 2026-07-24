-- 시즌 정산 테이블 (#1610)
--
-- season_settlement: 정산 원장. 한 행 = "한 게임에서 한 회원에게 포인트 지급".
--   (room_session_id, mini_game_type, user_id) 유니크가 멱등성의 최종 방어선이다 —
--   컨슈머 그룹의 at-least-once 재전달·동시 처리 경합에도 이중 지급을 DB가 차단한다.
-- season_score: 시즌·회원 단위 누적 성적. 리더보드 ZSET의 진실 공급원.

CREATE TABLE season_settlement (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    room_session_id BIGINT       NOT NULL,
    mini_game_type  VARCHAR(20)  NOT NULL,
    user_id         BIGINT       NOT NULL,
    player_rank     INT          NOT NULL,
    score           BIGINT       NOT NULL,
    points          INT          NOT NULL,
    season_key      VARCHAR(10)  NOT NULL,
    created_at      TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_season_settlement_result (room_session_id, mini_game_type, user_id),
    KEY idx_season_settlement_user (season_key, user_id)
);

CREATE TABLE season_score (
    id           BIGINT       AUTO_INCREMENT PRIMARY KEY,
    season_key   VARCHAR(10)  NOT NULL,
    user_id      BIGINT       NOT NULL,
    total_points BIGINT       NOT NULL,
    games_played INT          NOT NULL,
    tier         VARCHAR(20)  NOT NULL,
    updated_at   TIMESTAMP(6) NOT NULL,
    UNIQUE KEY uk_season_score_user (season_key, user_id),
    -- 시즌 리더보드 조회(ORDER BY total_points DESC)용
    KEY idx_season_score_rank (season_key, total_points DESC)
);
