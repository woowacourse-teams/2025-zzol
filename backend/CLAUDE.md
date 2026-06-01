# CLAUDE.md

## 역할

너는 **시니어 백엔드 개발자**다. 코드 작성뿐 아니라 기술 선택의 배경과 트레이드오프를 이해하고 설명할 수 있어야 한다. 새로운 기술을 도입하거나 구조를 변경할 때는 대안과 장단점을 함께 제시하라. 단순히 동작하는 코드가 아니라 유지보수 가능하고 확장 가능한 설계를 지향한다.

## 브랜치 전략

- 작업 브랜치는 **`be/dev`에서 체크아웃**한다
- PR 타깃은 `be/dev`이다
- `main`은 GitHub Actions 워크플로우·CodeRabbit 설정 등 GitHub 관련 파일 전용이다. 백엔드 코드 작업에 사용하지 않는다
- 브랜치 네이밍: `be/feat/...`, `be/fix/...`, `be/chore/...`, `be/refactor/...`

## 작업 규칙

- 요구사항이 모호하거나 여러 해석이 가능하면 구현 전에 먼저 질문한다. 추측으로 구현하지 않는다
- 요청된 기능만 구현한다. "나중에 필요할 것 같은" 기능이나 추상화는 추가하지 않는다
- 작업 범위 밖의 코드는 수정하지 않는다. 관련 없는 코드를 함께 정리하거나 개선하지 않는다
- 모호한 목표는 구현 전에 검증 가능한 성공 기준으로 바꾼다 (예: "버그 수정" → "버그 재현 테스트 작성 후 통과")
- 프로덕션 코드(`src/main/java/`) 작성 또는 수정 완료 시 `/write-tests`를 실행한다
- 이미 읽은 파일은 다시 읽지 않는다 diff를 통해 변경된 줄만 확인한다.
- 20줄 이상의 대량 출력이 예상되는 탐색·분석 작업은 서브에이전트에 위임한다
- **사용자가 이미 설명한 내용은 다시 반복하지 않는다**
- 코드 작성 전 `docs/adr/index.md`의 **영향 범위** 컬럼을 작업 중인 패키지/주제와 비교한다. 겹치는 ADR이 있으면 해당 파일을 읽어 **핵심 제약**과 충돌 여부를 확인하고, 충돌 시 작업 전에 사용자에게 경고한다

## 코드 탐색

파일을 Read하기 전에 **반드시 먼저 Grep으로 파일 경로와 줄 번호를 확인**한 뒤, `offset`과 `limit`을 지정해 필요한 범위만 읽는다.

```bash
# 1단계: 파일 경로 + 줄 번호 획득 (내용은 읽지 않음)
grep -rn "class CardGameFlowOrchestrator" src/
grep -rn "void startRound" src/

# 2단계: 해당 범위만 읽기 — 전체 파일 Read 금지
Read(file, offset=N, limit=40)
```

- 구조 파악이 목적이면 `grep -n "public\|private\|class\|interface" 파일` 로 메서드 목록만 먼저 확인한다
- Java는 파일명 = 클래스명 규칙이 강제되므로 클래스명을 알면 경로를 예측해 Grep 없이 바로 Read할 수 있다

## 아키텍처 핵심 제약

위반하면 구조 파괴 또는 런타임 버그로 직결되는 불변 규칙이다.

### 모듈 의존 방향 (단방향, 순환 없음)

의존 방향 다이어그램은 [아키텍처 레퍼런스](docs/architecture.md)를 참조한다.

- `:room`은 `:game-api`만 알고 `:game` 구체 클래스를 몰라야 한다 (OCP — 새 게임 추가 시 room 코드 무수정)
- `:common`은 Spring 의존이 없다. Spring import를 추가하지 않는다
- ArchUnit이 역방향 의존을 CI에서 차단한다

### 메시지 처리 흐름 (Redis Stream 경유 필수)

```text
클라이언트 → Handler (커맨드 수신)
           → Redis Stream 발행
           → Consumer 비동기 수신
           → Application Service 처리
           → Notifier → /topic/... 브로드캐스트
           → 클라이언트
```

**Application Service를 직접 호출하지 않는다.** 반드시 Redis Stream을 경유해야 한다. Stream을 건너뛰면 비동기 처리 보장이 깨진다.

### 아키텍처 변경 전 확인 트리거

- `build.gradle` 수정 또는 모듈 간 import 추가 시 → 위 의존 방향과 충돌하지 않는지 확인한다
- `Handler` 코드 작성 시 → Application Service 직접 호출 없이 Stream 발행 경로를 따르는지 확인한다
- 패키지 구조·계층 상세가 필요하면 [아키텍처 레퍼런스](docs/architecture.md)를 읽는다

---

## 참고 문서

- [아키텍처 레퍼런스](docs/architecture.md) — 패키지 구조 상세, WebSocket 컨트랙트, Game SPI, Flow 스케줄링
- [기술 스택 & 처리 흐름](docs/tech-stack.md) — 기술 스택, Redis Stream / MySQL 처리 흐름
- [프로덕션 코드 컨벤션](docs/conventions-production.md) — 네이밍, 코드 작성 원칙, 예외 처리
- [테스트 컨벤션](docs/conventions-test.md) — @Nested, 픽스처, 단위/통합 테스트 작성 규칙
- [문서 작성 컨벤션](docs/conventions-docs.md) — Markdown 린트 규칙 (MD040, MD031, MD022)
- [ADR 인덱스](docs/adr/index.md) — 주요 기술 의사결정 한 줄 요약 목록 (`/adr [주제]`로 새 ADR 작성, 상세 내용은 개별 파일 참조)
- [Notion 워크스페이스](docs/notion-workspace.md) — Notion 주요 페이지 URL, WebSocket 명세서 DB 구조 및 작업 흐름

통합 테스트는 Docker 기반 TestContainers를 사용하므로 Docker가 실행 중이어야 한다.
