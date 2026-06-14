# 0018. 비속어 필터링 자체 모듈 전환 — `:profanity`

- 날짜: 2026-05-23
- 상태: 승인

## 컨텍스트

비속어 필터링은 `io.github.vaneproject:badwordfiltering:1.0.0` 외부 라이브러리에 의존하고 있었다.
이 라이브러리는 한국어 단어 목록만 내장하며, 단어 목록을 런타임에 교체하거나 영어 비속어를 추가하는 기능이 없다.
또한 단어를 메모리에만 관리하기 때문에 다중 인스턴스 환경에서 인스턴스 간 동기화가 불가능하다.

운영 중 아래 요구사항이 추가됐다.

- 한국어 + 영어 비속어를 함께 관리해야 한다 (오픈소스 목록 통합: vane, LDNOOBW 등)
- 관리자가 런타임에 단어를 추가·수정·비활성화할 수 있어야 한다
- AI 기반 닉네임 검열(Gemini)은 `:profanity` 모듈 내 `domain/audit` 패키지에 통합한다

## 결정

외부 라이브러리 의존을 제거하고 `:profanity` Gradle 서브모듈로 자체 구현한다.

### 모듈 의존 관계

```text
:common  ←  ProfanityWordBlockedEvent, ProfanityChecker, NicknameSubmittedEvent 유지 (cross-module 계약)
:profanity  →  :common
:room  →  :common      (:profanity 의존 없음, ProfanityChecker 계약만 참조)
:admin  →  :profanity  (단어 관리 CRUD)
:user  →  :common      (:profanity 의존 없음)
```

`ProfanityChecker` 인터페이스와 `NicknameSubmittedEvent`는 `:common`에 둔다.
`:room`과 `:user` 모두 `:profanity`를 거치지 않고 `ProfanityChecker` 계약만 `:common`에서 참조한다.
`:user`는 닉네임 변경 시 `NicknameSubmittedEvent`를 직접 발행하며,
`:room`은 `PlayerNameRankingCleanupService`에서 `ProfanityChecker`를 주입받아 사용한다.
`:room`의 relay 리스너(`UserNicknameAuditListener`)는 제거됐다.

### `:profanity` 내부 구조

```text
domain/
  ProfanityWord        — 비속어 단어 (word, language, source, is_active); 검증만 담당, 정규화는 TextNormalizer 독점
  ProfanityChecker     — 인터페이스 (:common으로 이관)
  ProfanityWordRepository
  Language             — KOREAN / ENGLISH
  audit/
    NicknameAuditor    — AI 검열 포트 인터페이스
    NicknameAuditResult / NicknameAuditStatus / AiConfidence — 검열 결과 도메인
    NicknameSubmittedEvent — :common으로 이관 (cross-module 이벤트)

application/
  ProfanityFilterService          — ProfanityChecker 구현, Aho-Corasick + TextNormalizer 조합
  ProfanityWordManagementService  — CRUD (add / deactivate / bulk import)
  NicknameAuditService            — UNAUDITED 닉네임 배치 검열 오케스트레이션
  NicknameAuditBatchProcessor     — 단건 검열 결과 적용 + FLAGGED 시 사전 등록
  NicknameFeedbackService         — 관리자 수동 허용/차단 피드백 처리
  NicknameSubmittedEventListener  — NicknameSubmittedEvent 수신 → 검열 큐 등록

infra/
  AhoCorasickEngine          — ahocorasick-java, trie 빌드 및 매칭
  TextNormalizer             — 리트스피크(1→i, 3→e, 0→o), 유니코드 정규화, 특수문자 제거
  ProfanityWordJpaRepository
  WordListResourceLoader     — 애플리케이션 시작 시 txt 리소스 → DB seed (중복 skip)
  GeminiNicknameAuditor      — NicknameAuditor Gemini 구현체 (@Profile "!local & !test")
  NoOpNicknameAuditor        — 테스트·로컬용 No-op 구현체
  NicknameAuditScheduler     — 12시간 주기 배치 실행
  persistence/audit/         — NicknameAuditEntity(player_name_audit), NicknameFeedbackEntity(player_name_feedback)
```

### 단어 목록 구성

| source             | 언어  | 설명                                                       |
|--------------------|-----|----------------------------------------------------------|
| `VANE`             | 한국어 | vane 라이브러리 내장 목록 추출                                      |
| `LDNOOBW`          | 영어  | List of Dirty, Naughty, Obscene, and Otherwise Bad Words |
| `MANUAL`           | 한/영 | 관리자 직접 등록                                                |
| `AI_FLAGGED`       | 한/영 | Gemini 검열에서 FLAGGED 판정된 닉네임이 자동 등록                       |
| `OPERATOR_ALLOWED` | 한/영 | 관리자가 AI 오판 닉네임을 허용 처리할 때 등록; MANUAL만 재차단 가능              |

### 다중 인스턴스 동기화

관리자가 단어를 추가·변경하면 Redis pub/sub으로 모든 인스턴스에 즉시 broadcast해 Aho-Corasick trie를 리빌드한다.
이미 `:infra`에서 Redis를 사용하고 있으므로 추가 인프라 의존이 없다.

### 랭킹 비속어 처리

`PlayerNameRankingCleanupService`(:room)가 참조하던 `PlayerNameAuditRepository.findByStatus(BLOCKED)`를
`ProfanityWordRepository.findByIsActiveTrue()`로 교체한다.
단어 진실 공급원(source of truth)이 `:profanity` 모듈의 DB 테이블로 일원화된다.

### DB 스키마

```sql
profanity_word (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  word        VARCHAR(200) NOT NULL,
  language    VARCHAR(10)  NOT NULL,  -- KOREAN | ENGLISH
  source      VARCHAR(20)  NOT NULL,  -- VANE | LDNOOBW | MANUAL | AI_FLAGGED | OPERATOR_ALLOWED
  is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
  created_at  TIMESTAMP    NOT NULL,
  updated_at  TIMESTAMP    NOT NULL,
  UNIQUE KEY uk_profanity_word (word)
)
```

### 닉네임 검열 큐 테이블 (`:room`에서 이전, 테이블명 하위호환 유지)

```sql
player_name_audit (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  nickname    VARCHAR(10)  NOT NULL,
  status      VARCHAR(10)  NOT NULL,  -- UNAUDITED | FLAGGED | PENDING | CLEAN | ALLOWED | BLOCKED
  confidence  DECIMAL(3,2),
  reason      VARCHAR(255),
  created_at  TIMESTAMP    NOT NULL,
  audited_at  TIMESTAMP
)

player_name_feedback (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  nickname    VARCHAR(10)  NOT NULL,
  feedback    VARCHAR(10)  NOT NULL,  -- ALLOWED | BLOCKED
  created_at  TIMESTAMP    NOT NULL
)
```

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| vane 라이브러리 유지 + 영어 목록만 추가 | 변경 최소화 | 다중 인스턴스 동기화 불가, 단어 관리 DB 연동 불가 |
| `:profanity` 없이 `:room` 내부에서 직접 구현 | 모듈 수 증가 없음 | `:admin`의 단어 관리 API가 `:room`에 의존해야 해 경계 위반 |
| 단어 목록을 DB 대신 배포 단위로 관리 | 인프라 단순 | 런타임 추가·즉시 반영 불가, 운영 유연성 없음 |
| Perspective API를 1차 필터로 사용 | 우회 패턴 탐지 우수 | 레이턴시 100~300ms, 닉네임 입력 응답 지연, 비용 |

## 트레이드오프

**감수한 것들**

- Aho-Corasick trie 리빌드는 단어 변경 시마다 발생한다. 단어 수가 수만 건을 넘지 않는 한 수십 ms 이내이므로 허용한다.
- `ProfanityWordBlockedEvent`가 `:common`에 남아 있어 패키지 순수성이 완전하지 않다.
  `:user`를 `:profanity`에 의존시키는 것보다 현실적인 절충이다.
- TextNormalizer가 놓치는 우회 패턴(초성 단독, 이모지 조합 등)은 ADR 0001의 AI 감사 레이어에서 보완한다.
- `match_count` 기반 목록 정제는 초기 요구사항이었으나, 실제 운영 데이터 없이는 통계 의미가 없다고 판단해 컬럼 자체를 제거했다(V29 마이그레이션). 필요 시 별도 테이블로 재도입한다.

**얻은 것들**

- 한국어·영어 비속어를 단일 DB 테이블에서 관리하고 언어·출처별로 필터링·정제할 수 있다.
- Redis pub/sub으로 다중 인스턴스 간 즉시 동기화된다.
- AI 검열 레이어는 `:profanity-ai` 별도 모듈 대신 `:profanity` 내부 `domain/audit` 패키지로 통합했다. 초기 설계에서는 Perspective API 기반 Phase 2로 분리 예정이었으나, Gemini 기반 구현이 `:profanity`의 응집도를 높이는 방향이라 판단해 통합을 선택했다. AI 의존이 커질 경우 패키지 → 모듈 분리는 여전히 가능하다.

## 결과

- `io.github.vaneproject:badwordfiltering` 의존을 `:room`과 `:app`(testImplementation)에서 모두 제거한다
- `ProfanityChecker` 인터페이스를 `coffeeshout.room.domain.service` → `coffeeshout.global.nickname`으로 이관한다
- `NicknameSubmittedEvent`를 `coffeeshout.profanity.domain.audit` → `coffeeshout.global.nickname`으로 이관한다
- `:user` 닉네임 변경 시 `NicknameSubmittedEvent`를 직접 발행하도록 변경하고, relay 역할의 `UserNicknameAuditListener`(:room)를 제거한다
- `VaneProfanityChecker`, `ProfanityCheckerSyncListener`, `CustomProfanityLoader`를 제거하고 `:profanity` 구현체로 대체한다
- `PlayerNameRankingCleanupService`의 차단 단어 조회를 `ProfanityWordRepository`로 교체한다
- `:admin` 모듈에 비속어 관리 CRUD REST API(`/admin/profanity/words`)를 추가한다
