---
description: 프로덕션 코드 작성 시 핵심 체크 항목. 전체 컨벤션은 docs/conventions-production.md 참고.
paths:
  - "**/src/main/java/**/*.java"
---

전체 컨벤션: `docs/conventions-production.md`

## 자주 놓치는 항목

- `if-else` 대신 early return. then이 `return`/`throw`로 끝나는 `else`는 PMD `NoElseAfterReturn`이 CI에서 잡는다
- 비즈니스 로직은 도메인 객체 안에. 서비스는 조합만
- 조정 가능한 값은 `app/src/main/resources/config/<영역>.yml`(`game.yml`·`service.yml` 등) + `@ConfigurationProperties`. `application.yml`은 import만 한다. 하드코딩 금지
- 식별자·핵심 개념은 record(Value Object). 원시 타입을 시그니처에 직접 노출 금지
- 도메인 계층은 DTO·UI 계층에 의존하지 않는다. 도메인 메서드의 매개변수·반환 타입에 Request/Response DTO를 사용 금지
- 예외 메시지는 한국어로 작성한다
- 도메인 예외는 `CoffeeShoutException` 계열이다. 규칙 위반은 `BusinessException(${Domain}ErrorCode, 메시지)`, 내부 불변식 위반은 `SystemException`(500 ErrorCode). `IllegalStateException`·`IllegalArgumentException`은 PMD `NoRawExceptionInDomain`이 CI에서 막는다
- 데이터베이스 엔티티의 시간 필드는 `LocalDateTime` 대신 `Instant`를 사용한다

## 계층별 클래스 네이밍

| 계층                  | 패턴                            |
|---------------------|-------------------------------|
| Application Service | `{Domain}Service`             |
| 플로우 오케스트레이터         | `{Domain}FlowOrchestrator`    |
| WebSocket 알림        | `{Domain}Notifier`            |
| 커맨드 서비스 (Application) | `{Domain}CommandService`      |
| WebSocket 컨트롤러      | `{Domain}WebSocketController` |
| 커맨드 핸들러             | `{Action}CommandHandler`      |
| Redis Consumer      | `{Event}Consumer`             |
| JPA 영속성 객체          | `{Domain}Entity`              |
| ErrorCode           | `{Domain}ErrorCode`           |

## 예외 계층

```text
CoffeeShoutException
├── BusinessException       — 도메인 규칙 위반
├── InfrastructureException — Redis, DB 오류
└── SystemException         — 시스템 레벨
```

## WebSocket 복구

웹소켓 재접속 복구는 `:websocket` 모듈의 `WsRecoveryService`가 담당한다.
Notifier에서 메시지를 브로드캐스트할 때 `LoggingSimpMessagingTemplate`이 내부적으로
`WsRecoveryService.save()`를 호출해 Redis Stream에 메시지를 백업한다.
클라이언트는 재접속 후 `POST /api/rooms/{joinCode}/recovery?playerName=&lastId=` 로
유실 메시지를 일괄 수신한다.
새 미니게임 Notifier를 작성할 때 별도 복구 로직을 추가하지 않아도 된다.
