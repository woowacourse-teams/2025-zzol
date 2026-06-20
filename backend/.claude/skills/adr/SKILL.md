---
name: adr
description: Architecture Decision Record를 작성한다. 기술 선택, 설계 결정, 패턴 도입 등 팀이 내린 중요한 기술적 의사결정을 기록할 때 사용한다.
argument-hint: "[결정 주제] (예: Redis Stream 도입, Notifier 패턴 적용)"
disable-model-invocation: true
allowed-tools: Read, Glob, Write, Bash
---

`docs/adr/`에 ADR 문서를 작성하고 `docs/adr/index.md`에 한 줄을 추가한다. 형식·상태 값·index 행 형식은 [format.md](format.md)를 따른다.

## 순서

1. **다음 번호 산정** — `docs/adr/`의 `NNNN-*.md` 파일명 최대값과 `index.md` 행 번호 최대값을 **둘 다** 구해 +1 한다. 두 최대값이 다르면 파일과 인덱스가 어긋난 것이니 임의로 진행하지 말고 멈춰서 사용자에게 보고한다 (과거 `0023→0025` 재번호 사고 이력).
2. [format.md](format.md)의 형식으로 `docs/adr/NNNN-{kebab-case-title}.md`를 작성한다. 상태 값은 작성 시점에 맞게 고른다.
3. `index.md` 테이블 맨 끝에 행을 추가한다 ([format.md](format.md)의 행 형식, 번호 오름차순 유지).
4. **markdownlint 검증** — 저장소 루트에서 `npx markdownlint-cli2@0.22.1`을 실행해 통과시킨다 (Docs CI가 be/dev PR에서 강제). 규칙·예시는 `docs/conventions-docs.md`.

## 작성 원칙

- 컨텍스트는 "당시 상황"을 기준으로 서술한다. 나중에 읽는 사람이 왜 이 결정을 내렸는지 이해할 수 있어야 한다
- 고려한 대안은 실제로 검토한 것만 작성한다
- 트레이드오프는 단점을 숨기지 않고 솔직하게 작성한다
