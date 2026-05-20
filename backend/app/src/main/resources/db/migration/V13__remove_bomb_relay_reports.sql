-- BombRelay 게임 제거에 따른 데이터 정리.
-- gameType은 nullable이며 본문(content/joinCode)는 운영 참고용으로 보존한다.
UPDATE report
SET game_type = NULL
WHERE game_type = 'BOMB_RELAY';
