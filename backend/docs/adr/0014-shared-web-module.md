# ADR-0014: 공유 웹 인프라 모듈 `:web` 도입

- 날짜: 2026-05-23
- 상태: 적용됨

## 컨텍스트

ADR-0011 멀티 모듈 전환 이후 `RestExceptionHandler`가 `:app` 모듈에 위치한다.
`:app`은 모든 모듈을 조합하는 composition root이므로 순환 의존 없이 도메인 모듈이 역방향으로 참조할 수 없다.

이로 인해 두 가지 문제가 발생한다.

1. **테스트 컨텍스트 불완전**: 도메인 모듈 단독 테스트 시 `RestExceptionHandler`가 로드되지 않아
   `BusinessException`이 HTTP 상태 코드로 변환되지 않고 `ServletException`으로 노출된다.
   ADR-0013에서 `TestRestExceptionHandler`로 임시 대처했으나, 모듈이 늘어날수록 동일한
   test stub이 각 모듈에 복제된다.

2. **HTTP 응답 정책 분산**: 새로운 예외 타입 추가 시 `:app`의 `RestExceptionHandler`와
   각 도메인 모듈의 `TestRestExceptionHandler`를 함께 수정해야 한다.

`RestExceptionHandler`의 실제 의존 관계는 다음과 같다.

```text
RestExceptionHandler
  → coffeeshout.global.exception.*   (:common)
  → coffeeshout.global.ipblock.*     (:infra)
  → coffeeshout.global.log.*         (:infra)
  → Spring MVC (ProblemDetail, @RestControllerAdvice 등)
```

`:app`이 아닌 별도 공유 모듈에 두기 위한 조건이 이미 충족된다.

## 결정

`:web` 모듈을 신설하고 HTTP 계층 횡단 관심사를 집중 관리한다.

### 모듈 위치

```text
:common
  ↑
:infra
  ↑
:web          ← 신설 (Spring MVC, HTTP 횡단 관심사)
  ↑
:websocket  :room  :user  :admin  :zzolbot  ...
  ↑──────────────────────────────────────────┘
:app
```

`:web`은 `:infra`에 의존하고, REST 엔드포인트를 노출하는 모든 도메인 모듈은 `:web`에 의존한다.
`:websocket`도 REST 엔드포인트(`WsRecoveryController` 등)를 노출하므로 `:web`에 의존한다.

### `:web`의 책임

| 구성요소 | 이동 전 위치 | 비고 |
|---|---|---|
| `RestExceptionHandler` | `:app` | 도메인 예외 → HTTP 상태 코드 변환 |
| `WebMvcConfig` + `CorsProperties` | `:app` | 환경별 CORS 허용 출처 설정 |
| `SwaggerConfig` | `:infra` | OpenAPI Bean 정의 |
| `spring-boot-starter-web` | 각 도메인 모듈 | `api`로 노출, 도메인 모듈 중복 선언 제거 |
| `spring-boot-starter-validation` | 각 도메인 모듈 | `api`로 노출, 도메인 모듈 중복 선언 제거 |
| `springdoc-openapi-starter-webmvc-ui` | 각 도메인 모듈 | `api`로 노출, 도메인 모듈 중복 선언 제거 |

`java-library` 플러그인의 `api` 설정을 사용하므로 루트 `build.gradle.kts`에서 `java` → `java-library`로 전환한다.
이를 통해 `:web`에 선언한 의존이 다운스트림 모듈의 컴파일 클래스패스에 전파된다.

### `:app`에서 유지할 것

composition root 고유 설정(Bean 조합, `MiniGameFactoryConfig`, 프로파일별 설정 등)은
`:app`에 그대로 둔다. `:web`은 HTTP 프로토콜 레이어만 담당하며 도메인 비즈니스 빈을
직접 조합하지 않는다.

### ADR-0013과의 관계

`:web` 도입으로 다음 사항이 해소되었다.

- `TestRestExceptionHandler` → 제거 완료. `:room:test`가 `:web` 의존 후 실제 핸들러 사용
- `miniGameFactoryMap` mock → ADR-0013 대안 D(`:test-support` 집중 관리)로 별도 검토 예정

## 고려한 대안

### 대안 A: `:infra`로 이동

`:room`이 이미 `:infra`에 의존하므로 즉시 사용 가능하다.
그러나 `:infra`의 책임(JPA, Redis, Outbox, Lock, IpBlock)과 HTTP 예외 처리는
성격이 다르다. `:infra`가 Spring MVC에 의존하게 되어 웹 서블릿 없이 `:infra`만 쓰는
시나리오(배치, 이벤트 컨슈머)에서 불필요한 의존이 포함된다.

### 대안 B: `:app` 유지 (현재)

단기적으로 가장 단순하다. 도메인 모듈 수가 적고 HTTP 정책 변경이 드물다면 유지할 수 있다.
그러나 `:room` 다음으로 `:user`, `:admin` 등의 단독 테스트가 필요해지면 동일한 stub 복제
문제가 반복된다.

### 대안 C: `@ControllerAdvice` 를 각 도메인 모듈에 복제

모듈 자율성이 극대화되나 HTTP 응답 포맷/로깅 정책이 분산되어 유지보수 비용이 선형 증가한다.

## 결과

### 긍정적 효과

- 도메인 모듈 단독 테스트 시 test stub 없이 일관된 HTTP 응답 검증 가능
- HTTP 응답 정책(ProblemDetail 구조, 로깅 수준, IP 차단 연동)을 단일 위치에서 변경 가능
- `:app`이 HTTP 정책 대신 Bean 조합에만 집중하도록 책임 분리

### 부정적 효과 / 트레이드오프

- 모듈이 10개 → 11개로 증가. `settings.gradle.kts`, CI 파이프라인, 의존 다이어그램 갱신 필요
- REST 엔드포인트가 없는 모듈(`:zzolbot` 등)도 `:web`에 의존하게 될 수 있어,
  모듈별로 의존 선택을 의식해야 한다

### 구현 완료 내용

- `settings.gradle.kts`에 `:web` 모듈 등록
- 루트 `build.gradle.kts` `subprojects {}`: `java` → `java-library` 플러그인 전환 (`api` 설정 활성화)
- `RestExceptionHandler`, `WebMvcConfig`, `CorsProperties` 이동 (`coffeeshout.app.*` → `coffeeshout.web.*`)
- `SwaggerConfig` 이동 (`:infra` `coffeeshout.global.config` → `:web` `coffeeshout.web.config`)
- `WebMvcConfig`: 와일드카드 허용에서 `CorsProperties`(`web.cors.allowed-origins`) 기반으로 교체
- `:web`이 `api`로 노출하는 의존: `spring-boot-starter-web`, `spring-boot-starter-validation`, `springdoc-openapi-starter-webmvc-ui`
- REST 엔드포인트를 가진 모든 모듈에 `implementation(project(":web"))` 추가: `:room`, `:game`, `:user`, `:admin`, `:zzolbot`, `:websocket`
- 해당 모듈에서 `spring-boot-starter-web`, `spring-boot-starter-validation`, `springdoc-openapi` 중복 선언 제거
- `:infra`에서 `springdoc-openapi` 제거, `IpBlockFilter`의 `org.jspecify.annotations.NonNull` → `org.springframework.lang.NonNull` 교체
- `RoomTestApplication.scanBasePackages`에 `"coffeeshout.web"` 추가
- `TestRestExceptionHandler` 제거 완료
