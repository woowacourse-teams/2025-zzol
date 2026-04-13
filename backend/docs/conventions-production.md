# 프로덕션 코드 컨벤션

## 네이밍

### 계층별 클래스 네이밍

| 계층                  | 패턴                                       | 예시                               |
|---------------------|------------------------------------------|----------------------------------|
| Application Service | `{Domain}Service`                        | `CardGameService`                |
| 플로우 오케스트레이터         | `{Domain}FlowOrchestrator`               | `CardGameFlowOrchestrator`       |
| WebSocket 알림        | `{Domain}Notifier`                       | `CardGameNotifier`               |
| 도메인 서비스             | `{Domain}CommandService`                 | `CardGameCommandService`         |
| WebSocket 컨트롤러      | `{Domain}WebSocketController`            | `RoomWebSocketController`        |
| 커맨드 핸들러             | `{Action}CommandHandler`                 | `SelectCardCommandHandler`       |
| Redis Consumer      | `{Event}Consumer`                        | `SelectCardCommandEventConsumer` |
| 도메인 이벤트             | `{Action}CommandEvent` / `{Domain}Event` | `SelectCardCommandEvent`         |
| JPA 엔티티             | `{Domain}Entity`                         | `RoomEntity`                     |
| ErrorCode           | `{Domain}ErrorCode`                      | `CardGameErrorCode`              |
| 요청 객체               | `{Action}Request` / `{Action}Message`    | `RoomEnterRequest`               |

### 값 객체(Value Object)
도메인의 식별자와 핵심 개념은 record로 정의한다. 원시 타입(`String`, `int`)을 도메인 메서드 시그니처에 그대로 노출하지 않는다.

---

## 코드 작성 원칙

- 클래스는 단일 책임을 갖는다. 변경 이유가 두 가지 이상이라면 클래스를 분리한다
- 인스턴스 변수는 가능한 한 `final`로 선언한다
- 메서드 블록 내 지역 변수에는 `final`을 붙인다. 매개변수에는 붙이지 않는다
- `if-else` 사용을 지양하고 early return으로 중첩을 줄인다
- 들여쓰기 깊이가 깊어지면 메서드로 분리한다. 한 메서드는 한 가지 추상화 수준의 일만 한다
- 외부 의존성(시간, 랜덤, I/O)은 파라미터로 주입받아 테스트에서 제어 가능하게 만든다
- 사이드 이펙트가 있는 로직과 계산 로직을 분리한다
- 서비스는 트랜잭션 스크립트 패턴이 되지 않도록 한다. 비즈니스 로직은 도메인 객체 안에 두고, 서비스는 도메인 객체를 조합하는 역할만 담당한다
- 상태 변경 메서드는 결과를 반환하여 테스트에서 검증 가능하게 한다

---

## 예외 처리

- 모든 도메인 에러코드는 `ErrorCode` 인터페이스를 구현하는 enum으로 정의한다
- 모든 커스텀 예외는 `CoffeeShoutException`을 상속한다

```
CoffeeShoutException (abstract)
├── BusinessException        — 도메인 규칙 위반, 잘못된 상태, 존재하지 않는 엔티티 등 비즈니스 로직 예외
├── InfrastructureException  — Redis, DB 등 외부 인프라 연동 오류
└── SystemException          — 시스템 레벨 예외
```

---

## 도메인 이벤트 작성

- 이벤트는 record로 정의하고 `BaseEvent`를 구현한다
- 컴팩트 생성자에서 `eventId`(UUID)와 `timestamp`(Instant.now())를 자동 생성한다
- 분산 추적이 필요하면 `Traceable`도 함께 구현한다

> **외부 의존성 주입 원칙과의 관계**: `eventId`·`timestamp`는 "이벤트가 생성된 사실 자체"를 기록하는 메타데이터이므로, 비즈니스 로직 테스트의 격리 대상이 아니다. 이벤트 생성 시점이 테스트에 영향을 준다면 이벤트 객체 자체보다 그것을 소비하는 쪽의 설계를 먼저 검토한다.

---

## 설정 관리

타이밍, 스레드풀 크기 등 조정 가능한 값은 `application.yml`에 선언하고 `@ConfigurationProperties`로 바인딩한다. 하드코딩하지 않는다.
