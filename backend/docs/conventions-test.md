# 테스트 컨벤션

## 작성 원칙

- 테스트 메서드명은 한글로 작성한다 (도메인 언어 사용)
- `@Nested`로 연관된 테스트를 시나리오 단위로 그룹화한다. 중첩 클래스명은 테스트 대상 상황을 설명한다
- 복수 검증은 `SoftAssertions`를 사용한다
- `@SpringBootTest` 없이 순수 Java로 작성한다 (아래 베이스 클래스/어노테이션 사용 시 제외)

## :test-support 모듈

테스트 인프라(TestContainers, 베이스 클래스, 공용 유틸)는 `:test-support` 모듈에서 제공한다.
각 도메인 모듈의 `build.gradle.kts`에 아래를 추가하면 MySQL·Valkey TestContainer, 베이스 클래스, `ExceptionAssertions`, `TestStompSession` 등을 전이 의존성으로 사용할 수 있다.

```kotlin
testImplementation(project(":test-support"))
```

공통 테스트 설정은 `:test-support`의 `application-test-base.yml`에 정의되며, 각 모듈의 `application-test.yml`과 함께 `test` 프로파일 적용 시 자동으로 로드된다.

## 테스트 종류별 베이스

| 종류 | 베이스 | 특징 |
|------|--------|------|
| 순수 단위 테스트 | 없음 (순수 Java) | 스프링 컨텍스트 없이 도메인 로직만 검증 |
| 서비스 테스트 | 모듈 로컬 `{Module}ServiceTest` 상속 | `coffeeshout.support.ServiceTest` 확장. `@SpringBootTest` + `@ActiveProfiles("test")` + `@Transactional` 상속. 모듈별 `ServiceTestConfig`에 외부 의존 Mock 선언 |
| WebSocket 통합 테스트 | 모듈 로컬 `{Module}IntegrationTest` 상속 + `TestStompSession` 사용 | `coffeeshout.support.IntegrationTestSupport` 확장. `RANDOM_PORT` 명시 오버라이드 + `test` 프로파일. `TestStompSession`으로 STOMP 구독·전송·메시지 수집 |
| 일반 통합 테스트 (REST, Stream 등) | 모듈 로컬 `{Module}IntegrationTest` 상속 | `coffeeshout.support.IntegrationTestSupport` 확장. 기본 `MOCK` + `test` 프로파일 + `@BeforeEach`/`@AfterEach` DB cleanup |

모든 모듈 로컬 베이스는 `coffeeshout.support.ServiceTest` 또는 `coffeeshout.support.IntegrationTestSupport`를 통해 `TestContainerSupport`를 상속하므로 MySQL·Valkey TestContainer가 자동으로 구동된다.

### 모듈 로컬 베이스 클래스

각 도메인 모듈은 `src/test/java/coffeeshout/` 아래에 두 개의 베이스 클래스를 정의한다.

**서비스 테스트용** — `@SpringBootTest`·`@ActiveProfiles`·`@Transactional`·`MockEventPublisherConfig` 모두 부모에서 상속한다.

```java
@Import(ServiceTestConfig.class)
public abstract class {Module}ServiceTest extends coffeeshout.support.ServiceTest {
}
```

**통합 테스트용** — 부모의 auto-detection을 대신해 모듈 테스트 앱 클래스를 명시한다. 기본값은 `MOCK`이며, WebSocket/STOMP·`TestRestTemplate`·`WebTestClient` 등 실제 TCP 소켓이 필요한 경우에만 `RANDOM_PORT`로 오버라이드한다.

```java
@SpringBootTest(classes = {Module}TestApplication.class, webEnvironment = WebEnvironment.MOCK)
@Import(ServiceTestConfig.class)
public abstract class {Module}IntegrationTest extends coffeeshout.support.IntegrationTestSupport {
}
```

WebSocket/STOMP 또는 `TestRestTemplate` · `WebTestClient`를 사용하는 모듈은 `RANDOM_PORT`로 명시 오버라이드한다.

```java
@SpringBootTest(classes = {Module}TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(ServiceTestConfig.class)
public abstract class {Module}IntegrationTest extends coffeeshout.support.IntegrationTestSupport {
}
```

모듈 외부 의존(인터페이스 구현체가 없는 포트 등)은 `src/test/java/coffeeshout/config/ServiceTestConfig.java`에 `@TestConfiguration`으로 선언한다.
`ApplicationEventPublisher` Mock은 부모(`coffeeshout.support.ServiceTest`)가 `@MockitoBean`으로 이미 제공하므로 `ServiceTestConfig`에 재선언하지 않는다.

> **주의 — 테스트 클래스 내부 `@TestConfiguration`은 자동 감지되지 않는다**
>
> `@SpringBootTest`가 **부모 클래스**(`{Module}IntegrationTest` 등)에 선언된 경우,
> 자식 테스트 클래스에 작성한 `static class` 형태의 내부 `@TestConfiguration`은
> Spring Boot Test가 자동으로 로드하지 않는다.
>
> **해결**: 빈 정의를 독립 파일(`XxxTestConfig.java`)로 분리하고 `@Import`로 명시한다.
>
> ```java
> // 잘못된 예 — 자동 감지 안 됨
> class MyIntegrationTest extends RoomModuleIntegrationTest {
>     @TestConfiguration
>     static class TestConfig { /* 빈 선언 */ }
> }
>
> // 올바른 예 — 독립 파일로 분리 후 Import
> @Import(MyTestConfig.class)
> class MyIntegrationTest extends RoomModuleIntegrationTest { }
> ```

## 픽스처

테스트 데이터를 직접 생성하지 않고 픽스처를 통해 재사용한다. 메서드명은 한글 도메인 용어를 사용한다.

### 위치

- 모듈 간 공유: `src/testFixtures/java/coffeeshout/fixture/`
- 모듈 내부 전용: `src/test/java/coffeeshout/fixture/`

### 유형별 네이밍

| 유형         | 클래스명 패턴          | 특징                            |
|------------|------------------|-------------------------------|
| 도메인 객체 팩토리 | `*Fixture`       | 순수 Java 정적 팩토리, 스프링 컨텍스트 불필요  |
| DB 영속화 헬퍼  | `TestDataHelper` | `@Component`, 통합 테스트에서 DI로 사용 |
| 경량 대체 구현   | `*Fake`          | 실제 로직을 갖지만 외부 의존을 제거한 구현      |
| 최소 더미 구현   | `*Dummy`         | 인터페이스 계약을 최소한으로 충족, 로직 없음     |
| 반환값 제어     | `Stub*`          | 특정 메서드의 반환값을 고정하거나 무력화        |

위 5가지 패턴 외 클래스명은 사용하지 않는다.

## 통합 테스트 (WebSocket)

`IntegrationTestSupport`를 상속하고 `TestStompSession`(`coffeeshout.support`)을 직접 생성해서 사용한다.
`subscribe()`는 `MessageCollector`를 반환하며, `get()`으로 메시지를 Awaitility 기반으로 대기한다.

```java
TestStompSession session = new TestStompSession(stompSession, principalName);
MessageCollector collector = session.subscribe("/topic/...");
session.send("/app/...", requestPayload);

MessageResponse response = collector.get();          // 기본 5초 대기
MessageResponse response = collector.get(3, TimeUnit.SECONDS);

collector.assertNoMessage();                         // 메시지 없음 검증 (기본 1초)
```

### subscribe()는 등록 완료까지 블록한다 (subscribe→publish 레이스 방지)

STOMP SUBSCRIBE는 비동기라 `session.subscribe()`(Spring)는 등록 완료를 기다리지 않고 즉시 반환한다. 구독 직후 동기적으로 브로드캐스트를 트리거하면(예: 게임 시작), 등록 전에 발행된 가장 이른 브로드캐스트가 구독자 0명에게 전달되어 유실되고, 이후 메시지가 한 칸씩 밀려 `get()`이 타임아웃한다. 부하가 큰 CI에서만 간헐 재현되는 flaky의 원인이었다(#1410).

`TestStompSession.subscribe()`는 이를 막기 위해 **`/topic/*` 구독은 반환 전 브로커 등록 완료까지 블록한다** — 별도의 대기 호출이 필요 없다. (인메모리 SimpleBroker는 SUBSCRIBE에 RECEIPT를 보내지 않으므로, 방금 구독한 그 토픽으로 고유 토큰 ping을 도착할 때까지 재전송한다. 브로커는 등록된 구독자에게만 전달하므로 ping 도착이 곧 등록 증거다. inbound 채널이 멀티스레드라 프레임 순서가 보장되지 않아도 성립하는 결정론적 방식이다. 단, 등록 확인용 SEND가 추가로 발생하므로 인바운드 메트릭 카운트·세션 rate-limit 예산에 잡힌다. ping은 일반 메시지 큐에서 걸러져 단언을 오염시키지 않는다.)

`/user/*`·`/queue/*` 목적지는 echo round-trip이 성립하지 않아(UserDestination 변환 등) 배리어를 적용하지 않고 즉시 반환한다. 이들 구독은 게임 시작 같은 동기 발행 레이스 대상이 아니다.

```java
MessageCollector stateResponses = session.subscribe("/topic/room/{joinCode}/.../state"); // 등록 완료 후 반환
startGame();                                                                              // 즉시 첫 브로드캐스트를 발행해도 안전
```

## 비동기·시간 의존 검증

`Thread.sleep`은 사용하지 않는다. 고정 대기는 실행 환경에 따라 테스트가 flaky해지는 원인이 된다.
시간이 지난 뒤 상태 변화를 검증해야 할 때는 Awaitility를 사용한다.

```java
await().atMost(Duration.ofSeconds(3))
        .until(() -> store.tryAcquire(ip));
```

## 검증 스타일

검증이 여러 개일 때는 subject(대상)가 같은지 다른지에 따라 스타일을 선택한다.

**같은 subject에 대한 복수 검증 → 체이닝**

```java
assertThat(result.getFirst())
        .returns("철수", BlockStackingTopPlayerResponse::playerName)
        .returns(25L, BlockStackingTopPlayerResponse::maxFloor);
```

**다른 subject에 대한 복수 검증 → `SoftAssertions`**

```java
SoftAssertions.assertSoftly(softly -> {
    softly.assertThat(result).hasSize(3);
    softly.assertThat(result.get(0).playerName()).isEqualTo("철수");
    softly.assertThat(result.get(1).playerName()).isEqualTo("영희");
});
```

`assertSoftly`는 람다 종료 시 자동으로 `assertAll()`을 호출해 실패를 누적 보고한다.
단일 검증이거나 체이닝으로 표현 가능한 경우에는 `SoftAssertions`를 쓰지 않는다.

## 예외 검증

`CoffeeShoutException` 계열 예외는 `ExceptionAssertions.assertCoffeeShoutException`을 사용한다.

```java
import static coffeeshout.support.ExceptionAssertions.assertCoffeeShoutException;

assertCoffeeShoutException(
        () -> service.someMethod(),
        RoomErrorCode.ROOM_FULL
);
```

예외 타입(`CoffeeShoutException` 여부)과 `ErrorCode`를 한 번에 검증한다. `assertThatThrownBy(...).isInstanceOf(...).satisfies(...)` 체인을 직접 작성하지 않는다.

`IllegalArgumentException`, `IllegalStateException` 등 도메인 외 예외는 기존 `assertThatThrownBy`를 그대로 사용한다.

## 테스트 프로파일

`test` 프로파일 적용 시 `:test-support`의 `application-test-base.yml`과 각 모듈의 `application-test.yml`이 자동 적용된다.

- 타이밍 값이 500ms~2s로 단축됨
- DB: MySQL TestContainer 사용 (Flyway 비활성화)
- Valkey(Redis): TestContainers로 실제 컨테이너 구동
- Redisson 제외
