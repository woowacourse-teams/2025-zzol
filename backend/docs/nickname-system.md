# 닉네임 시스템 설계

> **브랜치:** `be/feat/1123-nickname`
> **상태:** 1·2번 구현 완료, 3번 설계 중

---

## 개요

닉네임 시스템은 세 가지 기능으로 구성된다.

| # | 기능 | 상태 |
|---|------|------|
| 1 | 닉네임 자동 생성 | 구현 완료 |
| 2 | 입장 시 비속어 검열 | 구현 완료 |
| 3 | 랭킹 닉네임 AI 기반 사후 검열 | 설계 중 |

---

## Feature 2: 입장 시 비속어 검열

### 선택 라이브러리

**[bad-word-filtering](https://github.com/VaneProject/bad-word-filtering)** (`io.github.vaneproject:badwordfiltering:1.0.0`)

- 한국어 특화 비속어 사전 내장
- `check(text)` — 기본 탐지
- `blankCheck(text)` — 공백 삽입 우회 탐지 (예: "씨 발")
- 커스텀 단어 추가/제거 가능

> **확인된 한계:** `blankCheck()`는 공백 삽입은 탐지하나 특수문자 삽입(예: `씨*발`)은 탐지하지 못한다. 초성 우회(예: `ㅅㅂ`) 또한 기본 제공 범위 밖이다. 두 케이스 모두 실제 우회 사례 발견 시 `BadWordFiltering.add()`로 커스텀 단어를 보완한다.

### 아키텍처

```
POST /rooms         (호스트 방 생성)
POST /rooms/{joinCode} (게스트 입장)
  ↓
RoomService.createRoom() / enterRoomAsync()
  → nicknameValidator.validate(name)   ← Redis Stream 발행 전 동기 실행
      BadWordFiltering.check()
      BadWordFiltering.blankCheck()
      └─ 탐지 시 InvalidArgumentException(PLAYER_NAME_CONTAINS_PROFANITY)
          → RestExceptionHandler → 400 응답
  → (정상) PlayerName 생성 → Room 도메인 처리
```

`BadWordFiltering`은 `RoomConfig`에서 Spring `@Bean`으로 등록해 싱글톤 관리한다. `PlayerName` VO는 DI가 불가능한 record이므로, 비속어 검증은 application layer(`NicknameValidator`)에서 처리한다.

### 추가된 ErrorCode

```java
// RoomErrorCode
PLAYER_NAME_CONTAINS_PROFANITY("비속어가 포함된 닉네임은 사용할 수 없습니다."),
```

### 적용 지점

- `POST /rooms` (호스트 방 생성) — `RoomService.createRoom()`
- `POST /rooms/{joinCode}` (게스트 입장) — `RoomService.enterRoomAsync()`

---

## Feature 1: 닉네임 자동 생성

### 생성 전략

`[형용사] + [명사]` 조합. 예: "용감한호랑이", "빠른여우"

- 최대 길이 10자 이내로 단어 목록 선정 (형용사 최대 4자 + 명사 최대 4자)
- 형용사 30개 × 명사 30개 = 900가지 조합
- 충돌 시: 숫자 suffix 없이 다른 조합으로 재시도 (최대 50회)
  - 방 최대 9명 기준 900가지 조합에서 충돌 가능성 극히 낮음

### 엔드포인트

```
GET /rooms/nickname/random           → 호스트용 (빈 목록 기준 생성)
GET /rooms/nickname/random?joinCode= → 게스트용 (방 기존 멤버 제외)
```

`joinCode`가 있으면 현재 방 멤버 닉네임을 제외하고 생성한다.

---

## 구현 체크리스트

### Feature 2: 입장 시 비속어 검열
- [x] `build.gradle.kts` — `badwordfiltering:1.0.0` 의존성 추가
- [x] `RoomErrorCode` — `PLAYER_NAME_CONTAINS_PROFANITY` 추가
- [x] `RoomConfig` — `BadWordFiltering` Bean 등록
- [x] `NicknameValidator` — `check()` + `blankCheck()` 적용
- [x] `RoomService.createRoom()` — 호스트 닉네임 검증 연결
- [x] `RoomService.enterRoomAsync()` — 게스트 닉네임 검증 연결
- [x] `NicknameValidatorTest` — 단위 테스트 (정상 / 직접 비속어 / 공백 우회)
- [x] `RoomServiceTest` — 통합 테스트 (`닉네임_비속어_검증` @Nested)

### Feature 1: 닉네임 자동 생성
- [x] `NicknameGenerator` — 형용사 30 × 명사 30 조합, 충돌 시 재시도(최대 50회)
- [x] `RoomService.generateRandomNickname()` — joinCode 없는 호스트용
- [x] `RoomService.generateRandomNickname(joinCode)` — joinCode 있는 게스트용
- [x] `RandomNicknameResponse` — 응답 DTO
- [x] `RoomRestController` — `GET /rooms/nickname/random?joinCode={optional}`
- [x] `RoomApi` — Swagger 문서 추가
- [x] `NicknameGeneratorTest` — 단위 테스트 (길이 ≤10, 기존 목록 미포함, 충돌 회피)
- [x] `RoomServiceTest` — 통합 테스트 (`랜덤_닉네임_생성` @Nested)

### Feature 3: 랭킹 닉네임 AI 기반 사후 검열
- [ ] Gemini SDK 의존성 추가
- [ ] `NicknameAuditStatus` Enum (`FLAGGED`, `PENDING`, `CLEAN`)
- [ ] `NicknameAuditResult` 도메인 객체
- [ ] `nickname_audit` 테이블 Flyway 마이그레이션
- [ ] `nickname_feedback` 테이블 Flyway 마이그레이션
- [ ] `GeminiNicknameAuditor` — 배치 API 호출 + 신뢰도 기반 분기
- [ ] `NicknameAuditScheduler` — 12시간 주기 스케줄러
- [ ] `NicknameFeedbackService` — 운영자 피드백 저장 + 프롬프트 주입
- [ ] Thymeleaf 의존성 추가
- [ ] 운영자 대시보드 (`/admin/nickname-audit`) — 목록 조회 / 승인 / 거절

---

## Feature 3: 랭킹 닉네임 AI 기반 사후 검열

### 배경과 목적

입장 시 비속어 필터를 통과했더라도, 창의적 우회(예: "씨b알", 유사 글자 대체, 문맥 의존 표현)는 탐지하지 못한다. 랭킹에 노출되는 닉네임은 공개 가시성이 높으므로 사후 AI 검열을 추가한다.

### AI 모델

**Gemini 2.0 Flash (무료 티어)**

| 항목  | 제한         |
|-----|------------|
| RPM | 15         |
| TPD | 1,500 요청/일 |
| 비용  | 무료         |

랭킹 닉네임은 수가 제한적이고 실시간 처리가 아니므로 무료 티어로 충분하다.
Rate limit 초과 시 이미 도입된 **Resilience4j**의 retry/circuit breaker로 처리한다.

### 처리 흐름

```
[스케줄러 (12시간마다) 또는 관리자 수동 트리거]
  │
  ▼
GeminiNicknameAuditor.audit(List<String> nicknames)
  │  닉네임을 최대 50개 단위 배치로 나눔
  │  → Gemini API 호출 (배치당 1회)
  │  → JSON 응답 파싱: { nickname, flagged, confidence, reason }
  │
  ├─ confidence ≥ 임계값 & flagged=true  → FLAGGED (즉시 운영자 검토 큐)
  ├─ confidence < 임계값 & flagged=true  → PENDING (보류, 운영자 대시보드)
  └─ flagged=false                       → CLEAN (정상)
```

**자동 삭제/변경은 하지 않는다.** AI 오판 가능성이 있으므로 모든 처리는 운영자 검토를 거친다.

### 신뢰도 임계값 (초안)

| 상태 | 조건 | 처리 |
|------|------|------|
| `FLAGGED` | `flagged=true` & `confidence ≥ 0.85` | 운영자 검토 큐 상단 노출 |
| `PENDING` | `flagged=true` & `confidence < 0.85` | 운영자 대시보드 보류 목록 |
| `CLEAN` | `flagged=false` | 별도 처리 없음 |

> 임계값 0.85는 초기값이며, 실제 오판율을 보고 조정한다.

### 프롬프트 설계

#### 기본 구조

```
너는 한국어 닉네임 검열 전문가다.
아래 닉네임 목록을 검토하고 각 항목에 대해 JSON으로 응답하라.

비속어 판단 기준:
- 직접적 욕설뿐 아니라 자모 분리, 특수문자 삽입, 유사 발음 대체로 우회한 경우 포함
- 문화적 맥락을 고려한다 (예: "미쳤다"는 일반 감탄사로 사용되므로 flagged=false)
- 판단이 애매한 경우 confidence를 낮게 설정한다

[Few-shot 예시]
입력: ["씨b알", "용감한호랑이", "열받네", "개쓰레기"]
출력:
[
  { "nickname": "씨b알",    "flagged": true,  "confidence": 0.97, "reason": "비속어 우회 (특수문자 삽입)" },
  { "nickname": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "일반 닉네임" },
  { "nickname": "열받네",   "flagged": false, "confidence": 0.80, "reason": "일상 감탄사로 판단" },
  { "nickname": "개쓰레기",  "flagged": true,  "confidence": 0.92, "reason": "비하 표현" }
]

검열할 닉네임 목록:
{nicknames}

JSON 배열로만 응답하라. 다른 텍스트는 포함하지 마라.
```

#### 오판 피드백 주입 (Prompt Refinement)

운영자가 AI 판단을 뒤집으면(FLAGGED → 승인, CLEAN → 수동 신고), 해당 케이스를 few-shot 예시 풀에 누적한다.

```
[운영자 피드백 누적 예시]
- "열받네" → 운영자가 CLEAN 승인 → few-shot에 confidence=0.10으로 추가
- "ㅅㅂ야" → 운영자가 직접 FLAGGED 처리 → few-shot에 confidence=0.99로 추가
```

피드백 풀이 일정 수(예: 20건) 이상 쌓이면 프롬프트에 동적으로 삽입한다.
피드백 데이터는 `nickname_feedback` DB 테이블에 저장하여 재시작 후에도 유지된다.

**저장 스키마 (초안):**

```sql
CREATE TABLE nickname_feedback (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    nickname    VARCHAR(10)  NOT NULL,
    ai_flagged  BOOLEAN      NOT NULL,
    ai_confidence DECIMAL(3,2) NOT NULL,
    operator_decision ENUM('APPROVED', 'REJECTED') NOT NULL,  -- 운영자 최종 판단
    reason      VARCHAR(255),
    created_at  DATETIME     NOT NULL
);
```

> **보류 결정:** 피드백 주입 방식은 구현 확정이나, 운영자 대시보드 범위가 결정되면 이 항목을 ADR로 분리한다.

### 운영자 대시보드

**확정: Thymeleaf (서버사이드 렌더링)**

백엔드 팀 단독 운영이므로 Thymeleaf가 적합하다. Spring Boot와 자연스럽게 통합되고 별도 빌드 파이프라인이 없으며 CORS 설정이 불필요하다. 동적 인터랙션이 필요한 부분(목록 갱신, 승인/거절 처리)은 AJAX로 보완한다.

### 보류 중인 결정

| 항목 | 선택지 | 현재 상태 |
|------|--------|-----------|
| confidence 임계값 | 0.85 (초안) | 운영 후 조정 예정 |

---

## ADR 등록 후보

아래 결정이 확정되면 ADR로 분리한다.

| 주제 | 등록 조건 |
|------|-----------|
| 비속어 라이브러리 선택 (bad-word-filtering) | Feature 2 구현 완료 후 |
| AI 검열 + 운영자 피드백 루프 설계 | Feature 3 구현 완료 후 |
