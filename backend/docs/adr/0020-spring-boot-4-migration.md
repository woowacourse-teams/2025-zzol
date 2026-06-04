# ADR-0020: Spring Boot 4 마이그레이션 — 3단계 점진 전환

## 상태

제안 (2026-06-04)

## 컨텍스트

현재 프로젝트는 Spring Boot **3.5.3** (Java 21 toolchain) 기반이다. 두 가지 외부 압력이 있다.

1. **OSS 지원 종료**: Boot 3.5의 OSS 지원이 **2026-06-30 종료**된다. 3.5는 3.x의 마지막 라인이므로, 이후 보안 패치를 받으려면 4.x 전환이 유일한 OSS 경로다.
2. **죽은 의존성**: `com.querydsl:querydsl-jpa 5.0.0`은 2021년 이후 릴리스가 없고, Boot 4 + Hibernate 7 환경에서 동작하지 않는다. Spring 팀도 OpenFeign fork(`io.github.openfeign.querydsl`) 전환을 공식 권고한다.

마이그레이션 영향 범위 실측:

- **resilience4j 사용처**: `@CircuitBreaker`(`IpBlockStore`, `ReportRateLimitStore`, `OracleObjectStorageService`), `@RateLimiter`(`GeminiZzolBotClient`, `GeminiNicknameAuditor`), `@Retry`(Gemini 클라이언트 2곳, OracleStorage)
- **QueryDSL 사용 모듈**: `:admin`, `:infra`, `:profanity`, `:room`, `:user`, `:game` — 6개
- **Jackson 2 의존 서드파티**: jjwt-jackson, Redisson, OCI SDK, springdoc 2.8

## 결정

Spring Boot 4.0.x로 마이그레이션하되, 리스크를 분리하기 위해 **3단계 점진 전환**한다.

### Phase 1 — QueryDSL fork 교체 (Boot 3.5에서 선행)

`com.querydsl` → `io.github.openfeign.querydsl` 7.x로 교체한다. fork는 Boot 3.5에서도 동작하므로 Boot 4 전환과 분리해 가장 큰 변수를 먼저 제거한다. `jakarta` classifier 제거 + Q클래스 재생성 + 6개 모듈 빌드 검증이 범위다.

### Phase 2 — Boot 4.0.x 전환 (Jackson 2 호환 모드)

- Boot 4.0.x + Framework 7 + Security 7 전환
- Jackson은 **Jackson 2 스타일 defaults 플래그 + `spring-boot-jackson2` 공존 모듈**로 직렬화 호환을 유지한다 — WebSocket 컨트랙트(STOMP 페이로드)와 REST 응답의 JSON이 변하지 않음을 보장
- springdoc 2.8 → 3.0.x, Redisson·spring-dotenv 호환 버전 확인
- `io.spring.dependency-management` 플러그인 → Gradle 네이티브 `platform()` BOM 전환

### Phase 3 — Jackson 3 네이티브 + 내장 resilience 부분 채택

- Jackson 3(`tools.jackson`) 네이티브 전환. 프론트엔드와 페이로드 호환 검증 필수
- `@Retry` 사용처를 Framework 7 내장 `@Retryable`로 교체
- `@CircuitBreaker` / `@RateLimiter`는 내장 대체가 없으므로 **resilience4j를 유지**한다 (`resilience4j-spring-boot3`는 Boot 4 호환)

## 기대 이점

| 이점                   | 내용                                                            | 이 프로젝트에서의 의미                                         |
|----------------------|---------------------------------------------------------------|------------------------------------------------------|
| 보안 패치 지속             | 3.5 OSS EOL(2026-06-30) 해소                                    | 전환의 1차 동기                                            |
| API 버저닝              | `@RequestMapping(version=...)` + `ApiVersionConfigurer` 일급 지원 | 외부 API 버전 분기를 프레임워크 레벨로                              |
| 모듈러 스타터              | autoconfigure 단일 JAR → 기술별 모듈 분리                              | 12개 모듈 각각의 클래스패스 슬림화, compile avoidance 개선           |
| JSpecify null 안전성    | 프레임워크 전체 null 계약 명시, 컴파일 타임 NPE 검출                            | `:game-api` SPI 등 모듈 간 계약에 null 계약 추가 가능             |
| HTTP Interface 클라이언트 | `@ImportHttpServices`로 선언적 HTTP 클라이언트 (서드파티 불필요)              | 외부 API 연동 보일러플레이트 제거 후보                              |
| Spring Data AOT      | 쿼리 생성을 런타임 → 빌드 타임으로 이동                                       | 기동 시간 단축 (50~70% 보고 사례)                              |
| 내장 resilience        | `@Retryable`, `@ConcurrencyLimit` 코어 내장                       | `@Retry` 사용처만 대체 가능 — 서킷브레이커·레이트리미터는 resilience4j 유지 |
| OpenTelemetry 공식 통합  | 관측성 표준화                                                       | Actuator 기반 모니터링 확장 경로                               |
| Java 25 LTS 지원       | Framework 7 기준선                                               | 현 Java 21에서 차기 LTS 전환 여지 확보                          |

## 고려한 대안

### 대안 A: Boot 3.5 유지 + 상용 지원(HeroDevs 등)

- 장점: 코드 변경 없음
- 단점: 비용 발생, 기술 부채 누적, 죽은 QueryDSL 문제는 그대로 잔존
- 기각: 사이드 프로젝트 규모에서 상용 지원은 비합리적

### 대안 B: 빅뱅 전환 (QueryDSL + Boot 4 + Jackson 3 동시)

- 장점: PR 1개로 종결
- 단점: 실패 시 원인 격리 불가, 리뷰 범위 과대, WebSocket 페이로드 회귀를 다른 변경과 섞어 검증
- 기각: TestContainers 통합 테스트가 안전망이지만, 직렬화 회귀는 단계 분리 없이는 진단 비용이 크다

## 결과

- Phase별 독립 PR로 진행하며, 각 Phase는 전체 통합 테스트(TestContainers) + ArchUnit 통과를 완료 조건으로 한다
- Phase 2 완료 시점부터 보안 패치 공백이 해소된다
- Jackson 3 네이티브 전환(Phase 3) 전까지 WebSocket 컨트랙트의 JSON 직렬화는 기존과 동일하게 유지된다
- 빌드 스크립트 변경 시 [ADR-0011](0011-multi-module-migration.md)의 모듈 의존 방향 제약을 그대로 따른다
