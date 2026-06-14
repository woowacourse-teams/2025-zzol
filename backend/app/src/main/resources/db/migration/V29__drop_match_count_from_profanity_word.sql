ALTER TABLE profanity_word DROP COLUMN match_count;

-- UNAUDITED 상태 닉네임 중복 등록 방지 (TOCTOU 레이스 컨디션 차단)
-- 유니크 제약 추가 전 기존 중복 행 제거 (id 작은 것 보존)
DELETE a1
FROM player_name_audit a1
         INNER JOIN player_name_audit a2
                    ON a1.player_name = a2.player_name
                        AND a1.status = a2.status
                        AND a1.id > a2.id;

ALTER TABLE player_name_audit
    ADD UNIQUE KEY uq_player_name_audit_name_status (player_name, status);
