# 아키텍처 레퍼런스

> **핵심 제약**(의존 방향 규칙, Redis Stream 경유 필수, 구조 규칙)은 `CLAUDE.md`의 "아키텍처 핵심 제약" 섹션을 참조한다.
> 이 문서는 패키지 구조 상세, WebSocket 컨트랙트, Game SPI, Flow 스케줄링 등 배경 레퍼런스를 담는다.

---

## 모듈 구성

프로젝트는 13개 Gradle 모듈로 구성된다.

```text
:common       — Spring 무관 순수 추상 (ErrorCode, BaseEvent, VO)
:infra        — Spring + JPA + Redis + Outbox + Lock + IpBlock + Health + Metric
:web          — 공유 HTTP 인프라 (RestExceptionHandler, CORS, SpringDoc)
:websocket    — STOMP 플랫폼 (도메인 무지)
:game-api     — 게임 SPI (Playable, MiniGameFactory, FlowScheduler, Gamer)
                + 도메인 간 계약 (이벤트, RoomSnapshotQuery·SeasonUserProfileQuery 포트)
:user         — User + Auth + Friend
:room         — Room aggregate + Player + Roulette + RoomSessionToken
:game         — 6게임 구현체 + minigame orchestration
:profanity    — 비속어 필터 (:admin·:app 이 사용, :room·:game 은 테스트에서만)
:admin        — dashboard + patchnote + report
:zzolbot      — AI 운영자 어시스턴트
:app          — Spring Boot 진입점, 모든 모듈 조합
:test-support — 통합/서비스 테스트 공통 인프라 (testImplementation 전용)
```

### 의존 방향 (단방향, 순환 없음)

계층은 아래에서 위로만 의존한다. 같은 계층끼리의 의존은 `:room → :user` 하나뿐이다.

```text
L4  조립      :app                                        ← 모든 모듈 조합
L3  소비자    :admin      :zzolbot
L2  도메인    :room ──→ :user      :game      :profanity
L1  플랫폼    :websocket  :web  :infra  :game-api  :test-support
L0  순수      :common                                     ← Spring 무관
```

모듈별 프로덕션 의존(`implementation`/`api`)은 다음과 같다. 표의 `→`는 **"왼쪽이 오른쪽에 의존한다"**를 뜻한다.

| 모듈 | 프로덕션 의존 |
| --- | --- |
| `:common` | 없음 |
| `:web` | `:common` |
| `:infra` | `:common` |
| `:game-api` | `:common` |
| `:test-support` | `:common` |
| `:websocket` | `:common` `:web` |
| `:profanity` | `:common` `:infra` |
| `:game` | `:common` `:game-api` `:infra` `:web` `:websocket` |
| `:user` | `:common` `:game-api` `:infra` `:web` `:websocket` |
| `:room` | `:common` `:game-api` `:infra` `:web` `:websocket` **`:user`** |
| `:zzolbot` | `:common` `:game-api` `:infra` `:web` + `:game` `:room` |
| `:admin` | `:common` `:game-api` `:infra` `:web` + `:game` `:room` `:user` `:profanity` |
| `:app` | 전 모듈 (`:test-support` 제외 — 테스트 전용) |

**L2 도메인 4개 중 `:game`·`:user`·`:profanity`는 다른 도메인 모듈을 컴파일 시점에 모른다.** `:game`이 방·유저와 주고받는 것은 전부 `:game-api`의 이벤트·조회 포트를 거친다(ADR-0025, ADR-0034). ArchUnit `game_프로덕션은_room을_직접_참조할_수_없다`·`game_프로덕션은_user를_직접_참조할_수_없다`가 재유입을 CI에서 차단한다.

`:room → :user`는 남아 있는 유일한 도메인 간 의존이다(인증 타입 + 닉네임 조회).

#### 테스트 스코프는 위 그림과 다르다

`testImplementation`/`testFixtures`로만 걸린 의존은 프로덕션 의존이 아니다. `@SpringBootTest` 컨텍스트가 전이 빈을 로드해 생긴 것으로, 위 계층 판단의 근거로 쓰지 않는다.

```text
:game → :room  :user  :profanity     (테스트 전용 — 프로덕션 의존 아님)
:room → :profanity                   (테스트 전용)
전 모듈 → :test-support               (테스트 전용)
```

---

## 도메인 패키지 구조

각 도메인은 동일한 계층 구조를 따른다.

```text
{domain}/
  application/   # 유스케이스 서비스, 외부 진입점, 플로우 오케스트레이션
  domain/        # 핵심 비즈니스 로직, 엔티티, 도메인 서비스, 도메인 이벤트
  infra/         # 영속성(JPA), 메시징(Redis Stream) 구현체, 스케줄러 구현체
  ui/            # WebSocket 메시지 핸들러, STOMP 엔드포인트
  config/        # 도메인별 스프링 설정 (타이밍, 스레드풀 등)
```

`:common` 모듈이 담는 것:

| 패키지          | 역할                                                  |
|--------------|-----------------------------------------------------|
| `event/`     | ProfanityWordBlockedEvent, BaseEvent                |
| `exception/` | ErrorCode 인터페이스, BusinessException 계층               |
| `nickname/`  | ProfanityChecker, NicknameSubmittedEvent, WordPicker 등 닉네임 유틸 |
| `redis/`     | BaseEvent, StreamKey 인터페이스                          |
| `log/`       | NotificationMarker                                  |
| `ipblock/`   | IpBlockAttributes (속성 VO만 — 필터·저장소는 `:infra`)       |

`:infra` 모듈이 담는 것:

| 패키지          | 역할                                              |
|--------------|-------------------------------------------------|
| `config/`    | 프레임워크 Bean 등록 (Async, Clock, QueryDsl 등)        |
| `redis/`     | Redis Stream 인프라, Redisson, 커넥션 설정              |
| `ipblock/`   | IP 차단 (필터, 저장소, 악성 경로 감지)                       |
| `metric/`    | HTTP·Redis Stream Micrometer 메트릭 수집             |
| `outbox/`    | Transactional Outbox (이벤트 유실 방지)                |
| `lock/`      | Redisson 기반 분산 락                                |
| `health/`    | Spring Actuator 헬스 인디케이터                        |
| `trace/`     | OTel 트레이싱 설정, ObservationRegistry 프로바이더         |

`:web` 모듈이 담는 것:

| 패키지          | 역할                                          |
|--------------|---------------------------------------------|
| `config/`    | CorsProperties, SwaggerConfig, WebMvcConfig |
| `exception/` | RestExceptionHandler (전역 HTTP 예외 처리)        |

> ADR-0014: `:web`은 `:common` 위에 위치한다. `spring-boot-starter-web`, `spring-boot-starter-validation`, `springdoc-openapi`는 `:web`이 `api`로 노출하므로 REST 엔드포인트 모듈은 `:web`에만 의존하면 된다.

---

## 계층별 역할 구분

### Application Layer

- 유스케이스 단위로 클래스를 나눈다
- 도메인 서비스들을 조합하고 외부 의존성(스케줄러, 알림 등)을 주입받는다
- `{Domain}FlowOrchestrator`: 복잡한 게임 흐름(타이밍, 페이즈 전환)을 관리
- `{Domain}Notifier`: 도메인 이벤트를 WebSocket 메시지로 변환하여 발행

### Domain Layer

- 순수 비즈니스 로직만 포함한다. 스프링 의존성을 최소화한다
- `{Domain}CommandService`: 단일 커맨드 처리 (select, touch 등)
- 포트(interface)를 도메인에 정의하고, 구현체는 `infra/`에 위치
- 도메인 이벤트는 record로 정의한다

### Infrastructure Layer

- 포트 구현체, JPA 엔티티, Redis Stream Consumer
- JPA 엔티티는 `{Domain}Entity`로 도메인 객체와 분리

---

## 메시지 처리 흐름

```text
클라이언트 WebSocket 메시지
  → ui/ Handler (커맨드 수신)
  → 도메인 이벤트 생성 (record)
  → StreamPublisher → Redis Stream 발행
  → Consumer 비동기 수신
  → Application Service 처리
  → Notifier → /topic/... 브로드캐스트
  → 클라이언트 수신
```

**카드 선택 예시:**
1. 클라이언트가 `/app/room/{joinCode}/player/select-card`로 메시지 전송
2. `SelectCardCommandHandler`가 수신 → `SelectCardCommandEvent` 생성 → Redis Stream 발행
3. `SelectCardCommandEventConsumer`가 소비 → `CardGameService.selectCard()` 호출
4. `CardGameCommandService`가 도메인 처리 → `CardGameNotifier`가 결과 브로드캐스트

---

## 종료 순서 (lifecycle phase)

`SmartLifecycle` 빈의 `stop()`은 **phase 내림차순**으로 호출된다. 값 자체보다 **상대 순서**가 계약이며,
표의 절반은 우리가 소유하지 않은 값이다 — 라이브러리가 바꾸면 우리 주석은 조용히 거짓말한다.
그래서 순서는 `ShutdownPhaseOrderTest`가 실제 컨텍스트에서 검증한다. **이 표를 고치면 그 테스트도 같이 고친다.**

| phase | 빈 | 하는 일 | 소유 |
| --- | --- | --- | --- |
| `MAX-1` | `WebSocketGracefulShutdownHandler` | WS 세션 드레인 | **우리** (`:websocket`) |
| `MAX-1024` | `WebServerGracefulShutdownLifecycle` | HTTP 요청 드레인 | Spring Boot |
| `MAX-2048` | `WebServerStartStopLifecycle` | 웹서버 stop | Spring Boot |
| `1024` | `RedisStreamContainerRegistry` | Stream 폴러 정지 | **우리** (`:infra`) |
| `512` | `RedisStreamLagMetricService` | XLEN 게이지 소등 | **우리** (`:infra`) |
| `0` | `LettuceConnectionFactory` | Redis 커넥션 정지 | spring-data-redis |

각 자리의 근거:

- **WS 세션 드레인이 가장 먼저** — HTTP 요청 드레인이 시작되기 전에 클라이언트를 정리해야 세션이 끊기지 않는다.
- **폴러 정지(1024)가 커넥션 팩토리(0)보다 먼저** — 순서가 뒤집히면 폴러가 정지된 팩토리에 무한 재시도한다 (ADR-0022).
- **게이지 소등(512)이 폴러 정지 뒤, 커넥션 팩토리 앞** — 폴러가 멈춘 뒤에도 백로그는 관측 대상이다.
  기본값(`Integer.MAX_VALUE`)으로 두면 드레인(`spring.lifecycle.timeout-per-shutdown-phase: 5m`) 내내
  액추에이터는 살아 스크레이핑되는데 게이지만 NaN이 된다 (#1642).

컨텍스트 종료 전체 순서는 `AbstractApplicationContext.doClose()`가 정한다 —
① `ContextClosedEvent` 발행 → ② 위 표의 lifecycle stop → ③ 빈 파괴. **이벤트가 stop보다 먼저**이므로,
`ContextClosedEvent` 리스너(예: 마지막 metric publish를 수행하는 `MeterRegistryCloser`)와
lifecycle stop 사이의 순서는 phase로 조정할 수 없다.

---

## 게임 SPI 패턴

새 게임을 추가할 때 기존 코드를 수정하지 않아도 된다 (OCP).

```text
:game-api
  Playable        — 게임이 구현해야 하는 인터페이스
  MiniGameFactory — 게임 생성 SPI (각 게임이 Spring 빈으로 등록)
  Gamer           — 게임 참여자 (String name, Long userId, Integer colorIndex; 불변 class)

:game
  CardGameFactory implements MiniGameFactory  — 빈 등록만 하면 자동 디스패치
  CardGame implements Playable
```

`MiniGameEventService`는 `List<MiniGameFactory>`를 주입받아 `EnumMap<MiniGameType, MiniGameFactory>`로 관리한다. 새 게임 추가 = `MiniGameType` enum 1줄 + `{NewGame}Factory` 빈 등록.

`Gamer`는 `room.Player` 대신 게임이 사용하는 플레이어 표현으로, game 모듈이 room 타입 없이 플레이어 정보를 다룰 수 있게 한다. 식별(`name`+`userId`)과 표시 상태(`colorIndex`)를 함께 갖는 불변 class이며, 동등성은 식별만으로 정의한다(`colorIndex`는 `equals`/`hashCode` 제외). 색상은 `Player.toGamer()`가 채우고, 게임 응답 DTO가 Room 재조회 없이 `Gamer.colorIndex()`에서 읽는다 (ADR-0025 Step 3).

### 전용 스케줄러·스트림을 쓰는 게임 — 테스트 미러링 (자주 누락)

동적 타이머가 필요한 게임(SpeedTouch·BlindTimer·Nunchi)은 OCP 한 줄 등록(`MiniGameType` + Factory) 외에 **전용 빈/스트림**을 추가한다. 이때 프로덕션에만 등록하고 테스트측 미러를 빠뜨리면, 도메인·서비스 단위 테스트는 통과하지만 **통합테스트가 컨텍스트 로딩 실패 또는 "메시지 미수신"으로 깨진다**.

★ **전용 스케줄러 빈 미러는 모듈마다 따로 존재하는 3곳을 전부 추가해야 한다.** 이 셋은 서로 다른 테스트 컨텍스트가 import하므로, 한 곳만 고치면 그 곳을 안 쓰는 모듈의 IT가 깨진다(아래 표 1행).

| 프로덕션 등록 | 같은 커밋에서 반드시 추가할 테스트 미러 | 누락 시 증상 |
|---|---|---|
| `@Bean("xGameScheduler") @Profile("!test")` (전용 `TaskScheduler`) | **같은 이름** 빈을 다음 3곳에 모두 추가:<br>① `game/src/testFixtures/.../config/GameSchedulerTestConfig` → `new TestTaskScheduler()` (game·service 테스트가 import)<br>② `game/src/test/.../config/IntegrationTestConfig` → `new ShutDownTestScheduler()`<br>③ `app/src/test/.../support/app/config/IntegrationTestConfig` → `new ShutDownTestScheduler()` (전체 컨텍스트 로드 IT가 쓰는 곳) | 컨텍스트 로딩 실패 — `NoSuchBeanDefinitionException: TaskScheduler` (해당 미러가 빠진 모듈의 IT 전체가 무더기 실패) |
| `config/redis.yml`의 `redis.stream.keys["[x]"]` (전용 입력 스트림) | `test-support/.../application-test-base.yml`의 `redis.stream.keys`에 **같은 키** | 컨슈머 미기동 → 스트림 경로 IT가 타임아웃("메시지 미수신") |

체크: 새 게임 PR에 `@Profile("!test")` 빈 또는 새 `redis.stream.keys` 항목이 있으면, 대응하는 테스트 설정이 같은 diff에 있는지 확인한다. 모두 **공유 테스트 자원**이라 누락 시 그 게임만이 아니라 해당 stream/scheduler를 쓰는 통합테스트 전체가 영향받는다.

ADR-0031(Nunchi) 선례: 1차에서 ①②(game쪽)는 추가했으나 ③(app쪽 `IntegrationTestConfig`)을 빠뜨려, PR #1484 CI에서 전체 컨텍스트 로드 IT 약 55건이 `nunchiGameScheduler` `NoSuchBeanDefinitionException`으로 무더기 실패했다(상세: [postmortem 0004](postmortem/0004-test-mirror-checklist-incomplete-recurrence.md)).

---

## 게임 플로우 스케줄링

게임 페이즈 전환(로딩 → 플레이 → 스코어보드)은 `CompletableFuture` 체인으로 구현된다.

```text
FlowOrchestrator
  → FlowScheduler (port, :game-api)
    → CompletableFutureFlowScheduler (infra 구현체, :game)
      → ScheduledExecutorService로 지연 실행
      → EarlyFinishTrigger로 조기 종료 가능
```

타이밍 값은 `application.yml`에서 관리하며, 테스트 시 `application-test.yml`로 오버라이드된다.

---

## WebSocket 패키지 구조

`coffeeshout.websocket/`는 STOMP 기반 WebSocket 인프라 전체를 담는다.

| 서브패키지          | 역할                                                                                                                      |
|----------------|-------------------------------------------------------------------------------------------------------------------------|
| (루트)           | `StompSessionManager`, `SubscriptionInfoService`, `PlayerKey`, `UserPrincipal`, `LoggingSimpMessagingTemplate` 등 핵심 서비스 |
| `aspect/`      | `MessageMappingTracingAspect` — 메시지 핸들러 트레이싱                                                                            |
| `config/`      | STOMP 브로커 설정 (`WebSocketMessageBrokerConfig`)                                                                           |
| `docs/`        | WebSocket 컨트랙트 디스커버리 (애너테이션 + `/dev/ws-catalog`)                                                                        |
| `event/`       | Spring 이벤트 리스너 — 세션 구독·해제 처리                                                                                            |
| `exception/`   | `WebSocketExceptionHandler`                                                                                             |
| `interceptor/` | STOMP 인터셉터 — 레이트 리밋, 메트릭, Graceful Shutdown                                                                             |
| `lifecycle/`   | `WebSocketGracefulShutdownHandler`, `GracefulShutdownHealthIndicator`                                                   |
| `metric/`      | `WebSocketMetricService`                                                                                                |
| `ratelimit/`   | `WebSocketRateLimiter`                                                                                                  |
| `ui/`          | `WsRecoveryController`, `WsRecoveryApi` — 세션 복구 REST 엔드포인트                                                              |

Room 세션 인증(`RoomSessionToken*`)과 접속/해제 이벤트 처리는 `:room.infra.websocket`, `:room.infra.auth`에 위치한다.

STOMP 연결 엔드포인트: `/ws` (SockJS 폴백 지원)

---

## WebSocket 컨트랙트 디스커버리

`coffeeshout.websocket.docs` 패키지는 WebSocket 엔드포인트 명세를 런타임에 자동 수집한다.

### 애너테이션

| 애너테이션        | 부착 위치        | 의미                      |
|--------------|--------------|-------------------------|
| `@WsTopic`   | Notifier 메서드 | 서버 → 클라이언트 broadcast 토픽 |
| `@WsQueue`   | Notifier 메서드 | 서버 → 클라이언트 유저별 큐        |
| `@WsReceive` | Handler 메서드  | 클라이언트 → 서버 수신 경로        |

### 카탈로그 조회

`GET /dev/ws-catalog` (`!prod` 프로파일에서만 활성화)

`WsCatalogBuilder`가 `ApplicationContext`를 스캔하여 애너테이션이 붙은 모든 Bean을 수집하고 JSON으로 직렬화한다.
