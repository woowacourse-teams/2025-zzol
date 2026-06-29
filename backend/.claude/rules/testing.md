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
- **새 게임이 전용 스케줄러(`@Profile("!test")` 빈)·전용 Redis Stream 키를 쓰면** 테스트 미러를 같은 커밋에 추가: `IntegrationTestConfig`에 동일 이름 `ShutDownTestScheduler` 빈, `application-test-base.yml`의 `redis.stream.keys`에 동일 키. 누락 시 IT가 `NoSuchBeanDefinitionException`(스케줄러) 또는 "메시지 미수신" 타임아웃(스트림). 상세 표: `docs/architecture.md` → 게임 SPI 패턴 → 전용 스케줄러·스트림 테스트 미러링

## :test-support 모듈

`TestContainerSupport`, `IntegrationTestSupport`, `ExceptionAssertions`, `TestStompSession` 등은 `:test-support` 모듈에서 제공한다.
각 도메인 모듈의 `build.gradle.kts`에 `testImplementation(project(":test-support"))`를 추가해 사용한다.

## 베이스 클래스 선택

| 종류 | 베이스 |
|------|--------|
| 순수 단위 테스트 | 없음 (순수 Java) |
| 서비스 테스트 | 모듈 로컬 `{Module}ServiceTest` 상속 (`coffeeshout.support.ServiceTest` 확장) |
| WebSocket / REST / Stream 통합 | 모듈 로컬 `{Module}IntegrationTest` 상속 (`coffeeshout.support.IntegrationTestSupport` 확장) |

`{Module}IntegrationTest`의 `webEnvironment` 기본값은 **`MOCK`**이다. `WebSocket/STOMP`(`StandardWebSocketClient`), `TestRestTemplate`, `WebTestClient` 등 실제 TCP 소켓이 필요한 경우에만 `WebEnvironment.RANDOM_PORT`로 명시 오버라이드한다.

두 베이스 클래스는 `src/test/java/coffeeshout/` 아래에 위치하며, Mock 빈은 `src/test/java/coffeeshout/config/ServiceTestConfig.java`에 선언한다.
`ApplicationEventPublisher`는 `coffeeshout.support.ServiceTest`가 `@MockitoBean`으로 제공 — `ServiceTestConfig`에 재선언 금지.
