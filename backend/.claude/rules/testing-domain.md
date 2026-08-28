---
description: 도메인 단위 테스트 전용 규칙 — 순수 Java, 컨텍스트 없음. 공통은 testing.md.
paths:
  - "**/src/test/java/**/domain/**/*Test.java"
---

전체 컨벤션: `docs/conventions-test.md`. 공통 체크: `testing.md`

## 이 계층에서 무엇을 (ADR-0033)

**테스트할 것**: 비즈니스 분기·상태전이·스코어링·경계값·불변식을 **여기서 소진**한다(피라미드 밑변). `입력 x,y → 결과 z` 형태로 표현되는 모든 로직.

**자제할 것**: 트리비얼 getter/DTO, private 메서드(테스트하고 싶으면 클래스 분리 신호), 프레임워크/라이브러리 동작, 과잉 목킹(협력자는 목 대신 Fake/Stub).

## 도메인 단위 테스트 체크

- 베이스 클래스 **없음** — 순수 Java 단위 테스트. PMD가 막는다(`NoBaseClassInDomainTest`·`NoSpringContextInDomainTest`).
  단 `..domain.service..`의 `*ServiceTest`는 이름이 말하는 대로 `testing-service.md`가 관할한다
- 도메인 객체의 비즈니스 로직만 검증한다. 협력 객체가 필요하면 Fake/Stub 픽스처로 대체한다
- DB·Redis·네트워크 등 외부 의존이 필요하면 도메인 단위 테스트가 아니다 → 서비스(`testing-service.md`) 또는 통합(`testing-integration.md`) 계층으로 올린다
