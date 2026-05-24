# ADR-0016: 게임 통합 테스트 FlowScheduler 실제 구현 주입 전략

## 상태

적용됨 (2026-05-24)

## 컨텍스트

ADR-0013으로 `:test-support` 모듈에 `BaseIntegrationTestConfig`를 도입하고, `@IntegrationTest` 어노테이션이 이를 import하도록 구성했다.
`BaseIntegrationTestConfig`는 `blockStackingFlowScheduler`, `cardGameFlowScheduler`, `ladderFlowScheduler`를 `Mockito.mock(FlowScheduler.class)`로 등록한다.

이 mock은 두 가지 역할을 한다.

1. **DI 플레이스홀더**: `@SpringBootTest`는 전체 애플리케이션 컨텍스트를 로드한다. `BlockStackingFlowOrchestrator` 등은 `blockStackingFlowScheduler` 빈을 주입받아야 하는데, 프로덕션 설정(`BlockStackingTaskSchedulerConfig`)은 `@Profile("!test")`라 test 프로파일에서 빈이 생성되지 않는다. mock이 없으면 컨텍스트 초기화 자체가 실패한다.
2. **비게임 통합 테스트 격리**: `PatchNoteControllerTest`, `UserWithdrawalControllerTest` 등 게임 흐름을 실행하지 않는 테스트는 FlowScheduler를 실제로 호출하지 않으므로 mock으로 충분하다.

그러나 `BlockStackingIntegrationTest`, `CardGameIntegrationTest`, `LadderIntegrationTest`는 FlowScheduler가 실제로 동작해야 한다.
게임 흐름(PREPARE → PLAYING → DONE)이 실제로 실행되어야 상태 전환 메시지를 WebSocket으로 수신할 수 있기 때문이다.

mock의 `schedule()`은 기본적으로 `null`을 반환하고, `FlowOrchestrator`에서 반환값에 `.andThen()`을 호출하면 NPE가 발생한다.

## 결정

`GameWebSocketIntegrationTestSupport`를 `:app` 테스트 소스(`app/src/test`)에 신규 생성한다.
이 클래스는 `RoomWebSocketTestSupport`를 상속하고 `@Import(IntegrationTestConfig.class)`를 선언한다.

`IntegrationTestConfig`는 `CompletableFutureFlowScheduler(ShutDownTestScheduler)` 조합으로 세 FlowScheduler 빈을 등록한다.
`application-test.yml`에 `spring.main.allow-bean-definition-overriding: true`를 설정해, `BaseIntegrationTestConfig`의 mock보다 나중에 처리되는 `IntegrationTestConfig` 빈이 override되도록 한다.

게임 FlowScheduler가 실제로 필요한 세 테스트는 `RoomWebSocketTestSupport` 대신 `GameWebSocketIntegrationTestSupport`를 상속한다.

```text
@IntegrationTest (BaseIntegrationTestConfig — mock FlowScheduler 등록)
    ↓ 상속
IntegrationTestSupport
    ↓ 상속
WebSocketIntegrationTestSupport
    ↓ 상속 (room testFixtures)
RoomWebSocketTestSupport
    ↓ 상속 (app/src/test, @Import(IntegrationTestConfig.class))
GameWebSocketIntegrationTestSupport
    ↓ 상속
BlockStackingIntegrationTest / CardGameIntegrationTest / LadderIntegrationTest
```

Spring 컨텍스트 처리 순서상 클래스 레벨 `@Import`는 상위 어노테이션의 `@Import`보다 나중에 처리되므로, `IntegrationTestConfig` 빈이 `BaseIntegrationTestConfig` mock을 덮어쓴다.

## 대안

### 대안 A: BaseIntegrationTestConfig에서 직접 실제 구현 사용

`BaseIntegrationTestConfig`의 FlowScheduler 빈을 `CompletableFutureFlowScheduler`로 교체한다.
이렇게 하면 override 없이 모든 통합 테스트에서 실제 FlowScheduler가 주입된다.

거부 이유: `CompletableFutureFlowScheduler`는 `:game` 모듈에 위치하는데, `:game`이 이미 `testImplementation(project(":test-support"))`로 `:test-support`에 의존하고 있어 순환 의존이 발생한다.

### 대안 B: 각 게임 테스트 클래스에 @Import 직접 추가

`BlockStackingIntegrationTest` 등 각각에 `@Import(IntegrationTestConfig.class)`를 추가한다.

거부 이유: 동일한 설정이 세 클래스에 분산되어 신규 게임 통합 테스트 추가 시 누락 위험이 있다.

## 결과

- `BaseIntegrationTestConfig`는 변경 없이 DI 플레이스홀더 역할을 유지한다.
- 게임 통합 테스트만 `GameWebSocketIntegrationTestSupport`를 상속해 실제 FlowScheduler를 주입받는다.
- 신규 게임 WebSocket 통합 테스트는 `GameWebSocketIntegrationTestSupport`를 상속하는 것으로 관례화된다.
- `allow-bean-definition-overriding: true`는 테스트 전용 설정(`application-test.yml`)에만 적용되므로 프로덕션 컨텍스트에 영향을 주지 않는다.
