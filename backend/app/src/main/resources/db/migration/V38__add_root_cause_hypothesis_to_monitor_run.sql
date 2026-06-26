-- LLM 근본원인 가설을 영속화한다. 기존엔 MonitorAnalysis.rootCauseHypothesis가 저장 시 버려졌으나,
-- 자동 수정 봇(zzolbot remediation)이 결함 분류·결함 위치 특정 입력으로 사용하므로 컬럼을 추가한다.
ALTER TABLE zzolbot_monitor_run
    ADD COLUMN root_cause_hypothesis TEXT DEFAULT NULL AFTER analysis_summary;
