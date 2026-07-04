---
description: 통합 테스트(IntegrationTest) 전용 규칙 — Docker·컨테이너·WebSocket·스트림. 공통은 testing.md.
paths:
  - "src/test/java/**/*IntegrationTest.java"
---

전체 컨벤션: `docs/conventions-test.md` → 통합 테스트 (WebSocket). 공통 체크: `testing.md`

## 통합 테스트 체크

- 베이스: 모듈 로컬 `{Module}IntegrationTest`(`coffeeshout.support.IntegrationTestSupport` 확장). `webEnvironment` 기본값은 **`MOCK`** — WebSocket/STOMP(`StandardWebSocketClient`)·`TestRestTemplate`·`WebTestClient`처럼 실제 TCP 소켓이 필요할 때만 `WebEnvironment.RANDOM_PORT`로 명시 오버라이드
- **Docker 필요**: MySQL·Valkey TestContainer가 뜬다(JVM=모듈별 독립 컨테이너, 물리 격리 — #1402). Docker가 꺼져 있으면 컨테이너 초기화 단계에서 전부 실패
- `subscribe()`는 `/topic/*` 구독 시 브로커 등록 완료까지 블록한다 — 구독 직후 동기 발행을 트리거해도 안전(subscribe→publish 레이스 방지, #1410). 별도 대기 호출 불필요
- **여러 상태가 순차 브로드캐스트되는 WS 토픽**에서 특정 상태 검증 시 위치 기반 `collector.get()` 금지 → 목표 상태가 나올 때까지 훑는 헬퍼(`awaitState` 참조 구현)로 읽는다. 중복 스냅샷·순서 밀림 flaky 방지
- **새 게임이 전용 스케줄러(`@Profile("!test")` 빈)·전용 Redis Stream 키를 쓰면** 테스트 미러를 같은 커밋에 추가: `IntegrationTestConfig`에 동일 이름 `ShutDownTestScheduler` 빈, `application-test-base.yml`의 `redis.stream.keys`에 동일 키. 누락 시 `NoSuchBeanDefinitionException`(스케줄러) 또는 "메시지 미수신" 타임아웃(스트림). 상세 표: `docs/architecture.md` → 게임 SPI 패턴 → 전용 스케줄러·스트림 테스트 미러링
