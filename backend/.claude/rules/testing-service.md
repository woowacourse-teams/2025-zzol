---
description: 서비스 테스트(ServiceTest) 전용 규칙 — Mock 빈·이벤트 퍼블리셔. 공통은 testing.md.
paths:
  - "**/src/test/java/**/*ServiceTest.java"
---

전체 컨벤션: `docs/conventions-test.md`. 공통 체크: `testing.md`

## 이 계층에서 무엇을 (ADR-0033)

**테스트할 것**: Spring이 매개하는 것. 트랜잭션 경계·조합·이벤트 발행·요청 매핑·직렬화·쿼리 정확성이 여기 해당한다. 외부 의존은 목.

**자제할 것**: 단위로 되는 순수 도메인 분기 재검증(중복 커버리지). 슬라이스(`@DataJpaTest`·`@WebMvcTest`·`@JsonTest`)로 충분하면 풀 컨텍스트(`@SpringBootTest`) 지양.

## 서비스 테스트 체크

- 베이스: 모듈 로컬 `{Module}ServiceTest`(`coffeeshout.support.ServiceTest` 확장). `src/test/java/coffeeshout/` 아래에 위치
- Mock 빈은 `src/test/java/coffeeshout/config/ServiceTestConfig.java`에 선언한다
- `ApplicationEventPublisher`는 `coffeeshout.support.ServiceTest`가 `@MockitoBean`으로 제공한다. `ServiceTestConfig`에 **재선언 금지**
