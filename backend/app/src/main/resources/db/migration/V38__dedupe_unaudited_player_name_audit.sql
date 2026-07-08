-- #1519: #1467 fix 배포 이전에 생성된 잔존 중복 데이터 정리.
-- 같은 player_name이 (UNAUDITED)와 이미 검열된(terminal) 상태로 공존하면, 스케줄러가 UNAUDITED를
-- terminal 상태로 승격하는 순간 uq_player_name_audit_name_status(player_name, status)에 충돌한다.
-- 배치 전체가 롤백되고 문제 행이 UNAUDITED로 남아 매 스케줄 tick마다 재크래시 → 큐 영구 적체.
-- 이미 검열 완료된 terminal 행이 권위 있는 판정을 보유하므로, 중복된 UNAUDITED 행을 제거한다.
DELETE u
FROM player_name_audit u
         INNER JOIN player_name_audit t
                    ON t.player_name = u.player_name
                        AND t.status <> 'UNAUDITED'
WHERE u.status = 'UNAUDITED';
