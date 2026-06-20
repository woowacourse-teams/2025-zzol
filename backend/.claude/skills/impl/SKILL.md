---
name: impl
description: 새 기능을 도메인 → 서비스 → 컨트롤러 순서로 TDD 사이클로 구현한다. 각 계층 완료 후 /write-tests를 호출하고, 전체 완료 후 code-reviewer를 실행한다.
argument-hint: "[기능명] [--from=domain|service|controller]"
allowed-tools: Read, Glob, Grep, Write, Edit, Bash, Agent, Skill
---

# impl

`$ARGUMENTS`에서 기능명과 시작 단계(`--from=`)를 파싱한다. `--from`이 없으면 `domain`부터 시작한다.

## 사전 작업

0. `docs/specs/{기능명}.md` 명세가 있으면 읽어 **명확화 결과·설계·성공 기준**을 구현 기준으로 삼는다 (이미 `/design-first`로 합의된 상태).
1. `docs/adr/index.md`의 **영향 범위** 컬럼을 작업 대상 패키지/주제와 비교한다
2. 겹치는 ADR이 있으면 해당 파일을 읽어 핵심 제약을 확인한다. 충돌 시 사용자에게 경고 후 진행 여부를 묻는다
3. `docs/` 디렉토리에서 기능명 또는 도메인명과 관련된 파일을 Glob·Grep으로 탐색한다
   - 예: 도메인 모델 명세, WebSocket 스펙, 설계 문서 등
   - 찾은 문서를 읽어 구현 전에 요구사항과 제약을 파악한다
4. 관련 도메인의 기존 코드를 Grep으로 탐색해 네이밍·구조 컨벤션을 파악한다

## 요구사항·예외 케이스 합의 (구현 전 필수)

> `docs/specs/{기능명}.md` 명세가 있으면 그 **명확화 결과·성공 기준을 합의 결과로 채택**하고 이 절을 생략한다 — 무거운 합의는 `/design-first`가 이미 끝냈다. 명세가 없을 때만 아래 인라인 합의를 진행한다.

사전 작업으로 파악한 내용을 바탕으로, **Phase 1 코드를 작성하기 전에** 사용자와 범위·예외를 합의한다. 추측으로 구현하지 않는다(CLAUDE.md).

1. **무엇을 만들지 한 문단으로 요약**해 제시한다 — 대상 계층/클래스, 핵심 동작, 건드릴 모듈.
2. **열린 질문으로 예외·경계 케이스를 함께 짚는다.** 고정 체크리스트가 아니라, 사전 작업에서 파악한 도메인 맥락에서 실제로 동작이 갈릴 지점을 질문한다 — 빈 입력·경계값, 동시성·순서, 실패·롤백, 잘못된 상태 전이, 권한·인증, 중복 요청 등. "이 경우엔 어떻게 동작해야 하나요?" 식으로 사용자가 결정하게 한다.
3. 모호한 목표는 **검증 가능한 성공 기준**으로 바꿔 합의한다. 이슈에서 시작했다면 이슈의 `✅ 성공 기준`·`🔧 TODO`를 먼저 읽어 채운다.
4. 합의된 범위·예외·성공 기준을 확인받은 뒤에만 Phase 1로 넘어간다. 합의 내용이 ADR 핵심 제약과 충돌하면 멈추고 보고한다.

## Phase 완료 루틴 (모든 Phase 공통)

각 Phase의 구현이 끝나면 순서대로 수행한다.

1. **즉시 멈춘다 — 다음 Phase 코드를 미리 작성하지 않는다.** (마지막 Phase면 생략)
2. `/write-tests`로 해당 계층 테스트를 작성한다 (베이스 상속·픽스처 등 컨벤션은 `/write-tests`가 SSOT).
3. `/run-tests <해당패키지>.* --sync`로 모든 테스트 통과를 확인한다.
4. `test-verifier` agent를 `run_in_background: true`로 실행한다.
5. **커밋** — `bash .claude/skills/commit/preflight.sh`로 보호 브랜치·detached HEAD를 차단(`ABORT` 출력 시 중단·보고)한 뒤, 해당 계층 파일만 `git add` 후 `feat: [기능명] <계층> 구현`으로 커밋한다. Phase 테스트는 이미 검증했으므로 `/commit` 재검증은 생략한다.
6. 사용자에게 Phase 완료를 알리고 다음 Phase 진행 여부를 확인한다. (마지막 Phase면 생략)

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

→ **완료 후: [Phase 완료 루틴]** (커밋 메시지 `feat: [기능명] 도메인 계층 구현`)

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

→ **완료 후: [Phase 완료 루틴]** (커밋 메시지 `feat: [기능명] 서비스 계층 구현`)

---

## Phase 3: Controller 계층 (`--from=controller`)

**목표**: HTTP 요청/응답을 처리하고 서비스 계층에 위임한다.

구현 대상:
- REST Controller
- Request/Response DTO
- WebSocket Handler (해당 시): 커맨드 수신 후 **Redis Stream에 발행**한다. Application Service를 직접 호출하지 않는다 — CLAUDE.md 핵심 제약(Handler → Stream 발행 → Consumer → Service → Notifier). Stream을 건너뛰면 비동기 처리 보장이 깨진다. (REST Controller는 Service 직접 위임 OK)

완료 기준:
- [ ] Controller는 요청 파싱·응답 직렬화·서비스 위임만 담당
- [ ] 비즈니스 로직이 Controller에 없음
- [ ] API 응답 형식이 기존 컨벤션과 일치
- [ ] WebSocket 경로는 Application Service 직접 호출 없이 Stream 발행을 경유

→ **완료 후: [Phase 완료 루틴]** (커밋 메시지 `feat: [기능명] 컨트롤러 계층 구현`). 통합 테스트 컨벤션(베이스 상속·`@WebMvcTest` 금지·REST `MOCK`/WebSocket `RANDOM_PORT`)은 `/write-tests`가 SSOT다.

---

## 기능 개발 완료

모든 Phase가 끝나면:

1. `code-reviewer` agent를 `run_in_background: true`로 실행한다
2. 사용자에게 다음을 요약한다:
   - 구현된 클래스 목록 (패키지 경로 포함)
   - 작성된 테스트 파일 목록
   - 백그라운드 실행 중인 agent 목록 (`test-verifier` × Phase 수, `code-reviewer` × 1)
