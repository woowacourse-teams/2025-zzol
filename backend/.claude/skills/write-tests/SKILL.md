---
name: write-tests
description: 프로덕션 코드(src/main/java/) 작성 또는 수정 후 테스트 코드를 작성한다. 새로운 클래스나 메서드를 구현했을 때, 테스트가 없는 코드를 작성했을 때 사용한다.
allowed-tools: Read, Glob, Grep, Write, Edit
---

방금 작성하거나 수정한 프로덕션 코드에 대한 테스트를 작성한다.

## 순서

1. 대상 클래스의 public 메서드와 책임을 파악한다
2. 해당 **모듈의** `src/test/java/` 하위 동일 패키지에 테스트 파일을 생성한다 (멀티모듈 — 경로는 `<module>/src/test/java/coffeeshout/...`)
3. `docs/conventions-test.md`의 컨벤션을 따른다

## 반드시 지킬 규칙

- **모듈 로컬 베이스를 상속한다** — 서비스 테스트는 `{Module}ServiceTest`, 통합 테스트는 `{Module}IntegrationTest`(각각 `coffeeshout.support.ServiceTest` / `IntegrationTestSupport` 확장). 이 상속이 TestContainers(MySQL·Valkey)와 `test` 프로파일을 자동 구동한다 — 빠뜨리면 컨테이너가 안 떠 테스트가 깨진다 (ADR-0015). `@IntegrationTest` 같은 애노테이션은 없다(베이스 상속이다)
- **통합 테스트 종류별** — REST는 기본 `MOCK`(`@WebMvcTest` 금지), WebSocket은 `WebSocketIntegrationTestSupport` 상속 + `RANDOM_PORT` (ADR-0017)
- Fixture를 활용한다(없으면 생성). 모듈 간 **공유**는 `src/testFixtures/java/coffeeshout/fixture/`, 모듈 **내부 전용**은 `src/test/java/coffeeshout/fixture/`. 유형 구분: `*Fixture`(팩토리)·`*Fake`·`*Dummy`·`Stub*`·`TestDataHelper`(@Component DB헬퍼) (ADR-0016)
- `@Nested`로 시나리오를 그룹화한다
- 테스트 메서드명은 한글로 작성한다
- 복수 검증은 `SoftAssertions`를 사용한다

## 테스트하기 어려운 설계 판단 기준

테스트 작성 전에 다음 항목을 먼저 검토한다. 해당 항목이 발견되면 **테스트 코드 작성 전에 사용자에게 프로덕션 코드 변경을 먼저 제안**한다.

| 징후                                                          | 문제                 | 제안 방향                                                                                        |
|-------------------------------------------------------------|--------------------|----------------------------------------------------------------------------------------------|
| **비즈니스 로직 메서드**에서 `Instant.now()`, `Math.random()` 등을 직접 호출 | 테스트에서 결과를 제어할 수 없음 | 파라미터로 주입받도록 변경. 단, 도메인 이벤트·값 객체의 팩토리성 생성자(eventId, timestamp 자동 생성 등)는 이 프로젝트의 허용 패턴이므로 제외한다 |
| `new 구체클래스()`를 메서드 내부에서 직접 생성                               | 의존성 교체가 불가능        | 생성자 주입 또는 팩토리 파라미터로 변경                                                                       |
| 하나의 메서드가 여러 추상화 수준의 일을 동시에 처리                               | 단위 테스트 범위가 불명확     | 메서드 분리 후 각각 테스트                                                                              |
| void 메서드가 상태 변경 외에 아무것도 반환하지 않음                             | 결과 검증 불가           | 변경 결과를 반환하도록 수정                                                                              |
| `static` 메서드에 외부 의존성이 숨어 있음                                 | 모킹 불가              | 인스턴스 메서드로 전환                                                                                 |

변경 제안 시 다음 형식으로 사용자에게 설명한다:

```markdown
테스트하기 어려운 설계가 발견되었습니다.

[발견된 문제]
- 구체적인 코드 위치와 이유

[제안하는 변경]
- 변경 전 코드
- 변경 후 코드

변경 후 테스트를 작성하겠습니다. 변경을 진행할까요?
```
