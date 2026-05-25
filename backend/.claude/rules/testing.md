---
description: 테스트 코드 작성 시 핵심 체크 항목. 전체 컨벤션은 docs/conventions-test.md 참고.
paths:
  - "src/test/java/**/*.java"
---

전체 컨벤션: `docs/conventions-test.md`

## 자주 놓치는 항목

- 테스트 메서드명은 한글
- 복수 검증은 `SoftAssertions`
- `Thread.sleep` 금지 → Awaitility
- 테스트 데이터 직접 생성 금지 → 픽스처 사용. 클래스명은 반드시 5가지 패턴 중 하나: `*Fixture` / `TestDataHelper` / `*Fake` / `*Dummy` / `Stub*`
- `CoffeeShoutException` 계열은 `assertCoffeeShoutException` 사용. `assertThatThrownBy` 체인 직접 작성 금지

## 베이스 클래스 선택

| 종류               | 베이스                                                             |
|------------------|-----------------------------------------------------------------|
| 순수 단위 테스트        | 없음 (순수 Java)                                                    |
| 서비스 테스트          | `ServiceTest` 상속                                                |
| WebSocket 통합     | `WebSocketIntegrationTestSupport` 상속 (`@IntegrationTest` 중복 금지) |
| REST / Stream 통합 | `IntegrationTestSupport` 상속                                   |
