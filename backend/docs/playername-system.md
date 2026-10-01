# 플레이어명 시스템

플레이어명은 세 단계로 다룬다. 생성, 입장 시 검증, 사후 AI 검열이다. 생성과 검증은 `:room`에, 검열 파이프라인과 비속어 사전은 `:profanity`에 있다. 결정 기록은 [ADR-0001](adr/0001-ai-nickname-audit-with-operator-feedback-loop.md)이다.

## 1. 닉네임 생성

`PlayerNameGenerator`가 `:common`의 `RandomNameWordPool`에서 형용사와 명사를 하나씩 뽑아 붙인다. 예: "용감한호랑이". `PlayerName.MAX_NAME_LENGTH`(10자)를 넘거나 방에 이미 있는 이름이면 다른 조합으로 다시 뽑는다. 재시도 상한은 50회다.

```text
GET /rooms/nickname/random            → RoomService.generateRandomNicknameForHost()
GET /rooms/nickname/random?joinCode=  → RoomService.generateRandomNicknameForGuest(joinCode)  방 멤버 이름 제외
```

## 2. 입장 시 비속어 검증

`RoomService`가 방 생성과 입장에서 `PlayerNameValidator.validate(PlayerName)`을 부른다. 검증기는 `:common`의 `ProfanityChecker` 포트로 묻고, 걸리면 `BusinessException(RoomErrorCode.PLAYER_NAME_CONTAINS_PROFANITY)`을 던져 400으로 응답한다.

포트 구현은 `:profanity`의 `ProfanityFilterService`다. `profanity_word` 테이블의 활성 단어로 ahocorasick Trie를 만들어 두고, 입력을 `TextNormalizer`로 정규화한 뒤 부분 일치를 본다. 단어가 바뀌면 `TrieRefreshNotifier`가 Redis pub/sub으로 알리고 모든 인스턴스가 Trie를 다시 만든다. 재빌드 최소 간격은 `profanity.trie.rebuild.min-interval`이다.

## 3. 사후 AI 검열

입장 필터를 통과한 우회 표현을 잡기 위해 닉네임을 큐에 모아 Gemini로 다시 심사한다.

### 입력

`:common`의 `NicknameSubmittedEvent`를 `ProfanityAuditService.onNicknameSubmitted`가 받아 `player_name_audit`에 UNAUDITED 행을 넣는다. 발행처는 둘이다.

- `RouletteService`: 룰렛 결과 저장 시 당첨자 닉네임
- `UserProfileService`: 회원 닉네임 변경

닉네임 저장 트랜잭션 안에서 함께 커밋한다(#1618). 운영자가 허용한 단어이거나 이미 등록된 닉네임이면 건너뛴다.

### 회차

`NicknameAuditScheduler`가 `nickname-audit.cron` 주기로 `ProfanityAuditService.auditPending()`을 전용 실행기에 넘긴다. 분산 락으로 한 인스턴스만 돈다. UNAUDITED 행을 `batch-size`씩 읽어 `ProfanityAuditBatchProcessor.process()`에 넘기고, `max-run-duration`을 넘기면 남은 적체는 다음 회차로 미룬다.

배치 하나가 Gemini 호출 하나다. `GeminiNicknameAuditor`가 JSON 응답을 강제하고, 최근 운영자 피드백을 few-shot 예시로 프롬프트에 넣는다. `local`·`test` 프로파일은 `NoOpNicknameAuditor`가 대신 동작한다.

```text
UNAUDITED ─ Gemini 판정 ─┬─ flagged & confidence ≥ flagged-threshold → FLAGGED  비속어 조각을 사전에 등록해 즉시 차단
                         ├─ flagged & confidence < flagged-threshold → PENDING  운영자 검토 대기
                         └─ not flagged                              → CLEAN
                     호출 실패 ─┬─ 파싱 실패처럼 내용이 원인 → attempt_count +1, max-attempts 도달 시 DEAD_LETTER
                               └─ 네트워크·레이트리밋·타임아웃 → 배치 skip, 다음 회차 재시도
```

FLAGGED는 닉네임 전체가 아니라 `min-term-length` 이상인 비속어 조각을 `profanity_word`에 `AI_FLAGGED`로 넣는다. 등록되면 `ProfanityWordBlockedEvent`가 나가고, `UserNicknameCleanupService`가 같은 닉네임의 회원을 생성 닉네임으로 바꾼다.

### 운영자 피드백

`AdminProfanityController`가 `/admin/api/profanity/audits/{id}/allow|block` 요청을 받아 `ProfanityFeedbackService`를 부른다.

- `allow`: ALLOWED. 사전에서 `OPERATOR_ALLOWED`로 표시해 다시 검열 큐에 들지 않는다
- `block`: BLOCKED. 닉네임을 `MANUAL` 단어로 등록해 즉시 차단한다

둘 다 `player_name_feedback`에 남아 다음 회차 프롬프트의 few-shot 예시가 된다.

### 랭킹 닉네임 정리

`:admin`의 `RankingNicknameCollectionScheduler`가 월간 랭킹 상위 닉네임을 모아 `NicknamesCollectedEvent`로 발행한다. `:room`의 `PlayerNameRankingCleanupService`와 `:user`의 `UserNicknameCleanupService`가 사전에 걸리는 이름을 생성 닉네임으로 교체한다. 검열 큐와 별개로, 이미 차단된 단어가 랭킹에 남는 것을 막는 경로다.

### 상태

`NicknameAuditStatus` 7종: `UNAUDITED`, `FLAGGED`, `PENDING`, `CLEAN`, `ALLOWED`, `BLOCKED`, `DEAD_LETTER`.

DEAD_LETTER는 판정이 아니라 검열을 못 끝낸 상태다. 시도 횟수는 배치 전체에 함께 오르므로 문제 닉네임 하나가 같은 배치의 나머지를 데려간다. 되살릴 때는 SQL로 되돌린다.

```sql
UPDATE player_name_audit
SET status = 'UNAUDITED', attempt_count = 0
WHERE status = 'DEAD_LETTER';
```

## 설정

`app/src/main/resources/config/service.yml`의 `nickname-audit` 블록이 모델 `gemini-3.5-flash`와 `flagged-threshold`, `batch-size`, `feedback-injection-threshold`, `min-term-length`, `max-run-duration`, `max-attempts`, `request-timeout`, `cron`을 든다. `NicknameAuditProperties`가 바인딩한다.

Gemini 호출의 재시도와 속도 제한은 `config/resilience4j.yml`의 `geminiAudit` 인스턴스가 정한다. retry는 지수 백오프, ratelimiter는 13초에 1회다.

## 메트릭

`nickname.audit.*` 메트릭으로 Gemini 호출 시간, 파싱 실패, `status` 태그별 판정 분포, skip된 배치, DEAD_LETTER 전환, UNAUDITED 적체량을 낸다. 이름의 출처는 `ProfanityAuditService`·`ProfanityAuditBatchProcessor`의 등록 코드다.
