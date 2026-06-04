# ADR-0021: Redis Stream 트레이스 전파 — 페이로드 임베드에서 W3C traceparent 캐리어로 전환

## 상태

승인 (2026-06-04) — 1단계 구현 완료 ([PR #1356](https://github.com/woowacourse-teams/2025-zzol/pull/1356))

## 컨텍스트

분산 트레이싱 자체는 이미 동작 중이다 (Micrometer Tracing + OTel 브릿지 + OTLP → Tempo, `monitoring.yml`). 다만 Redis Stream 경계는 자동 계측 대상이 아니라서 커스텀 전파 패턴을 직접 만들어 쓰고 있다.

현재 구조:

```text
이벤트 생성자 → TraceInfoExtractor.extract() (정적 홀더 경유)
            → TraceInfo(traceId, spanId)가 이벤트 JSON에 embed
            → Redis Stream
            → EventDispatcher: instanceof Traceable 분기
            → TracerProvider: traceContextBuilder()로 수동 컨텍스트 조립
```

이 패턴의 문제 4가지:

1. **도메인 침투**: 이벤트 19개(`:room` 11개, `:game` 8개)가 `Traceable`을 구현하고 `TraceInfo` 필드를 보유한다. 새 이벤트를 만들 때마다 트레이싱 보일러플레이트가 반복되고, 도메인 이벤트가 관측성 인프라를 알게 된다.
2. **정적 서비스 로케이터**: 도메인 객체가 빈을 주입받을 수 없어 `ObservationRegistryProvider`가 `ObservationRegistry`를 정적 변수로 노출한다. 초기화 순서에 취약하고 테스트에서 전역 상태를 공유한다.
3. **샘플링 우회**: `TracerProvider`가 컨텍스트 복원 시 `sampled(true)`를 강제해 `TRACE_SAMPLING_PROBABILITY` 설정이 컨슈머 측에서 무시된다 (버그성 동작).
4. **비표준 포맷**: `{traceId, spanId}` 커스텀 레코드는 W3C Trace Context와 호환되지 않아, 외부 시스템·표준 도구와의 상호운용이 막혀 있다.

## 결정

전파 책임을 **이벤트(19곳)에서 인프라 경계(2곳)로** 옮기고, 포맷을 W3C `traceparent`로 표준화한다.

### 1단계 — traceparent 캐리어 전환

- **발행**: `StreamPublisher`가 MapRecord `{payload: <이벤트 JSON>, traceparent: <W3C 헤더>}`로 발행한다. 주입은 Micrometer `Propagator.inject()` 사용 — 설정된 전파 포맷(W3C 기본, `tracestate` 포함)을 그대로 따른다
- **수신**: `RedisStreamListenerStarter`가 `Propagator.extract()`로 컨텍스트를 복원하고 consumer span 스코프 안에서 `EventDispatcher.handle()`을 실행한다. `traceparent` 필드가 없으면 스팬 없이 실행한다 (현행 동작 유지)
- **제거**: `Traceable`, `TraceInfo`(`:common`), `TraceInfoExtractor`, `ObservationRegistryProvider`, `TracerProvider`(`:infra`) 삭제. `EventDispatcher`의 `instanceof Traceable` 분기 제거. 이벤트 19개에서 `TraceInfo` 필드·생성자 호출 제거
- **Outbox 경로**: `OutboxEventRecorder`가 **기록 시점에** `traceparent`를 outbox 레코드에 함께 저장한다 (V30 마이그레이션, VARCHAR(64)). 재시도 릴레이(`OutboxRelayWorker`)는 스케줄러 스레드에서 실행되어 원본 트레이스 컨텍스트가 없으므로, 발행 시점 추출로는 trace가 유실된다. `tracestate`는 저장하지 않는다 — Tempo 단일 백엔드 구성에서 실익이 없는 의도적 트레이드오프
- **게임 Flow 스케줄러 경계 보강**: 구현 중 비동기 경계 전수 감사에서, 게임 스케줄러 6개(ladder·blockstacking·cardgame·speedtouch·racing·blindtimer)의 지연 실행 → Stream 발행 경로가 trace를 유실하는 기존 갭을 발견했다 (구 `TraceInfoExtractor`도 동일하게 유실 — 회귀 아님). 각 `ThreadPoolTaskScheduler`에 `ContextSnapshot` TaskDecorator를 적용해 제출 시점 컨텍스트를 전파한다. 데코레이터는 `andThen` 체인까지 전이적으로 전파된다

### 신구 메시지 호환

- 구형 메시지(JSON에 `traceInfo` 필드 잔존)를 신형 코드가 읽는 경우: `redisObjectMapper`가 `FAIL_ON_UNKNOWN_PROPERTIES=false`라 무해하다
- 구형 레코드 구조(`ObjectRecord` 단일 필드)를 신형 리스너가 읽는 경우: 레거시 필드(`_raw`) 폴백을 1릴리스 동안 유지한 뒤 제거한다
- **신→구 비호환 (배포 주의)**: 구형 리스너는 신형 MapRecord를 읽지 못한다. 롤링 배포 윈도우 또는 롤백 시 신형 메시지가 스트림에 남아있는 동안 구버전 인스턴스에서 파싱 실패가 발생한다

### 구현 노트 — 테스트 환경

`@SpringBootTest`는 기본으로 `management.tracing.enabled=false`를 주입해(ObservabilityContextCustomizerFactory) `Propagator`가 noop이 된다 — 스팬은 생성되는데 전파만 안 되는 형태라 원인 파악이 어렵다. 전파를 검증하는 테스트에는 `@AutoConfigureObservability`가 필수다 (`RedisStreamContextPropagationTest` 참고). 로컬 프로파일(`application-local.yml`)도 `tracing.enabled: false`라 동일하게 전파가 비활성화된다.

### 2단계 (후속) — 이벤트 envelope 분리

`traceparent`와 같은 성격의 전송 메타데이터가 이벤트에 더 남아 있다: `eventId`(UUID 생성), `timestamp`(`Instant.now()`), 타입 정보(`@JsonTypeInfo`). 이를 발행 경계에서 감싸는 envelope로 옮기면:

- `BaseEvent`의 `@JsonTypeInfo` 제거 → **`:common`의 유일한 외부 라이브러리(Jackson) 의존이 사라진다**
- ~~`MiniGameBaseEvent`의 중복 계약을 `BaseEvent`로 통합~~ → 1단계 구현 중 구현체가 없는 미사용 인터페이스로 판명되어 삭제 완료
- 이벤트 생성자의 `UUID.randomUUID()`/`Instant.now()` 직접 호출 제거 → 시간·ID 주입이 가능해져 테스트 용이성 개선

2단계는 페이로드 스키마 전면 변경이므로 1단계 안정화 후 별도 작업으로 진행한다.

## Spring Boot 4와의 관계

이 전환은 **Boot 3.5에서 즉시 가능**하며 [ADR-0020](0020-spring-boot-4-migration.md)과 독립이다. 다만 Boot 4의 `spring-boot-starter-opentelemetry`가 현재 `:infra`에 수동 나열된 OTel 의존성 5개를 스타터 1개로 통합하므로, Boot 4 전환 시 관측성 정리라는 시너지가 있다.

## 고려한 대안

### 대안 A: 현행 유지

- 장점: 변경 없음
- 단점: 문제 4가지 지속, 새 이벤트마다 보일러플레이트 반복
- 기각: 샘플링 우회는 방치할 수 없는 버그성 동작

### 대안 B: OTel Java Agent 자동 계측

- 장점: 코드 무수정
- 단점: Lettuce 명령 단위 span만 생긴다. Stream 메시지 단위의 producer→consumer 컨텍스트 연결은 불가
- 기각: 핵심 요구(메시지 경계 전파)를 충족하지 못함

### 대안 C: Spring Cloud Stream 등 메시징 추상화 도입

- 장점: 바인더가 트레이스 전파를 내장
- 단점: 메시징 계층 전체 교체 — Redis Stream 직접 제어(XAdd 옵션, 컨슈머 그룹 운영, 스레드풀 설정)를 포기
- 기각: 전파 1가지를 위해 아키텍처 핵심 경로를 교체하는 것은 과도

## 결과

- 도메인 이벤트가 트레이싱을 전혀 모르게 된다 — 새 이벤트 작성 시 트레이싱 고려 불필요
- `:common`의 `trace` 패키지 삭제, 정적 홀더 제거
- 샘플링 결정이 producer→consumer로 표준 전파된다 (`sampled(true)` 강제 제거 — 기존에는 producer가 샘플링하지 않은 trace도 consumer 스팬이 강제 export되어 앞부분이 잘린 trace가 쌓였다)
- 게임 Flow의 지연 실행 단계(라운드 전환·종료 등)가 흐름을 시작한 명령의 trace에 child로 연결된다 — 기존에는 `@Observed` 브로드캐스트마다 고아 root trace가 생성됐다
- 메시지 처리 흐름(Handler → Stream → Consumer)은 변경 없음 — 전파 메커니즘만 교체
- `docs/architecture.md`, `docs/tech-stack.md`, `docs/conventions-production.md`의 TraceInfo/Traceable 언급 갱신 완료
