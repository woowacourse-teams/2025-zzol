---
description: 프로덕션 코드 작성 시 핵심 체크 항목. 전체 컨벤션은 docs/conventions-production.md 참고.
paths:
  - "src/main/java/**/*.java"
---

전체 컨벤션: `docs/conventions-production.md`

## 자주 놓치는 항목

- 인스턴스 변수·지역 변수는 `final`. 매개변수는 제외
- `if-else` 금지 → early return
- 비즈니스 로직은 도메인 객체 안에. 서비스는 조합만
- 조정 가능한 값은 `application.yml` + `@ConfigurationProperties`. 하드코딩 금지
- 식별자·핵심 개념은 record(Value Object). 원시 타입을 시그니처에 직접 노출 금지

## 계층별 클래스 네이밍

| 계층                  | 패턴                         |
|---------------------|----------------------------|
| Application Service | `{Domain}Service`          |
| 플로우 오케스트레이터         | `{Domain}FlowOrchestrator` |
| WebSocket 알림        | `{Domain}Notifier`         |
| 도메인 서비스             | `{Domain}CommandService`   |
| 커맨드 핸들러             | `{Action}CommandHandler`   |
| Redis Consumer      | `{Event}Consumer`          |
| JPA 영속성 객체          | `{Domain}Entity`           |
| ErrorCode           | `{Domain}ErrorCode`        |

## 예외 계층

```text
CoffeeShoutException
├── BusinessException       — 도메인 규칙 위반
├── InfrastructureException — Redis, DB 오류
└── SystemException         — 시스템 레벨
```
