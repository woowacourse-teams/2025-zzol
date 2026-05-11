# 아키텍처

## 도메인 패키지 구조

각 도메인(`cardgame`, `racinggame`, `speedtouch`, `room` 등)은 동일한 계층 구조를 따른다.

```text
{domain}/
  application/   # 유스케이스 서비스, 외부 진입점, 플로우 오케스트레이션
  domain/        # 핵심 비즈니스 로직, 엔티티, 도메인 서비스, 도메인 이벤트
  infra/         # 영속성(JPA), 메시징(Redis Stream) 구현체, 스케줄러 구현체
  ui/            # WebSocket 메시지 핸들러, STOMP 엔드포인트
  config/        # 도메인별 스프링 설정 (타이밍, 스레드풀 등)
  aspect/        # 도메인 전용 AOP 로깅 (선택)
  metric/        # 도메인 전용 Micrometer 메트릭 (선택)
```

`global/`은 도메인을 가로지르는 횡단 관심사를 담는다:

| 패키지          | 역할                                          |
|--------------|---------------------------------------------|
| `config/`    | 프레임워크 Bean 등록 (Async, Clock, WebMvc, Swagger 등) |
| `aspect/`    | 횡단 관심사 AOP (MessageMappingTracingAspect)    |
| `redis/`     | Redis Stream 인프라, Redisson, 커넥션 설정          |
| `ipblock/`   | IP 차단 (필터, 저장소, 악성 경로 감지, 설정)              |
| `metric/`    | HTTP·Redis Stream Micrometer 메트릭 수집         |
| `trace/`     | OpenTelemetry 연동, Observation 필터            |
| `outbox/`    | Transactional Outbox (이벤트 유실 방지)            |
| `lock/`      | Redisson 기반 분산 락                            |
| `exception/` | ErrorCode 인터페이스, 전역 예외 핸들러                  |
| `health/`    | Spring Actuator 헬스 인디케이터                    |
| `log/`       | 공유 로그 마커 (NotificationMarker 등)             |
| `nickname/`  | 닉네임 유틸 (NameValidator 등, `common/`에서 흡수)    |

최상위 공통 패키지:

| 패키지           | 역할                                                       |
|---------------|----------------------------------------------------------|
| `websocket/`  | STOMP 인터셉터, 세션 추적, 인증, 레이트 리밋, 메시지 복구 (멀티 모듈 분리 예약)      |
| `gamecommon/` | 게임 플로우 추상화 (`FlowScheduler`, `EarlyFinishTrigger`), 공통 메트릭 |

---

## 계층별 역할 구분

### Application Layer
- 유스케이스 단위로 클래스를 나눈다
- 도메인 서비스들을 조합하고 외부 의존성(스케줄러, 알림 등)을 주입받는다
- `{Domain}FlowOrchestrator`: 복잡한 게임 흐름(타이밍, 페이즈 전환)을 관리
- `{Domain}Notifier`: 도메인 이벤트를 WebSocket 메시지로 변환하여 발행

### Domain Layer
- 순수 비즈니스 로직만 포함한다. 스프링 의존성을 최소화한다
- `{Domain}CommandService`: 단일 커맨드 처리 (select, touch, etc.)
- 포트(interface)를 도메인에 정의하고, 구현체는 `infra/`에 위치
- 도메인 이벤트는 record로 정의한다 (`SelectCardCommandEvent`)

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

## 게임 플로우 스케줄링

게임 페이즈 전환(로딩 → 플레이 → 스코어보드)은 `CompletableFuture` 체인으로 구현된다.

```
FlowOrchestrator
  → FlowScheduler (port)
    → CompletableFutureFlowScheduler (infra 구현체)
      → ScheduledExecutorService로 지연 실행
      → EarlyFinishTrigger로 조기 종료 가능
```

타이밍 값은 `application.yml`에서 관리하며, 테스트 시 `application-test.yml`로 500ms~2s 단위로 오버라이드된다.

---

## WebSocket 엔드포인트

| 방향 | 경로                                        | 설명              |
|----|-------------------------------------------|-----------------|
| 구독 | `/topic/room/{joinCode}/gameState`        | 게임 상태 변경 브로드캐스트 |
| 구독 | `/topic/room/{joinCode}/participants`     | 참여자 목록 업데이트     |
| 발행 | `/app/room/{joinCode}/minigame/command`   | 미니게임 커맨드        |
| 발행 | `/app/room/{joinCode}/player/select-card` | 카드 선택           |

STOMP 연결 엔드포인트: `/ws` (SockJS 폴백 지원)
