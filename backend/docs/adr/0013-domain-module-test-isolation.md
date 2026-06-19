# ADR-0013: 도메인 모듈 테스트 독립 실행 전략

## 상태

적용됨 (2026-05-23, 보강 2026-05-24)

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
| `:app` — `RestExceptionHandler` | 동일 이유 | `:web` 모듈의 실제 핸들러로 대체됨 (ADR-0014 적용 완료) |

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

### 공통 테스트 스케줄러 설정 — `CommonTestSchedulerConfig`

도메인 모듈 수가 늘어나면서 `IntegrationTestConfig`와 `ServiceTestConfig` 양쪽에 동일한 `TaskScheduler` mock 빈(noOp `taskScheduler`, `delayRemovalScheduler`)이 중복 정의되는 문제가 발생했다.

공통 부분을 `CommonTestSchedulerConfig`로 추출하고 각 Config에서 `@Import(CommonTestSchedulerConfig.class)`로 재사용한다.

```java
@TestConfiguration(proxyBeanMethods = false)
@Profile("test")
public class CommonTestSchedulerConfig {

    @Bean(name = "taskScheduler")
    @Primary
    public TaskScheduler noOpTaskScheduler() {
        return Mockito.mock(TaskScheduler.class, Answers.RETURNS_MOCKS);
    }

    @Bean(name = "delayRemovalScheduler")
    public TaskScheduler testDelayRemovalScheduler() {
        return new ShutDownTestScheduler();
    }
}
```

`CommonTestSchedulerConfig`는 `:game` 의존이 없으므로 `:test-support`로 이동했다. `ServiceTest`와 `IntegrationTestSupport` 베이스 클래스에서 `@Import(CommonTestSchedulerConfig.class)`로 가져오므로, 이를 상속하는 모든 도메인 모듈 테스트에서 `taskScheduler`·`delayRemovalScheduler` 빈이 자동으로 제공된다. `IntegrationTestConfig`와 `ServiceTestConfig`에서 중복 `@Import`는 제거했다.

### 통합 테스트 — DB 격리를 위한 `@BeforeEach` cleanup

통합 테스트에서 여러 테스트 메서드가 DB 상태를 공유하여 실행 순서에 따라 결과가 달라지는 문제가 발생했다.
`@BeforeEach`를 `IntegrationTestSupport` 베이스 클래스(`:test-support`)에 추가해 각 테스트 메서드 시작 전 DB를 초기화한다. 직전 테스트 잔여물에 영향받지 않고 항상 깨끗한 상태에서 시작하도록 보장한다.

`@Transactional` 롤백 방식은 Redis Stream·비동기 이벤트처럼 트랜잭션 외부에서 발생하는 부수효과를 롤백하지 못하므로 사용하지 않는다.

### `:test-support`에서 게임 도메인 의존 제거

`:test-support`는 초기에 게임 관련 테스트 설정(FlowScheduler 빈 등)을 포함하고 있었다. 이는 `:test-support` → `:game` 의존을 유발해 공통 테스트 인프라 모듈이 특정 도메인을 알게 되는 구조였다.

게임 도메인 설정을 `:app:test`의 `IntegrationTestConfig`로 이동해 `:test-support`가 도메인 무관 공통 인프라(베이스 클래스, `application-test-base.yml`)만 제공하도록 정리했다. 함께 미사용 상태였던 `MockFlowSchedulerConfig`도 삭제했다.

`@IntegrationTest` 합성 어노테이션은 삭제했다. 기존에 이 어노테이션이 담당하던 `@SpringBootTest(RANDOM_PORT) + @ActiveProfiles("test")`는 `:test-support`의 `IntegrationTestSupport` 베이스 클래스가 제공하고, `@Import(IntegrationTestConfig.class)`는 `:app`의 `IntegrationTestSupport`가 직접 선언한다. 테스트 클래스는 어노테이션 합성 대신 베이스 클래스 상속으로 설정을 획득한다.

### `withReuse(true)` — 병렬 빌드 시 알려진 충돌 (2026-05-28)

`gradle.properties` 에 `org.gradle.parallel=true` 가 설정되어 있어 `./gradlew build` 는
`:profanity:test`, `:room:test`, `:user:test` 등 여러 모듈 테스트 태스크를 **별도 JVM 에서 동시 실행**한다.

`withReuse(true)` 는 동일 이미지·설정을 가진 컨테이너를 JVM 간에 공유(재사용)하게 한다.
결과적으로 병렬 JVM 들이 **같은 MySQL 컨테이너**에 동시에 접속하고,
각 JVM 의 Spring Context 초기화 시 `ddl-auto: create` 가 동시에 DROP → CREATE 를 실행한다.

```text
profanity JVM  ──┐
room      JVM  ──┼── 같은 MySQL 컨테이너 공유 (withReuse=true)
user      JVM  ──┘
      각 JVM: ddl-auto: create → DROP TABLE → CREATE TABLE
      → 다른 JVM이 방금 만든 테이블을 DROP → SQLSyntaxErrorException
```

이 레이스 컨디션은 `testcontainers.reuse.enable=true` 가 `~/.testcontainers.properties` 에
설정된 개발자 로컬 환경에서만 재현된다. CI 환경은 이 설정이 없으므로 영향 없다.

`withReuse(true)` 제거 시 로컬 재실행 비용이 ~30초 증가하므로 **현재는 유지한다.**
`ddl-auto: create` 대신 Flyway 기반 테스트 마이그레이션으로 전환하면
`withReuse` 를 유지하면서 충돌도 제거할 수 있다 — 향후 개선 과제로 남긴다.

### CI에서 reuse 활성화 (2026-06-07)

위 레이스 컨디션의 원인이었던 **단일 공유 DB(`zzol_test`)** 구조는 이후
모듈별 독립 DB(`zzol_test_<module>`) + 모듈별 Redis DB 인덱스(0~8) 격리로 해소됐다
(루트 `build.gradle.kts`의 `test.db.name` / `test.redis.db` 시스템 프로퍼티 참조).
각 모듈 JVM의 `ddl-auto: create` 는 자기 DB만 DROP/CREATE 하므로 충돌하지 않는다.

이에 따라 CI 워크플로우(`backend-ci.yml`)에서도 `~/.testcontainers.properties` 에
`testcontainers.reuse.enable=true` 를 설정해 reuse를 활성화했다.

- **이전**: `org.gradle.parallel=true` 로 모듈 test 태스크가 별도 JVM에서 병렬 실행될 때
  JVM마다 MySQL + Valkey 컨테이너를 새로 기동 (통합 테스트 보유 모듈 수만큼 반복)
- **이후**: 최초 JVM이 기동한 컨테이너 1세트를 이후 JVM이 재사용

알려진 한계: 병렬 JVM들이 거의 동시에 최초 기동하면 reuse 해시 조회 전에
컨테이너가 중복 생성될 수 있다. 각 JVM은 자신이 만든 컨테이너를 사용하므로
정합성 문제는 없고, 최악의 경우가 reuse 비활성 상태와 동일하다.
CI 러너는 잡 종료 시 폐기되므로 잔여 컨테이너 정리도 불필요하다.

### reuse 영구 비활성화 — 모듈별 격리 제거 (2026-06-11)

위 reuse 활성화는 철회한다. 모듈별 DB·Redis 인덱스 격리는 **데이터 충돌**만 막을 뿐,
병렬 모듈 테스트가 단일 공유 컨테이너(한 MySQL·Valkey 프로세스)를 동시에 두드릴 때 생기는
**자원 경합**(처리량 한계·지연 변동)은 막지 못한다. 타이밍 민감한 게임 통합테스트가 이
경합으로 간헐 awaitility 타임아웃했고, 스트림/컨텍스트 개선(#1361/#1369)이 머지된 뒤에도
reuse-on은 플레이키로 재현됐다(로컬 `./gradlew test --rerun-tasks`).

따라서 `withReuse(true)` 를 제거(JVM별 독립 컨테이너 = 물리적 자원 격리)하고, 그에 따라
무효가 된 모듈별 DB(`zzol_test_<module>`)·Redis 인덱스(0~8) 격리와 `test.db.name` /
`test.redis.db` 시스템 프로퍼티를 함께 제거했다. 각 모듈 JVM은 자기 컨테이너의 `zzol_test` 와
redis db 0 을 단독 사용한다. 기동 시간 증가는 #1369(컨텍스트 기동 횟수↓)·#1401(게임 IT 시간↓)로
상쇄됐다.

- 이슈: #1402
- CI `backend-ci.yml` 의 "Enable Testcontainers reuse" 스텝은 no-op이 된다 — 워크플로 정리는 별도(main 전용)

### TestContainers 버전 고정

Spring Boot BOM 이 트랜지티브 의존으로 `testcontainers` 를 1.x 로 다운그레이드한다.
1.x 는 Windows Named Pipe Docker 연결을 지원하지 않아 `Could not find a valid Docker environment` 오류가 발생한다.

`:test-support` 의 `build.gradle.kts` 에서 `api` 로 선언해 BOM 보다 우선 적용되도록 하고, 도메인 모듈은 전이 의존으로 획득한다.

```kotlin
// :test-support/build.gradle.kts
val testcontainersVersion = rootProject.extra["testcontainers"] as String
api("org.testcontainers:testcontainers:$testcontainersVersion")
api("org.testcontainers:mysql:$testcontainersVersion")
api("org.testcontainers:junit-jupiter:$testcontainersVersion")
```

도메인 모듈은 별도 선언 없이 `testImplementation(project(":test-support"))` 전이 의존으로 testcontainers 를 획득한다.

## 관련 포스트모템

reuse 정책 변경(활성화 → 비활성)을 둘러싼 인시던트·의사결정 회고:

- [포스트모템 0001 — 게임 통합테스트 플레이키](../postmortem/0001-game-integration-test-flaky.md)
- [포스트모템 0002 — Testcontainers reuse 의사결정 2회 번복](../postmortem/0002-testcontainers-reuse-decision-reversal.md)

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
- **신설 `:web` 모듈**: `:infra` 위에 위치하는 공유 HTTP 인프라 모듈로 ADR-0014에 별도 제안.
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
  방지하는 것과 동일한 원리다 (ADR-0014 참조).

## 결과

### 긍정적 효과

- `:room:test` 단독 실행으로 room 도메인 변경의 회귀를 즉시 검출할 수 있다
- `application-test-base.yml` 로 공통 설정 중복이 제거된다
- 다른 도메인 모듈도 동일 패턴으로 마이그레이션할 수 있다

### 부정적 효과 / 트레이드오프

- `RestExceptionHandler` mock 은 ADR-0014(`:web` 모듈) 구현 전까지 각 모듈 테스트에 분산된다.
  HTTP 응답 정책이 바뀌면 프로덕션 핸들러와 테스트 mock 을 함께 수정해야 한다.
- `MiniGameFactoryConfig` mock 은 `:room` 이 `:game-api` 추상만 알고 `:game` 구체 팩토리를
  모르는 ADR-0011 OCP 원칙을 테스트 레벨에서 유지하기 위한 비용이다.
  `MiniGameDummy` 의 `CardGameScore`(`:game`) 의존을 익명 `MiniGameScore` 서브클래스(`:game-api`)로
  교체하고 `testImplementation(project(":game"))` 을 제거하여 OCP 위반이 해소됐다.
- TestContainers 버전은 `:test-support` 한 곳에서 관리되므로, 신규 도메인 모듈 추가 시 버전 선언을 누락하면 BOM 다운그레이드가 발생할 수 있다. `:test-support` 의존을 반드시 포함해야 한다.

### 미적용 모듈

현재 `:profanity`, `:room`, `:user`, `:websocket`, `:infra`, `:admin`, `:zzolbot` 이 이 패턴을 적용했다.
나머지 도메인 모듈(`:game`) 은 아직 `:app:test` 에 의존한다.
마이그레이션 순서는 팀 협의로 결정한다.
