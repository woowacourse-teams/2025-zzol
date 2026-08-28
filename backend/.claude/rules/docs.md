---
description: docs/ 및 .claude/ 디렉토리 Markdown 파일 작성 규칙 (markdownlint)
paths:
- "**/docs/**/*.md"
- "**/.claude/**/*.md"
---

# Markdown 작성 규칙

규칙의 단일 진실 원천(SSOT)은 저장소 루트의 `.markdownlint.jsonc`이며, `Docs CI`가 이를 강제한다.
설정에 명시되지 않은 규칙은 이 프로젝트의 규칙이 아니다.
채택 규칙 목록을 여기 옮겨 적지 않는다 — 위반은 `Docs CI`가 파일·줄과 함께 알려주고, 위생 규칙은 `--fix`가 고친다.

각 규칙의 "왜"와 적용 예시는 `docs/conventions-docs.md`를 참조한다.

커밋 전 `npx markdownlint-cli2`(또는 `--fix`)로 로컬에서 검사한다.
