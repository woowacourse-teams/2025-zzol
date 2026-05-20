-- 방 세션 테이블
CREATE TABLE room_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    join_code VARCHAR(5) NOT NULL,
    room_status VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 플레이어 테이블
CREATE TABLE player (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_session_id BIGINT NOT NULL,
    player_name VARCHAR(10) NOT NULL,
    player_type VARCHAR(10) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_player_room_session FOREIGN KEY (room_session_id) REFERENCES room_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 룰렛 결과 테이블
CREATE TABLE roulette_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_session_id BIGINT NOT NULL,
    winner_id BIGINT NOT NULL,
    winner_probability INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_roulette_room_session FOREIGN KEY (room_session_id) REFERENCES room_session(id),
    CONSTRAINT fk_roulette_winner FOREIGN KEY (winner_id) REFERENCES player(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 미니게임 플레이 테이블
CREATE TABLE mini_game_play (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_session_id BIGINT NOT NULL,
    mini_game_type VARCHAR(20) NOT NULL,
    CONSTRAINT fk_mini_game_room_session FOREIGN KEY (room_session_id) REFERENCES room_session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 미니게임 결과 테이블
CREATE TABLE mini_game_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mini_game_play_id BIGINT NOT NULL,
    player_id BIGINT NOT NULL,
    player_rank INT NOT NULL,
    score INT,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_mini_game_result_play FOREIGN KEY (mini_game_play_id) REFERENCES mini_game_play(id),
    CONSTRAINT fk_mini_game_result_player FOREIGN KEY (player_id) REFERENCES player(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
