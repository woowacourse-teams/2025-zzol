---
description: 통합 테스트(IntegrationTest) 전용 규칙 — Docker·컨테이너·WebSocket·스트림. 공통은 testing.md.
paths:
  - "**/src/test/java/**/*IntegrationTest.java"
---

전체 컨벤션: `docs/conventions-test.md` → 통합 테스트 (WebSocket). 공통 체크: `testing.md`

## 이 계층에서 무엇을 (ADR-0033)

**테스트할 것**

- 플로우당 **현실적 해피패스 시나리오 1개** — 상태 전이(예: `DESCRIPTION→READY→PLAYING→DONE`) 관통 + WS 브로드캐스트
- **임계 와이어링**: 비동기 경로(Handler→Redis Stream→Consumer→Service→Notifier→broadcast)·실제 MySQL 거동
- **그 플로우를 진행하다 자연스럽게 생길 수 있는 예외의 처리** — 예: 블록쌓기에서 규칙대로 못 쌓는 입력이 들어와도 브로드캐스트되지 않고 게임이 계속 진행되는지. 별도 예외 매트릭스를 만드는 게 아니라, 한 플로우에서 실제 일어날 수 있는 예외 정도만 확인

**자제할 것 (하위로 내림)**

- 도메인 분기 매트릭스 열거 → domain/service (통합엔 대표 1개만)
- 같은 종료를 보려 수십 번 왕복 반복 → 게임을 테스트용으로 짧게 구성하거나 도메인 테스트로
- 페이즈/타이밍을 실시간 `sleep`으로 검증 → `CapturingScheduler`로 오케스트레이터 계층에서(`NunchiFlowOrchestratorTest` 참조)
- Redis/JPA 등 라이브러리·프레임워크 동작 자체 검증

## 통합 테스트 체크

- 베이스: 모듈 로컬 `{Module}IntegrationTest`(`coffeeshout.support.IntegrationTestSupport` 확장). `webEnvironment` 기본값은 **`MOCK`** — WebSocket/STOMP(`StandardWebSocketClient`)·`TestRestTemplate`·`WebTestClient`처럼 실제 TCP 소켓이 필요할 때만 `WebEnvironment.RANDOM_PORT`로 명시 오버라이드
- **Docker 필요**: MySQL·Valkey TestContainer가 뜬다(JVM=모듈별 독립 컨테이너, 물리 격리 — #1402). Docker가 꺼져 있으면 컨테이너 초기화 단계에서 전부 실패
- `subscribe()`는 `/topic/*` 구독 시 브로커 등록 완료까지 블록한다 — 구독 직후 동기 발행을 트리거해도 안전(subscribe→publish 레이스 방지, #1410). 별도 대기 호출 불필요
- **여러 상태가 순차 브로드캐스트되는 WS 토픽**에서 특정 상태 검증 시 위치 기반 `collector.get()` 금지 → 목표 상태가 나올 때까지 훑는 헬퍼(`awaitState` 참조 구현)로 읽는다. 중복 스냅샷·순서 밀림 flaky 방지
- **새 게임이 전용 스케줄러(`@Profile("!test")` 빈)·전용 Redis Stream 키를 쓰면** 테스트 미러를 같은 커밋에 추가한다 — 어디를 고칠지는 `game-test-mirror.md`. 빠뜨리면 `:app`의 `SchedulerMirrorTest`·`ConfigMirrorTest`가 잡는다(Docker 불필요)
