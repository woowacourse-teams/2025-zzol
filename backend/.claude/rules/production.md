---
description: 프로덕션 코드 작성 시 핵심 체크 항목. 전체 컨벤션은 docs/conventions-production.md 참고.
paths:
  - "**/src/main/java/**/*.java"
---

전체 컨벤션: `docs/conventions-production.md`

## 자주 놓치는 항목

- `if-else` 금지 → early return (then 절이 return·throw로 끝나는 형태는 PMD `NoElseAfterReturn`)
- 비즈니스 로직은 도메인 객체 안에. 서비스는 조합만
- 조정 가능한 값은 `application.yml` + `@ConfigurationProperties`. 하드코딩 금지
- 식별자·핵심 개념은 record(Value Object). 원시 타입을 시그니처에 직접 노출 금지
- 도메인 계층은 DTO·UI 계층에 의존하지 않는다. 도메인 메서드의 매개변수·반환 타입에 Request/Response DTO를 사용 금지
- 예외 메시지는 한국어로 작성한다
- 도메인에서 던지는 예외는 반드시 `BusinessException(${Domain}ErrorCode, 메시지)` 형태로 작성한다
- 데이터베이스 엔티티의 시간 필드는 `LocalDateTime` 대신 `Instant`를 사용한다 (기존 7곳 전환은 #1718)

## 계층별 클래스 네이밍

| 계층                  | 패턴                         | 기계 검증          |
|---------------------|----------------------------|----------------|
| Application Service | `{Domain}Service`          | ArchUnit       |
| 플로우 오케스트레이터         | `{Domain}FlowOrchestrator` | ArchUnit       |
| WebSocket 알림        | `{Domain}Notifier`         | ArchUnit       |
| ErrorCode           | `{Domain}ErrorCode`        | ArchUnit       |
| 도메인 서비스             | `{Domain}CommandService`   | 리뷰             |
| 커맨드 핸들러             | `{Action}CommandHandler`   | 리뷰             |
| Redis Consumer      | `{Event}Consumer`          | 리뷰             |
| JPA 영속성 객체          | `{Domain}Entity`           | 리뷰             |

ArchUnit 열은 `..application..`의 `@Service`와 `ErrorCode` 구현체에 걸린다
(`app/src/test/java/coffeeshout/arch/ConventionArchitectureTest.java`).
나머지는 패키지·애노테이션만으로 대상을 특정할 수 없어 리뷰가 맡는다.

## 예외 계층

```text
CoffeeShoutException
├── BusinessException       — 도메인 규칙 위반
├── InfrastructureException — Redis, DB 오류
└── SystemException         — 시스템 레벨
```

파일 하나를 차지하는 `RuntimeException` 하위가 이 계층 밖에 있으면 ArchUnit이 막는다.

`..domain..`·`..application..`·`..event..`에서 JDK 런타임 예외를 직접 생성하는 것은 PMD가 막는다
(`NoRawExceptionInDomain` — `RuntimeException`·`IllegalArgumentException`·`IllegalStateException`·
`NullPointerException` 등 11종). **그 밖의 계층(`ui`·`infra`·`config`)은 아직 리뷰가 본다** — 범위 확대는 #1654다.

## WebSocket 복구

웹소켓 재접속 복구는 `global/websocket/GameRecoveryService`가 담당한다.
Notifier에서 메시지를 브로드캐스트할 때 `LoggingSimpMessagingTemplate`이 내부적으로
`GameRecoveryService.save()`를 호출해 Redis Stream에 메시지를 백업한다.
클라이언트는 재접속 후 `POST /api/rooms/{joinCode}/recovery?playerName=&lastId=` 로
유실 메시지를 일괄 수신한다.
새 미니게임 Notifier를 작성할 때 별도 복구 로직을 추가하지 않아도 된다.
