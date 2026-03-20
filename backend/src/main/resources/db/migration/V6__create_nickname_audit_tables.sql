-- Feature 3: 랭킹 닉네임 AI 기반 사후 검열

-- 1. nickname_audit: AI 검열 대상 및 결과
-- 랭킹 등록 시점에 UNAUDITED 상태로 INSERT, 스케줄러가 증분 처리
CREATE TABLE nickname_audit (
    id          BIGINT          AUTO_INCREMENT PRIMARY KEY,
    nickname    VARCHAR(10)     NOT NULL,
    status      ENUM('UNAUDITED', 'FLAGGED', 'PENDING', 'CLEAN') NOT NULL DEFAULT 'UNAUDITED',
    confidence  DECIMAL(3, 2),
    reason      VARCHAR(255),
    created_at  DATETIME        NOT NULL,
    audited_at  DATETIME
);

-- 스케줄러 증분 스캔 쿼리:
--   WHERE status = 'UNAUDITED' AND audited_at IS NULL ORDER BY created_at ASC LIMIT 500
-- (status, audited_at) → UNAUDITED & NULL 조건 필터
-- created_at → 등록 순서대로 정렬 (포함하여 커버링 인덱스 구성)
CREATE INDEX idx_nickname_audit_status_audited_created
    ON nickname_audit (status, audited_at, created_at);

-- 2. nickname_feedback: 운영자 피드백 이력 (AI 오판 보정용 few-shot 재료)
CREATE TABLE nickname_feedback (
    id                BIGINT          AUTO_INCREMENT PRIMARY KEY,
    nickname          VARCHAR(10)     NOT NULL,
    ai_flagged        BOOLEAN         NOT NULL,
    ai_confidence     DECIMAL(3, 2)   NOT NULL,
    operator_decision ENUM('APPROVED', 'REJECTED') NOT NULL,
    reason            VARCHAR(255),
    created_at        DATETIME        NOT NULL
);

-- few-shot 주입 쿼리:
--   SELECT * FROM nickname_feedback ORDER BY created_at DESC LIMIT 20
CREATE INDEX idx_nickname_feedback_created_at
    ON nickname_feedback (created_at);

-- 3. custom_profanity: 운영자 REJECTED 처리로 누적된 커스텀 비속어 목록
-- 애플리케이션 시작 시 전량 로딩 → BadWordFiltering.add()
CREATE TABLE custom_profanity (
    id         BIGINT          AUTO_INCREMENT PRIMARY KEY,
    word       VARCHAR(20)     NOT NULL,
    source     ENUM('AI_AUDIT', 'OPERATOR_MANUAL') NOT NULL,
    created_at DATETIME        NOT NULL,
    UNIQUE KEY uq_custom_profanity_word (word)  -- 중복 등록 방지 + 조회 인덱스
);
