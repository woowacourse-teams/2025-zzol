---
description: 도메인 단위 테스트 전용 규칙 — 순수 Java, 컨텍스트 없음. 공통은 testing.md.
paths:
  - "src/test/java/**/domain/**/*Test.java"
---

전체 컨벤션: `docs/conventions-test.md`. 공통 체크: `testing.md`

## 도메인 단위 테스트 체크

- 베이스 클래스 **없음** — 순수 Java 단위 테스트. Spring 컨텍스트·TestContainer를 띄우지 않는 가장 빠른 계층이다
- 도메인 객체의 비즈니스 로직만 검증한다. 협력 객체가 필요하면 Fake/Stub 픽스처로 대체한다
- DB·Redis·네트워크 등 외부 의존이 필요하면 도메인 단위 테스트가 아니다 → 서비스(`testing-service.md`) 또는 통합(`testing-integration.md`) 계층으로 올린다
