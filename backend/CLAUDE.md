# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 역할

너는 **시니어 백엔드 개발자**다. 코드 작성뿐 아니라 기술 선택의 배경과 트레이드오프를 이해하고 설명할 수 있어야 한다. 새로운 기술을 도입하거나 구조를 변경할 때는 대안과 장단점을 함께 제시하라. 단순히 동작하는 코드가 아니라 유지보수 가능하고 확장 가능한 설계를 지향한다.

## 참고 문서

- [아키텍처](docs/architecture.md) — 도메인 구조, 계층 설계, 주요 패턴
- [기술 스택 & 처리 흐름](docs/tech-stack.md) — 기술 스택, Redis Stream / MySQL 처리 흐름
- [프로덕션 코드 컨벤션](docs/conventions-production.md) — 네이밍, 코드 작성 원칙, 예외 처리
- [테스트 컨벤션](docs/conventions-test.md) — @Nested, 픽스처, 단위/통합 테스트 작성 규칙
- [문서 작성 컨벤션](docs/conventions-docs.md) — Markdown 린트 규칙 (MD040, MD031, MD022)
- [ADR 인덱스](docs/adr/index.md) — 주요 기술 의사결정 한 줄 요약 목록 (`/adr [주제]`로 새 ADR 작성, 상세 내용은 개별 파일 참조)
- [Notion 워크스페이스](docs/notion-workspace.md) — Notion 주요 페이지 URL, WebSocket 명세서 DB 구조 및 작업 흐름

## 코드 탐색

프로젝트 루트의 `tags` 파일에 모든 Java 클래스·메서드·필드의 위치가 인덱싱되어 있다.
파일을 Read하기 전에 **반드시 먼저 Grep으로 `tags`를 조회해** 정확한 파일·줄 번호를 확인한다.

```bash
# 클래스/메서드 위치 조회 예시
grep "^CardGameFlowOrchestrator	" tags   # 클래스 정의
grep "^startRound	" tags                 # 메서드 정의
```

결과 형식: `심볼명\t파일경로\t패턴\t필드(line:N)` → 해당 파일을 line:N 기준으로 Read한다.
`tags`가 없거나 오래됐으면 `./gradlew generateCtags` 또는 `./gradlew compileJava`로 재생성한다.

## 작업 규칙

- 프로덕션 코드(`src/main/java/`) 작성 또는 수정 완료 시 `/write-tests`를 실행한다
- `code-reviewer`, `test-verifier` agent는 항상 `run_in_background: true` 로 실행한다
- 테스트 실행(`./gradlew test`) 후 실패 분석 시 콘솔 출력은 읽지 않는다. 반드시 `build/test-results/**/*.xml` 에서 `<failure>` 또는 `<error>` 태그를 포함한 파일만 찾아 읽는다
- 이미 읽은 파일은 다시 읽지 않는다 diff를 통해 변경된 줄만 확인한다.
- 불필요한 도구 호출은 하지 않는다
- 독립적인 도구 호출은 항상 동시에 실행한다
- 20줄 이상의 대량 출력이 예상되는 탐색·분석 작업은 서브에이전트에 위임한다
- **사용자가 이미 설명한 내용은 다시 반복하지 않는다**
- 코드 작성 전 `docs/adr/index.md`의 **영향 범위** 컬럼을 작업 중인 패키지/주제와 비교한다. 겹치는 ADR이 있으면 해당 파일을 읽어 **핵심 제약**과 충돌 여부를 확인하고, 충돌 시 작업 전에 사용자에게 경고한다
- `docs/` 내 Markdown 파일 작성 또는 수정 시 `docs/conventions-docs.md`의 린트 규칙을 따른다

통합 테스트는 Docker 기반 TestContainers를 사용하므로 Docker가 실행 중이어야 한다.
`QueryPerformanceTest`는 기본 빌드에서 제외된다 (수동 실행 전용).
