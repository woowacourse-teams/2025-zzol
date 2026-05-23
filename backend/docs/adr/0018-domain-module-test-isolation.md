# ADR-0018: 도메인 모듈 테스트 독립 실행 전략

## 상태

적용됨 (2026-05-23)

## 컨텍스트

ADR-0011의 멀티 모듈 전환 이후, 통합 테스트가 모두 `:app:test` 에 집중되어 있었다.
`:app` 은 모든 모듈을 조합하는 composition root 이므로 테스트 컨텍스트에서도 전체 빈이 로드된다.
그러나 이 구조는 두 가지 문제를 야기한다.

1. **테스트 범위 불명확**: `:room` 도메인 변경이 `:app:test` 실패를 유발하지만 어느 계층의 문제인지 추적이 어렵다.
2. **빌드 결합도**: 도메인 모듈 단독 빌드 시 통합 테스트를 실행할 수 없다.

목표: 각 도메인 모듈이 자체 테스트를 독립적으로 실행할 수 있어야 한다.

## 결정

각 도메인 모듈의 `src/test/` 에 **TestApplication + TestConfig 패턴**을 도입한다.

### TestApplication (예: `RoomTestApplication`)

```java
@EntityScan(basePackages = "coffeeshout")
@EnableJpaRepositories(basePackages = "coffeeshout")
@ConfigurationPropertiesScan(basePackages = {
        "coffeeshout.room", "coffeeshout.user", "coffeeshout.auth",
        "coffeeshout.friend", "coffeeshout.websocket", "coffeeshout.global"
})
@SpringBootApplication(scanBasePackages = {
        "coffeeshout.room", "coffeeshout.user", "coffeeshout.auth",
        "coffeeshout.friend", "coffeeshout.websocket", "coffeeshout.global"
})
public class RoomTestApplication { ... }
```

- `scanBasePackages`: 해당 도메인 + 공통 의존 모듈만 지정한다.
- `@EntityScan` / `@EnableJpaRepositories`: `"coffeeshout"` 루트 전체를 스캔해 JPA 엔티티·리포지토리를 찾는다. `scanBasePackages` 와 범위가 다르므로 별도 명시가 필요하다.
- `@ConfigurationPropertiesScan`: `scanBasePackages` 와 동일한 범위로 제한한다. 루트 `"coffeeshout"` 로 설정하면 `:game` JAR의 `BlindTimerProperties` 같은 다른 모듈 설정이 유효성 검사 실패를 일으킨다.

### TestConfig (`*TestConfig.java`)

`:app` 에만 존재하는 빈은 테스트 컨텍스트에서 mock으로 대체한다.
`@Configuration` 으로 선언해 `RoomTestApplication` 의 컴포넌트 스캔에서 자동 등록되도록 한다.

**`@Configuration` vs `@TestConfiguration` 선택**

`@TestConfiguration` 은 컴포넌트 스캔에서 자동 발견되지 않고 `@Import` 또는
`@SpringBootTest` inner static class 로만 활성화된다.
`:room:test` 는 단일 `RoomTestApplication` 부트스트랩을 사용하고 모든 통합 테스트가
동일한 mock 빈을 필요로 하므로, `@Configuration` + 자동 스캔으로 단순화한다.
테스트 클래스마다 다른 mock 셋이 필요해지면 `@TestConfiguration` + `@Import` 로 전환한다.

**`@Primary` 의 방어적 사용**

mock 빈의 경쟁 대상(`:admin` 구현체, `:app` `MiniGameFactoryConfig`)은 모두
`RoomTestApplication.scanBasePackages` 범위 밖이므로 현재 충돌이 없다.
향후 scan 범위 확대나 새 의존성 추가에 대비해 `@Primary` 를 방어적으로 유지한다.

현재 `:room:test` 가 대체하는 빈:

| 원본 위치 | 이유 | 대체 방법 |
|---|---|---|
| `:admin` — `ReportAnonymizationPort` 구현체 | `:room` → `:admin` 순환 의존 불가 | `Mockito.mock()` |
| `:app` — `MiniGameFactoryConfig` | `:app` 은 `:room` 의 상위 모듈 | 각 `MiniGameType` 에 mock 팩토리 등록 |
| `:app` — `RestExceptionHandler` | 동일 이유 | `:web` 모듈의 실제 핸들러로 대체됨 (ADR-0019 적용 완료) |

### 공통 설정 — `application-test-base.yml`

`:test-support` 모듈의 `src/main/resources/application-test-base.yml` 에 모든 모듈이 공유하는 테스트 설정을 집중 관리한다.

각 모듈의 `application-test.yml` 에서 임포트한다.

```yaml
spring:
  config:
    import: classpath:application-test-base.yml
```

모듈별 설정(예: `room.removalDelay`)은 각 모듈의 `application-test.yml` 에 선언한다.

`redis.stream.keys` 처럼 여러 모듈에 걸친 설정은 컴파일 안전성과 관리 편의를 위해
base에 일괄 포함한다. 게임별 스트림 키(`:game`), Oracle Object Storage(`:room`),
Report rate-limit(`:admin`) 등 도메인 한정 설정도 현재는 base에 두어 누락으로 인한
컨텍스트 로드 실패를 방지한다. 각 모듈 마이그레이션 완료 후 점진적으로 모듈별 yml로 이동한다.

### TestContainers 버전 고정

Spring Boot BOM 이 트랜지티브 의존으로 `testcontainers` 를 1.x 로 다운그레이드한다.
1.x 는 Windows Named Pipe Docker 연결을 지원하지 않아 `Could not find a valid Docker environment` 오류가 발생한다.

각 도메인 모듈의 `build.gradle.kts` 에서 직접 선언해 BOM 보다 우선 적용되도록 한다.

```kotlin
val testcontainersVersion = rootProject.extra["testcontainers"] as String
testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
```

`resolutionStrategy.force` 는 `io.spring.dependency-management` 플러그인에 의해 무시되므로 사용하지 않는다.

## 고려한 대안

### 대안 A: `:app:test` 에서 모든 통합 테스트 실행 유지

- 장점: 기존 구조를 유지하므로 변경 최소화
- 단점: 도메인 모듈 단독 빌드 시 통합 테스트 불가. 실패 원인 추적 어려움
- 기각 이유: 멀티 모듈 전환의 목적(모듈 격리)에 반함

### 대안 B: `RestExceptionHandler` 를 공유 모듈로 이동

- **`:common`**: `:common` 은 Spring MVC 없이 사용 가능한 수준으로 유지하므로 기각
- **`:infra`**: `:infra` 는 이미 Spring에 의존하나, JPA·Redis·Outbox 등 저장소 계층이 목적이다.
  HTTP 예외 처리를 같이 두면 웹 컨텍스트 없이 `:infra` 만 쓰는 모듈(배치, 컨슈머)에
  불필요한 Spring MVC 의존이 포함된다.
- **신설 `:web` 모듈**: `:infra` 위에 위치하는 공유 HTTP 인프라 모듈로 ADR-0019에 별도 제안.
  이 방안이 채택되면 `TestRestExceptionHandler` 는 제거된다.

### 대안 C: `@WebMvcTest` 슬라이스 테스트로 교체

- 장점: 컨트롤러 레이어만 격리하여 빠른 실행
- 단점: Redis·JPA 등 전체 컨텍스트가 필요한 시나리오 검증 불가
- 기각 이유: 현재 통합 테스트 시나리오는 실제 Redis Stream·DB 상호작용을 검증한다

### 대안 D: `MiniGameFactory` mock 을 `:test-support` 에 집중 관리

현재 각 도메인 모듈의 `TestConfig` 에서 `MiniGameFactory` mock 을 직접 구성하고 있으나,
이를 `:test-support` 에 공유 `@Configuration` 으로 두는 방안이다.

```text
:game-api   ← MiniGameType, MiniGameFactory (인터페이스)
  ↑
:test-support  ← MockMiniGameFactoryConfig (@Configuration, mock EnumMap 제공)
  ↑
:room:test  :user:test  ...  (testImplementation(project(":test-support")))
```

- **장점**: `:room:test` 에서 `testImplementation(project(":game"))` 제거 가능.
  mock 구성 로직이 `:test-support` 한 곳에 집중되어 `MiniGameType` 추가 시 모든 모듈에 자동 반영된다.
- **단점**: `:test-support` 가 `src/main/resources`(yml) 중심에서 Spring Bean 설정을 포함하는 모듈로 확장된다.
  `:game-api` 의존이 `:test-support` 에 추가된다.
- **현재 상태**: `:room:test` 의 `build.gradle.kts` 에 `testImplementation(project(":game"))` 이 있어
  ADR-0011 OCP 원칙(`:room` 은 `:game-api` 추상만 알아야 함)을 테스트 수준에서 위반하고 있다.
  이 방안은 해당 위반을 해소하는 가장 자연스러운 경로이며, 다른 도메인 모듈 마이그레이션 시점에 함께 도입을 검토한다.
- **`:web` 모듈과의 유사성**: `:web` 이 HTTP 횡단 관심사를 공유 모듈로 빼내어 각 도메인 모듈의 test stub 복제를
  방지하는 것과 동일한 원리다 (ADR-0019 참조).

## 결과

### 긍정적 효과

- `:room:test` 단독 실행으로 room 도메인 변경의 회귀를 즉시 검출할 수 있다
- `application-test-base.yml` 로 공통 설정 중복이 제거된다
- 다른 도메인 모듈도 동일 패턴으로 마이그레이션할 수 있다

### 부정적 효과 / 트레이드오프

- `RestExceptionHandler` mock 은 ADR-0019(`:web` 모듈) 구현 전까지 각 모듈 테스트에 분산된다.
  HTTP 응답 정책이 바뀌면 프로덕션 핸들러와 테스트 mock 을 함께 수정해야 한다.
- `MiniGameFactoryConfig` mock 은 `:room` 이 `:game-api` 추상만 알고 `:game` 구체 팩토리를
  모르는 ADR-0011 OCP 원칙을 테스트 레벨에서 유지하기 위한 비용이다.
  `MiniGameDummy` 의 `CardGameScore`(`:game`) 의존을 익명 `MiniGameScore` 서브클래스(`:game-api`)로
  교체하고 `testImplementation(project(":game"))` 을 제거하여 OCP 위반이 해소됐다.
- TestContainers 버전을 도메인 모듈마다 직접 선언해야 한다.

### 미적용 모듈

현재 `:room` 만 이 패턴을 적용했다.
나머지 도메인 모듈(`:user`, `:game`, `:websocket`, `:admin`, `:zzolbot`) 은 아직 `:app:test` 에 의존한다.
마이그레이션 순서는 팀 협의로 결정한다.
