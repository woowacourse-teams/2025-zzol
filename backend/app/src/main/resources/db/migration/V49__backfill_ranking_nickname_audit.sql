-- #1906: 랭킹에 쓰는 닉네임을 저장할 때 검열 큐에 넣도록 바꿨다. 그 전에 저장된 이름은 검열 행이 없어서
-- 랭킹에서 "처리 중인 닉네임"으로 가려진 채 풀리지 않는다. 이번 달 랭킹에 오를 수 있는 이름을 한 번 넣는다.
--
-- 대상은 랭킹이 닉네임을 읽는 두 곳이다.
--   1. 미니게임 랭킹 다섯 개: player.player_name
--   2. TOP5·최저 확률 당첨자: 룰렛 당첨 회원의 app_user.nickname
-- 랭킹 구간은 이번 달 1일부터다. 서버와 DB 시간대가 달라도 이번 달을 다 덮도록 32일을 본다.
--
-- 이미 검열 행이 있는 이름은 넣지 않는다. 비교는 player_name 콜레이션을 따라 대소문자와 끝 공백을 무시한다.
-- 애플리케이션의 register()가 existsByNickname으로 거르는 기준과 같다.
-- 콜레이션이 같다고 보는 이름이 원천에 여럿 있으면 (player_name, status) 유니크 키에 걸리는데,
-- insertUnaudited와 같이 ON DUPLICATE KEY UPDATE로 흡수한다.
-- 원천 컬럼(player·app_user)은 utf8mb4_0900_ai_ci, 검열 컬럼은 utf8mb4_unicode_ci라 그대로 비교하면 ERROR 1267로 멈춘다(#1917).
-- 검열 컬럼 콜레이션으로 맞춰 PAD SPACE·대소문자 무시 기준을 지킨다.
-- created_at은 엔티티의 Instant 매핑과 맞춰 UTC로 넣는다.
INSERT INTO player_name_audit (player_name, status, attempt_count, created_at)
SELECT candidate.name, 'UNAUDITED', 0, UTC_TIMESTAMP(6)
FROM (SELECT p.player_name AS name
      FROM player p
      WHERE p.created_at >= NOW() - INTERVAL 32 DAY
      UNION
      SELECT u.nickname
      FROM roulette_result r
               JOIN player p ON p.id = r.winner_id
               JOIN app_user u ON u.id = p.user_id
      WHERE r.created_at >= NOW() - INTERVAL 32 DAY
        AND u.nickname IS NOT NULL) candidate
WHERE NOT EXISTS (SELECT 1 FROM player_name_audit a WHERE a.player_name = candidate.name COLLATE utf8mb4_unicode_ci)
ON DUPLICATE KEY UPDATE id = id;
