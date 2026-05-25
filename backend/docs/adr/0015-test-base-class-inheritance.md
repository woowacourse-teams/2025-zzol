# ADR-0015: `:test-support` 베이스 테스트 클래스 계층 설계

## 상태

적용됨 (2026-05-24)

## 컨텍스트

ADR-0013에서 도메인 모듈 독립 테스트 실행 전략을 도입했다.
각 모듈이 자체 `@SpringBootTest` 컨텍스트를 띄울 수 있어야 하고,
동시에 공통 설정(`TaskScheduler` mock, `ApplicationEventPublisher` mock, TestContainer)이 모든 모듈에서 중복 없이 제공되어야 한다.

`:app`에만 존재하던 `ServiceTest`를 `:test-support`로 이동하면서,
세 가지 Spring 동작 원리가 결합되어 이 구조가 가능해졌다.

## 결정

`:test-support`에 `ServiceTest`와 `IntegrationTestSupport` 추상 클래스를 두고,
각 모듈 또는 `:app`에서 상속해 도메인 전용 설정을 추가한다.

```java
// :test-support — 도메인 무관 공통 인프라
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import({CommonTestSchedulerConfig.class, MockEventPublisherConfig.class})
public abstract class ServiceTest extends TestContainerSupport {
    @MockitoBean
    protected ApplicationEventPublisher eventPublisher;
}

// :app — 게임 도메인 빈 mock 추가
@Import(ServiceTestConfig.class)
public abstract class ServiceTest extends coffeeshout.support.ServiceTest {
}

// :room 테스트 — test-support 직접 상속
class RedisJoinCodeRepositoryTest extends coffeeshout.support.ServiceTest { ... }
```

## 동작 원리

### 1. `@SpringBootTest`는 실행 모듈의 클래스패스에서 애플리케이션을 찾는다

`@SpringBootTest`에 `classes`를 지정하지 않으면 Spring TestContext Framework가
**테스트 클래스의 클래스패스**를 탐색해 `@SpringBootApplication` 또는
`@SpringBootConfiguration`이 붙은 클래스를 자동으로 찾는다.

`:test-support`는 `@SpringBootApplication` 클래스가 없는 라이브러리 모듈이다.
`@SpringBootTest`가 `:test-support` 소스에 선언되어 있더라도,
실제 테스트를 **실행하는 모듈**의 클래스패스가 기준이 된다.

```text
:room:test 에서 ServiceTest 상속
  → 클래스패스에 RoomTestApplication(@SpringBootApplication) 존재
  → Spring이 RoomTestApplication으로 컨텍스트 부트스트랩

:app:test 에서 ServiceTest 상속
  → 클래스패스에 Application(@SpringBootApplication) 존재
  → Spring이 Application으로 컨텍스트 부트스트랩
```

같은 `@SpringBootTest` 선언이 모듈마다 다른 애플리케이션을 부트스트랩하는 이유다.

### 2. Spring TestContext Framework는 클래스 계층 전체에서 어노테이션을 수집한다

Java 언어 레벨에서 `@Import`는 `@Inherited`가 없으므로 서브클래스에 상속되지 않는다.
그러나 Spring의 `MergedAnnotations` API는 클래스 계층을 재귀 탐색하여
상위 클래스에 붙은 어노테이션까지 모두 수집한다.

따라서 `:app`의 `ServiceTest`에 `@Import(ServiceTestConfig.class)`를 추가하면
부모인 `:test-support`의 `@Import(CommonTestSchedulerConfig.class)`와 합산된다.

```text
:app ServiceTest 가 extends coffeeshout.support.ServiceTest 할 때 적용되는 @Import:
  - coffeeshout.support.ServiceTest      → @Import({CommonTestSchedulerConfig, MockEventPublisherConfig})
  - coffeeshout.support.app.ServiceTest  → @Import(ServiceTestConfig.class)
  → 두 Config 모두 컨텍스트에 등록됨
```

`:room`의 테스트가 `coffeeshout.support.ServiceTest`를 직접 상속하면
`@Import(CommonTestSchedulerConfig.class)`만 적용되고 `ServiceTestConfig`는 포함되지 않는다.

### 3. `@MockitoBean` 필드도 클래스 계층에서 탐색된다

Spring TestContext Framework는 테스트 클래스 및 **모든 상위 클래스의 필드**를 스캔한다.
`protected` 접근자도 포함된다.

`:test-support`의 `ServiceTest`에 선언된 `@MockitoBean ApplicationEventPublisher eventPublisher`는
이를 상속하는 모든 테스트 클래스에서 `ApplicationEventPublisher`를 mock으로 교체한다.
하위 클래스에서 `eventPublisher` 필드를 직접 참조해 `verify(eventPublisher, ...)` 검증이 가능하다.

## 모듈별 적용 구조

```text
TestContainerSupport (MySQL + Redis 컨테이너 관리)
  │
  ├── coffeeshout.support.ServiceTest              ← :test-support
  │     @SpringBootTest, @Transactional
  │     @Import({CommonTestSchedulerConfig, MockEventPublisherConfig})
  │     @MockitoBean ApplicationEventPublisher
  │     │
  │     ├── coffeeshout.support.app.ServiceTest  ← :app (게임 빈 mock 추가)
  │     │     @Import(ServiceTestConfig)       ← FlowScheduler, SimpMessagingTemplate mock
  │     │
  │     └── :room, :user, ... 테스트 직접 상속  ← 게임 빈 없이 공통 인프라만
  │
  └── coffeeshout.support.IntegrationTestSupport   ← :test-support
        @SpringBootTest(RANDOM_PORT)
        @Import(CommonTestSchedulerConfig)
        @BeforeEach cleanDatabase()
        │
        └── coffeeshout.support.app.IntegrationTestSupport  ← :app (게임 빈 mock 추가)
              @Import(IntegrationTestConfig)
```

## 고려한 대안

### 대안 A: 각 모듈이 자체 ServiceTest를 선언

- 장점: 의존성이 명시적
- 단점: TestContainer 설정, `@MockitoBean`, `@SpringBootTest` 조합이 모듈마다 중복됨
- 기각 이유: 스케줄러 빈 추가 시 모든 모듈의 ServiceTest를 각각 수정해야 함

### 대안 B: `@SpringBootTest`를 test-support에 두지 않고 각 모듈에서 선언

- 장점: 어노테이션 동작이 단순 명확
- 단점: 대안 A와 동일하게 중복 발생
- 기각 이유: `@Import` 합산 메커니즘을 활용하면 중복 없이 계층적으로 설정을 조합할 수 있음

## 결과

- `:test-support`에 `@SpringBootTest`를 선언하더라도 실행 모듈의 클래스패스 기준으로 동작하므로 각 모듈이 독립적인 Spring 컨텍스트를 유지한다
- 새 도메인 모듈을 마이그레이션할 때 `coffeeshout.support.ServiceTest`를 상속하는 것만으로 공통 인프라(`TaskScheduler` mock, `ApplicationEventPublisher` mock, TestContainer)가 자동으로 제공된다
- 게임 도메인 빈 mock(`ServiceTestConfig`)은 `:app`의 `ServiceTest`에만 존재하므로 도메인 모듈 테스트가 `:game`에 의존하지 않아도 된다
