---
name: impl
description: 새 기능을 도메인 → 서비스 → 컨트롤러 순서로 TDD 사이클로 구현한다. 각 계층 완료 후 /write-tests를 호출하고, 전체 완료 후 code-reviewer를 실행한다.
argument-hint: "[기능명] [--from=domain|service|controller]"
allowed-tools: Read, Glob, Grep, Write, Edit, Bash, Agent
---

`$ARGUMENTS`에서 기능명과 시작 단계(`--from=`)를 파싱한다. `--from`이 없으면 `domain`부터 시작한다.

## 사전 작업

1. `docs/adr/index.md`의 **영향 범위** 컬럼을 작업 대상 패키지/주제와 비교한다
2. 겹치는 ADR이 있으면 해당 파일을 읽어 핵심 제약을 확인한다. 충돌 시 사용자에게 경고 후 진행 여부를 묻는다
3. `docs/` 디렉토리에서 기능명 또는 도메인명과 관련된 파일을 Glob·Grep으로 탐색한다
   - 예: 도메인 모델 명세, WebSocket 스펙, 설계 문서 등
   - 찾은 문서를 읽어 구현 전에 요구사항과 제약을 파악한다
4. 관련 도메인의 기존 코드를 tags로 탐색해 네이밍·구조 컨벤션을 파악한다

---

## Phase 1: 도메인 계층 (`--from=domain`)

**목표**: 비즈니스 규칙을 순수 Java로 표현한다. Spring·JPA 등 외부 의존성 없이 작성한다.

구현 대상:
- Entity, Aggregate Root
- Value Object
- Domain Event
- Domain Service (순수 비즈니스 로직만)
- Repository 인터페이스 (구현체 없이 인터페이스만)

완료 기준:
- [ ] 불변식(invariant)을 생성자 또는 팩토리 메서드에서 검증
- [ ] 외부 의존성(Spring, JPA 애노테이션 등) 없음
- [ ] public 메서드 네이밍에 의도가 드러남

완료 후:
1. **이 시점에서 즉시 멈춘다. Phase 2 코드를 미리 작성하지 않는다.**
2. `/write-tests`를 호출해 도메인 단위 테스트를 작성한다
3. 테스트가 **모두 통과**한 것을 확인한다 (`./gradlew test --tests "해당패키지.*"`)
4. `test-verifier` agent를 `run_in_background: true`로 실행한다
5. 사용자에게 Phase 1 완료를 알리고 Phase 2 진행 여부를 확인한다

---

## Phase 2: 서비스/Orchestrator 계층 (`--from=service`)

**목표**: 도메인 객체를 조합해 유스케이스를 구현한다.

구현 대상:
- Application Service 또는 FlowOrchestrator
- Repository 구현체, 외부 어댑터
- Command/Query DTO

완료 기준:
- [ ] 비즈니스 로직이 서비스 계층으로 누출되지 않음 (도메인 계층에 위임)
- [ ] 트랜잭션 경계가 명확함 (`@Transactional` 위치)
- [ ] 도메인 이벤트 발행 위치가 서비스 계층 이하

완료 후:
1. **이 시점에서 즉시 멈춘다. Phase 3 코드를 미리 작성하지 않는다.**
2. `/write-tests`를 호출해 서비스 단위 테스트를 작성한다
3. 테스트가 **모두 통과**한 것을 확인한다 (`./gradlew test --tests "해당패키지.*"`)
4. `test-verifier` agent를 `run_in_background: true`로 실행한다
5. 사용자에게 Phase 2 완료를 알리고 Phase 3 진행 여부를 확인한다

---

## Phase 3: Controller 계층 (`--from=controller`)

**목표**: HTTP 요청/응답을 처리하고 서비스 계층에 위임한다.

구현 대상:
- REST Controller
- Request/Response DTO
- 통합 테스트 (`@SpringBootTest` 또는 `@WebMvcTest`)

완료 기준:
- [ ] Controller는 요청 파싱·응답 직렬화·서비스 위임만 담당
- [ ] 비즈니스 로직이 Controller에 없음
- [ ] API 응답 형식이 기존 컨벤션과 일치

완료 후:
1. `/write-tests`를 호출해 통합 테스트를 포함한 테스트를 작성한다
2. `test-verifier` agent를 `run_in_background: true`로 실행한다

---

## 기능 개발 완료

모든 Phase가 끝나면:

1. `code-reviewer` agent를 `run_in_background: true`로 실행한다
2. 사용자에게 다음을 요약한다:
   - 구현된 클래스 목록 (패키지 경로 포함)
   - 작성된 테스트 파일 목록
   - 백그라운드 실행 중인 agent 목록 (`test-verifier` × 3, `code-reviewer` × 1)
