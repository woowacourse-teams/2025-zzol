# 닉네임 시스템 설계

> **브랜치:** `be/feat/1123-nickname`
> **상태:** 1·2번 구현 완료, 3번 구현 진행 중 (Phase 1~5 완료)

---

## 개요

닉네임 시스템은 세 가지 기능으로 구성된다.

| # | 기능                 | 상태    |
|---|--------------------|-------|
| 1 | 닉네임 자동 생성          | 구현 완료 |
| 2 | 입장 시 비속어 검열        | 구현 완료 |
| 3 | 랭킹 닉네임 AI 기반 사후 검열 | 구현 중  |

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
- [x] `spring-boot-starter-security` 의존성 추가
- [x] `spring-boot-starter-thymeleaf` + `thymeleaf-extras-springsecurity6` 의존성 추가
- [x] `com.google.genai:google-genai:1.44.0` Gemini SDK 의존성 추가
- [x] `NicknameAuditStatus` Enum (`UNAUDITED`, `FLAGGED`, `PENDING`, `CLEAN`)
- [x] `NicknameAuditResult` record — `of()` 팩토리 메서드 (external threshold)
- [x] `NicknameAuditor` 포트 인터페이스 (`domain/audit/`)
- [x] `V6__create_nickname_audit_tables.sql` — 3개 테이블 + 인덱스
- [x] `NicknameAuditEntity` + `NicknameAuditJpaRepository`
- [x] `NicknameFeedbackEntity` + `NicknameFeedbackJpaRepository`
- [x] `CustomProfanityEntity` + `CustomProfanityJpaRepository`
- [x] `NicknameAuditProperties` — `@ConfigurationProperties(prefix = "nickname-audit")`
- [x] `GeminiNicknameAuditor` — SDK 기반 배치 호출, 피드백 주입, 파싱 실패 시 skip
- [x] `NoOpNicknameAuditor` — local/test 프로파일용 no-op 구현체
- [x] `NicknameAuditService` — 증분 스캔, 배치 간 rate limit 대기, 메트릭
- [x] `NicknameAuditScheduler` — 12시간 주기 (`infra/`)
- [x] `RouletteService` — 룰렛 결과 저장 시 `nicknameAuditService.register()` 훅
- [x] `NicknameFeedbackService` — 운영자 피드백 저장 + 비속어 등록
- [x] `CustomProfanityLoader` (`ApplicationRunner`) — 시작 시 DB → BadWordFiltering 로딩
- [x] `GeminiNicknameAuditorConnectivityTest` — 실제 API 연결 테스트 (`@Disabled`)
- [ ] `SecurityConfig` — `/admin/**` 인증, 기존 API/WS `permitAll()`
- [ ] 운영자 대시보드 (`/admin/nickname-audit`) — 목록 조회 / 승인 / 거절

---

## Feature 3: 랭킹 닉네임 AI 기반 사후 검열

### 배경과 목적

입장 시 비속어 필터를 통과했더라도, 창의적 우회(예: "씨b알", 유사 글자 대체, 문맥 의존 표현)는 탐지하지 못한다. 랭킹에 노출되는 닉네임은 공개 가시성이 높으므로 사후 AI 검열을 추가한다.

### AI 모델

**Gemini 2.5 Flash** (`gemini-2.5-flash`) — `application.yml`의 `nickname-audit.model`로 변경 가능

| 항목  | 제한        |
|-----|-----------|
| RPM | 5 (무료 티어) |
| 비용  | 무료        |

> **주의:** `gemini-2.0-flash`는 무료 티어 quota가 0으로 설정된 계정이 있어 즉시 429를 반환한다. `gemini-2.5-flash`로 변경 확인 완료.

Rate limit 초과 시 **Resilience4j** retry + 지수 백오프 적용 (2s → 4s → 8s, 최대 30s).

### 처리 흐름

```
룰렛 결과 저장 (RouletteService.saveRouletteResult())
  → nicknameAuditService.register(winnerNickname)
      → nickname_audit에 (nickname, status=UNAUDITED) INSERT

[스케줄러 (12시간마다)]
  │
  ▼
NicknameAuditService.auditPending()
  │  UNAUDITED & audited_at IS NULL 조건으로 증분 조회 (batchSize=100)
  │
  ├─ [배치 루프]
  │   → GeminiNicknameAuditor.audit(batch)   ← Gemini API 호출
  │   → JSON 파싱 성공: entity.complete(status, confidence, reason)
  │   → JSON 파싱 실패: 빈 리스트 반환 → 해당 배치 skip (UNAUDITED 유지, 다음 실행 재시도)
  │   → 배치 간 13초 대기 (RPM 5 대응)
  │
  └─ 결과 분류
      ├─ confidence ≥ 0.85 & flagged=true  → FLAGGED
      ├─ confidence < 0.85 & flagged=true  → PENDING
      └─ flagged=false                     → CLEAN
```

**자동 삭제/변경은 하지 않는다.** AI 오판 가능성이 있으므로 모든 처리는 운영자 검토를 거친다.

### 신뢰도 임계값

| 상태        | 조건                                   | 처리             |
|-----------|--------------------------------------|----------------|
| `FLAGGED` | `flagged=true` & `confidence ≥ 0.85` | 운영자 검토 큐 상단 노출 |
| `PENDING` | `flagged=true` & `confidence < 0.85` | 운영자 대시보드 보류 목록 |
| `CLEAN`   | `flagged=false`                      | 별도 처리 없음       |

> 임계값 0.85는 초기값이며, `application.yml`의 `nickname-audit.flagged-threshold`로 조정한다.

### Rate Limit 대응 전략

```
batchSize = 100  (API 호출 1회당 처리량)
batchDelayMs = 13000  (RPM 5 → 60s/5 = 12s + 1s 버퍼)
→ 배치 1 처리 → 13초 대기 → 배치 2 처리 → ...
→ 100개 × 5회/min = 500개/min 처리 가능
```

### 프롬프트 설계

#### 기본 구조

```
너는 한국어 닉네임 검열 전문가다.
아래 닉네임 목록을 검토하고 각 항목에 대해 JSON 배열로만 응답하라.

비속어 판단 기준:
- 직접적 욕설뿐 아니라 자모 분리, 특수문자 삽입, 유사 발음 대체로 우회한 경우 포함
- 문화적 맥락을 고려한다 (예: "미쳤다"는 일반 감탄사로 사용되므로 flagged=false)
- 판단이 애매한 경우 confidence를 낮게 설정한다

응답 형식:
[
  { "nickname": "씨b알",     "flagged": true,  "confidence": 0.97, "reason": "비속어 우회 (특수문자 삽입)" },
  { "nickname": "용감한호랑이", "flagged": false, "confidence": 0.99, "reason": "일반 닉네임" }
]
```

`GenerateContentConfig.responseMimeType("application/json")` 설정으로 Gemini가 JSON만 반환하도록 강제한다.

#### 오판 피드백 주입 (Prompt Refinement)

운영자가 AI 판단을 뒤집으면 해당 케이스를 few-shot 예시 풀에 누적한다.

```
[운영자 피드백 누적 예시]
- "열받네" → 운영자가 CLEAN 승인 → few-shot에 confidence=0.01로 추가
- "ㅅㅂ야" → 운영자가 직접 REJECTED 처리 → few-shot에 confidence=0.99로 추가
```

피드백 풀이 `feedback-injection-threshold`(기본 20건) 이상 쌓이면 프롬프트에 동적으로 삽입한다.

---

### 운영자 인증 — Spring Security

**확정: `spring-boot-starter-security` + 환경변수 기반 InMemoryUserDetails**

#### 도입 이유

`/admin/**` 경로를 인증 없이 열어두면 프로덕션 보안 사고로 직결된다. Thymeleaf form 로그인과 Spring Security의 세션 기반 인증은 자연스럽게 결합되며, 별도 토큰 발급이나 OAuth 설정 없이 간단하게 구성할 수 있다.

#### 운영자 계정 관리 전략

운영자 계정이 2~3개 고정이므로 환경변수 기반 InMemoryUserDetails로 충분하다.

#### Security 구성 핵심 원칙

`spring-boot-starter-security` 추가 시 **모든 엔드포인트**에 기본 인증이 걸린다. 기존 REST API(`/rooms/**`)와 WebSocket 핸드셰이크(`/ws/**`)가 영향을 받지 않도록 `permitAll()`로 명시적으로 열어야 한다.

```java
// global/config/SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .anyRequest().permitAll()
            )
            .formLogin(form -> form
                .loginPage("/admin/login")
                .defaultSuccessUrl("/admin/nickname-audit")
                .permitAll()
            )
            .csrf(AbstractHttpConfigurer::disable);  // REST API + 내부망 admin
        return http.build();
    }
}
```

---

### 커스텀 비속어 DB 연동

#### 흐름

```
운영자가 FLAGGED 닉네임 REJECTED 처리
  → NicknameFeedbackService.reject(auditId)
      → custom_profanity 테이블에 저장 (중복 체크 후)
      → BadWordFiltering.add(nickname)   ← 런타임 즉시 반영

애플리케이션 시작 시
  → CustomProfanityLoader (ApplicationRunner)
      → SELECT word FROM custom_profanity
      → BadWordFiltering.add(word) × N  ← 재시작 후 복구
```

---

### 설정값 (`application.yml`)

```yaml
nickname-audit:
  gemini-api-key: ${GEMINI_API_KEY:}
  model: gemini-2.5-flash              # 모델 변경 시 이 값만 수정
  flagged-threshold: 0.85
  batch-size: 100                      # API 호출 1회당 처리량
  batch-delay-ms: 13000                # RPM 5 대응 배치 간 대기 (ms)
  feedback-injection-threshold: 20     # few-shot 주입 최소 피드백 수
```

프로파일별 동작:

| 프로파일 | NicknameAuditor 구현체 | API 키 필요 |
|----------|----------------------|------------|
| `local`, `test` | `NoOpNicknameAuditor` (항상 CLEAN 반환) | 불필요 |
| `dev`, `prod` | `GeminiNicknameAuditor` | 필요 |

---

### 모니터링 (Micrometer → Prometheus → Grafana)

#### 수집 메트릭

| 메트릭 이름                                 | 타입      | 태그                               | 설명                       |
|----------------------------------------|---------|----------------------------------|--------------------------|
| `nickname.audit.gemini.call.duration`  | Timer   | —                                | Gemini API 호출 레이턴시       |
| `nickname.audit.gemini.parse.failures` | Counter | —                                | JSON 파싱 실패 횟수            |
| `nickname.audit.result`                | Counter | `status=FLAGGED\|PENDING\|CLEAN` | 검열 결과 분포                 |
| `nickname.audit.batch.skipped`         | Counter | —                                | 파싱 실패로 skip된 배치 수        |
| `nickname.audit.unaudited.queue`       | Gauge   | —                                | 스케줄러 실행 시점 UNAUDITED 적체량 |

#### Grafana 대시보드 설정

**1. Prometheus scrape 확인**

`/actuator/prometheus` 엔드포인트에서 메트릭이 노출되는지 확인한다.

```bash
curl http://localhost:8080/actuator/prometheus | grep nickname_audit
```

**2. 패널 구성 예시**

**Gemini API 레이턴시 (p99)**
```promql
histogram_quantile(0.99,
  rate(nickname_audit_gemini_call_duration_seconds_bucket[5m])
)
```

**검열 결과 분포 (stacked bar)**
```promql
# FLAGGED 비율
rate(nickname_audit_result_total{status="FLAGGED"}[1h])

# PENDING 비율
rate(nickname_audit_result_total{status="PENDING"}[1h])

# CLEAN 비율
rate(nickname_audit_result_total{status="CLEAN"}[1h])
```

**파싱 실패율**
```promql
rate(nickname_audit_gemini_parse_failures_total[1h])
```

**UNAUDITED 적체량 (스케줄러 실행 시점)**
```promql
nickname_audit_unaudited_queue
```

**3. 알림(Alert) 권장 조건**

| 조건 | 임계값 | 의미 |
|------|--------|------|
| `nickname_audit_gemini_parse_failures_total` 증가율 | > 3회/시간 | 프롬프트 또는 모델 응답 형식 문제 |
| `nickname_audit_unaudited_queue` | > 500 | 스케줄러가 처리를 못 따라가는 상황 |
| `nickname_audit_gemini_call_duration_seconds` p99 | > 30s | Gemini 응답 지연 |

---

### 운영자 대시보드

**확정: Thymeleaf (서버사이드 렌더링)**

백엔드 팀 단독 운영이므로 Thymeleaf가 적합하다. Spring Boot와 자연스럽게 통합되고 별도 빌드 파이프라인이 없으며 CORS 설정이 불필요하다.

### 보류 중인 결정

| 항목             | 선택지       | 현재 상태      |
|----------------|-----------|------------|
| confidence 임계값 | 0.85 (초안) | 운영 후 조정 예정 |

---

## ADR 등록 후보

아래 결정이 확정되면 ADR로 분리한다.

| 주제                                | 등록 조건             |
|-----------------------------------|-------------------|
| 비속어 라이브러리 선택 (bad-word-filtering) | Feature 2 구현 완료 후 |
| AI 검열 + 운영자 피드백 루프 설계             | Feature 3 구현 완료 후 |
