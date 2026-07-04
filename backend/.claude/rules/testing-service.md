---
description: 서비스 테스트(ServiceTest) 전용 규칙 — Mock 빈·이벤트 퍼블리셔. 공통은 testing.md.
paths:
  - "src/test/java/**/*ServiceTest.java"
---

전체 컨벤션: `docs/conventions-test.md`. 공통 체크: `testing.md`

## 서비스 테스트 체크

- 베이스: 모듈 로컬 `{Module}ServiceTest`(`coffeeshout.support.ServiceTest` 확장). `src/test/java/coffeeshout/` 아래에 위치
- Mock 빈은 `src/test/java/coffeeshout/config/ServiceTestConfig.java`에 선언한다
- `ApplicationEventPublisher`는 `coffeeshout.support.ServiceTest`가 `@MockitoBean`으로 제공 — `ServiceTestConfig`에 **재선언 금지**
