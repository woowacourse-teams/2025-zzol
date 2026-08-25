-- 평가 시나리오에 검증 대상 경로(kind) 추가 (#1626)
--
-- 기존 eval 하네스는 챗봇(진단) 경로만 채점했다. 무인으로 Slack에 직행하는
-- 알림 분석 경로(AnomalyAnalyzer)에도 골든 시나리오를 두기 위해, 시나리오가
-- 어느 경로를 검증하는지 구분하는 kind(CHAT/MONITOR)를 추가한다.
-- 출처(source_type)와 직교하는 축이다 — 출처는 "어디서 왔나", kind는 "무엇을 채점하나".

ALTER TABLE zzolbot_eval_scenario
    ADD COLUMN kind VARCHAR(20) NOT NULL DEFAULT 'CHAT' AFTER name;

-- DEFAULT는 기존 행 백필용이다. 새 행은 앱이 항상 kind를 명시하므로 제거한다.
ALTER TABLE zzolbot_eval_scenario
    ALTER COLUMN kind DROP DEFAULT;
