---
description: 모든 테스트 공통 핵심 체크. 경계별 상세는 testing-integration/service/domain.md, 전체는 docs/conventions-test.md.
paths:
  - "**/src/test/java/**/*.java"
---

전체 컨벤션: `docs/conventions-test.md`

경계별 규칙(해당 테스트 파일을 열면 자동 로드): 통합 `testing-integration.md` · 서비스 `testing-service.md` · 도메인 `testing-domain.md`

## 회귀 테스트는 돌연변이로 검증한다

테스트를 추가·수정했으면 **대상 코드를 일부러 망가뜨려 그 테스트가 실패하는지 확인한다**(분기 뒤집기, 호출 주석 처리). 통과만 확인하면 아무것도 안 재는 테스트가 초록으로 남는다. 실제로 그런 사례가 이 저장소에 있었다(#1699).

## 테스트 피라미드 (ADR-0033)

케이스는 **가능한 낮은 계층에서 소진**한다. 판단 규칙: "이 케이스를 더 적은 컨텍스트로도 같은 이유로 실패시킬 수 있나?" → 예면 내린다. 상위 테스트가 빨간데 대응하는 하위 테스트가 없으면 하위 테스트가 누락된 것이다(작성 후 상위 중복 여부 판단).

- 도메인 분기·경계값 → 단위 (`testing-domain.md`)
- 트랜잭션·조합·이벤트·매핑·쿼리 → 서비스 (`testing-service.md`)
- 비동기 왕복·실제 영속성·WS 해피패스·플로우 중 예외 → 통합 (`testing-integration.md`), **얇게 유지**

베이스 클래스 선택·`:test-support` 사용법은 `docs/conventions-test.md`가 단일 출처다(여기 복사하지 않는다).

## 자주 놓치는 항목 (공통)

- 테스트 메서드명은 한글
- 복수 검증은 `SoftAssertions`
- `Thread.sleep` 금지 → Awaitility
- 테스트 데이터 직접 생성 금지 → 픽스처 사용. 클래스명은 반드시 5가지 패턴 중 하나: `*Fixture` / `TestDataHelper` / `*Fake` / `*Dummy` / `Stub*`
- `CoffeeShoutException` 계열은 `assertCoffeeShoutException` 사용. `assertThatThrownBy` 체인 직접 작성 금지
