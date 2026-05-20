# 테스트 컨벤션

## 작성 원칙

- 테스트 메서드명은 한글로 작성한다 (도메인 언어 사용)
- `@Nested`로 연관된 테스트를 시나리오 단위로 그룹화한다. 중첩 클래스명은 테스트 대상 상황을 설명한다
- 복수 검증은 `SoftAssertions`를 사용한다
- `@SpringBootTest` 없이 순수 Java로 작성한다 (아래 베이스 클래스/어노테이션 사용 시 제외)

## 테스트 종류별 베이스

| 종류                         | 베이스                                         | 특징                                                                                                |
|----------------------------|---------------------------------------------|---------------------------------------------------------------------------------------------------|
| 순수 단위 테스트                  | 없음 (순수 Java)                                | 스프링 컨텍스트 없이 도메인 로직만 검증                                                                            |
| 서비스 테스트                    | `ServiceTest` 추상 클래스 상속                     | `@SpringBootTest` + `test` 프로파일 + `@Transactional`. `ApplicationEventPublisher`는 MockitoBean으로 제공 |
| WebSocket 통합 테스트           | `WebSocketIntegrationTestSupport` 추상 클래스 상속 | `RANDOM_PORT` + `test` 프로파일 + `@Transactional` 포함. `@IntegrationTest` 추가 불필요                      |
| 일반 통합 테스트 (REST, Stream 등) | `@IntegrationTest` 어노테이션                    | `RANDOM_PORT` + `test` 프로파일 + `@Transactional`                                                    |

모든 베이스는 `TestContainerConfig`를 import하므로 Valkey TestContainer가 자동으로 구동된다.

## 픽스처

`src/test/java/coffeeshout/fixture/`에 픽스처 클래스를 모아 관리한다. 테스트 데이터를 직접 생성하지 않고 픽스처를 통해 재사용한다. 메서드명은 한글 도메인 용어를 사용한다.

## 통합 테스트 (WebSocket)

`WebSocketIntegrationTestSupport`를 상속하면 STOMP 세션 유틸(`createSession`, `assertMessage` 등)이 제공된다. `assertMessage`는 JSONAssert(lenient mode)로 비교한다. `@IntegrationTest`를 함께 붙이면 설정이 중복되므로 사용하지 않는다.

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
import static coffeeshout.ExceptionAssertions.assertCoffeeShoutException;

assertCoffeeShoutException(
        () -> service.someMethod(),
        RoomErrorCode.ROOM_FULL
);
```

예외 타입(`CoffeeShoutException` 여부)과 `ErrorCode`를 한 번에 검증한다. `assertThatThrownBy(...).isInstanceOf(...).satisfies(...)` 체인을 직접 작성하지 않는다.

`IllegalArgumentException`, `IllegalStateException` 등 도메인 외 예외는 기존 `assertThatThrownBy`를 그대로 사용한다.

## 테스트 프로파일

`test` 프로파일 적용 시 `application-test.yml`이 자동 적용된다.
- 타이밍 값이 500ms~2s로 단축됨
- DB: H2 인메모리 사용 (Flyway 비활성화)
- Valkey(Redis): TestContainers로 실제 컨테이너 구동 (`TestContainerConfig`)
- Redisson 제외
