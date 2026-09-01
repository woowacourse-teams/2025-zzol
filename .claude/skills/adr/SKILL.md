---
name: adr
description: Architecture Decision Record를 작성한다. 기술 선택, 설계 결정, 패턴 도입 등 팀이 내린 중요한 기술적 의사결정을 기록할 때 사용한다.
argument-hint: "[결정 주제] (예: Redis Stream 도입, Notifier 패턴 적용)"
allowed-tools: Read, Glob, Write, Bash
---

영역에 따라 저장 위치와 방식이 다르다.

- **전역/크로스커팅** (브랜치 전략·릴리스·모노레포 구조 등 BE/FE 어느 한쪽 관심사가 아닌 결정) → 루트 `docs/adr/`. **이슈/PR 번호를 ID**로 쓰고(`adr-<번호>-<slug>.md`), frontmatter가 SSOT이며 `index.md`는 **생성물**이다(`node docs/adr/gen-index.mjs`). 수동 index 편집·`NNNN` 시퀀스 없음.
- **BE/FE 국한** → `backend/docs/adr/`·`frontend/docs/adr/`. 레거시 방식: `NNNN` 순차 번호 + `index.md` 수동 한 줄 추가.

형식·상태 값·frontmatter/index 규약은 [format.md](format.md)를 따른다. 어느 영역인지 모호하면 사용자에게 묻는다. 내용 검색("X 관련 ADR 있나?")은 qmd(`qmd query "..." --collection zzol-docs`)를 1차 경로로 쓴다 — index 표는 상태·카탈로그 조회용이다.

## 순서 — 전역 (루트 `docs/adr/`)

1. **ID = 이슈/PR 번호.** 이 결정을 다루는 이슈 번호를 그대로 ID로 쓴다(전역 유일 → 병렬 작성 시 번호 충돌 없음).
2. [format.md](format.md)의 frontmatter + 본문 형식으로 `docs/adr/adr-<번호>-<kebab-slug>.md`를 쓴다. frontmatter 필수 키: `id`·`title`·`status`·`date`·`scope`·`constraint`.
3. **index 생성** — `node docs/adr/gen-index.mjs` 실행. `index.md`는 직접 편집하지 않는다.
4. **markdownlint 검증** — 루트에서 `npx markdownlint-cli2` 통과. Docs CI가 `dev` PR에서 index 신선도(`--check`) + 린트를 강제한다.

## 순서 — BE/FE (레거시)

1. **저장 위치 결정** — 백엔드면 `backend/docs/adr/`, 프론트면 `frontend/docs/adr/`을 `$ADR_DIR`로 둔다. `frontend/docs/adr/index.md`가 없으면 [format.md](format.md)의 헤더로 새로 만든다.
2. **다음 번호 산정** — `$ADR_DIR`의 `NNNN-*.md` 파일명 최대값과 `index.md` 행 번호 최대값을 **둘 다** 구해 +1 한다. 두 최대값이 다르면 멈춰서 사용자에게 보고한다 (과거 `0023→0025` 재번호 사고 이력). 번호는 영역별 독립 시퀀스다.
3. [format.md](format.md)의 형식으로 `$ADR_DIR/NNNN-{kebab-case-title}.md`를 작성하고, `$ADR_DIR/index.md` 테이블 맨 끝에 행을 추가한다(번호 오름차순).
4. **markdownlint 검증** — 루트에서 `npx markdownlint-cli2` 통과. 백엔드 규칙·예시는 `backend/docs/conventions-docs.md`.

## 작성 원칙

- 컨텍스트는 "당시 상황"을 기준으로 서술한다. 나중에 읽는 사람이 왜 이 결정을 내렸는지 이해할 수 있어야 한다
- 고려한 대안은 실제로 검토한 것만 작성한다
- 불필요하게 쓰인 영어는 쉬운 한국어로 옮길 수 있는지 먼저 고민한다 (예: reconcile → 차이 맞추기, divergence → 분기)
- 선택한 기술·방안이 우리 팀의 현재 제약과 어떻게 부합하는지 명시한다
- 간결한 문장과 능동태로 쓴다
- 이 결정으로 생기는 이점과 잠재적 위험을 솔직하게 적는다 (단점·트레이드오프를 숨기지 않는다)
- 내용이 복잡하면 아키텍처 흐름·변경점을 간단한 mermaid 다이어그램이나 흐름도로 함께 보인다 (코드 펜스는 ```` ```mermaid ````)
