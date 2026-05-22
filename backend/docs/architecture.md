# 아키텍처

## 모듈 구조

프로젝트는 10개 Gradle 모듈로 구성된다.

```text
:common     — Spring 무관 순수 추상 (ErrorCode, BaseEvent, TraceInfo, VO)
:infra      — Spring + JPA + Redis + Outbox + Lock + IpBlock + Health + Metric
:websocket  — STOMP 플랫폼 (도메인 무지)
:game-api   — 게임 SPI (Playable, MiniGameFactory, FlowScheduler, Gamer)
:user       — User + Auth + Friend
:room       — Room aggregate + Player + Roulette + RoomSessionToken
:game       — 6게임 구현체 + minigame orchestration
:admin      — dashboard + patchnote + report
:zzolbot    — AI 운영자 어시스턴트
:app        — Spring Boot 진입점, 모든 모듈 조합
```

의존 방향 (단방향, 순환 없음):

```text
:common → :infra → :websocket
                 → :user
                 → :game-api → :room → :game → :admin
                                             → :zzolbot
                 (모두) → :app
```

핵심 설계 원칙:
- `:room`은 `:game-api`만 알고 `:game` 구체 클래스를 모른다 — 새 게임 추가 시 room 코드 무수정 (OCP)
- `:common`은 Spring 의존이 없어 모든 모듈이 안전하게 import 가능
- ArchUnit이 모듈 내부 계층 역방향(domain→infra 등)을 CI에서 차단

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

| 패키지          | 역할                                              |
|--------------|-------------------------------------------------|
| `exception/` | ErrorCode 인터페이스, BusinessException 계층           |
| `trace/`     | TraceInfo, Traceable                            |
| `nickname/`  | NameValidator, WordPicker 등 닉네임 유틸              |
| `redis/`     | BaseEvent, StreamKey 인터페이스                      |
| `log/`       | NotificationMarker                              |

`:infra` 모듈이 담는 것:

| 패키지          | 역할                                              |
|--------------|-------------------------------------------------|
| `config/`    | 프레임워크 Bean 등록 (Async, Clock, Swagger 등)         |
| `redis/`     | Redis Stream 인프라, Redisson, 커넥션 설정              |
| `ipblock/`   | IP 차단 (필터, 저장소, 악성 경로 감지)                       |
| `metric/`    | HTTP·Redis Stream Micrometer 메트릭 수집             |
| `outbox/`    | Transactional Outbox (이벤트 유실 방지)                |
| `lock/`      | Redisson 기반 분산 락                                |
| `health/`    | Spring Actuator 헬스 인디케이터                        |

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

```
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

## 게임 SPI 패턴

새 게임을 추가할 때 기존 코드를 수정하지 않아도 된다 (OCP).

```
:game-api
  Playable        — 게임이 구현해야 하는 인터페이스
  MiniGameFactory — 게임 생성 SPI (각 게임이 Spring 빈으로 등록)
  Gamer           — 게임 참여자 (String name, Long userId)

:game
  CardGameFactory implements MiniGameFactory  — 빈 등록만 하면 자동 디스패치
  CardGame implements Playable
```

`MiniGameEventService`는 `List<MiniGameFactory>`를 주입받아 `EnumMap<MiniGameType, MiniGameFactory>`로 관리한다. 새 게임 추가 = `MiniGameType` enum 1줄 + `{NewGame}Factory` 빈 등록.

`Gamer` record는 `room.Player` 대신 게임이 사용하는 플레이어 표현으로, game 모듈이 room 타입 없이 플레이어 정보를 다룰 수 있게 한다.

---

## 게임 플로우 스케줄링

게임 페이즈 전환(로딩 → 플레이 → 스코어보드)은 `CompletableFuture` 체인으로 구현된다.

```
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

| 서브패키지          | 역할                                                                                           |
|----------------|----------------------------------------------------------------------------------------------|
| (루트)           | `StompSessionManager`, `PlayerKey`, `UserPrincipal`, `LoggingSimpMessagingTemplate` 등 핵심 서비스 |
| `config/`      | STOMP 브로커 설정 (`WebSocketMessageBrokerConfig`)                                                |
| `docs/`        | WebSocket 컨트랙트 디스커버리 (애너테이션 + `/dev/ws-catalog`)                                             |
| `event/`       | Spring 이벤트 리스너 — 세션 구독·해제 처리                                                                 |
| `interceptor/` | STOMP 인터셉터 — 레이트 리밋, 메트릭, Graceful Shutdown                                                  |
| `lifecycle/`   | `WebSocketGracefulShutdownHandler`                                                           |
| `metric/`      | `WebSocketMetricService`                                                                     |
| `ratelimit/`   | `WebSocketRateLimiter`                                                                       |

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
