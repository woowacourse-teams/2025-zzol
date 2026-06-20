---
description: docs/ 및 .claude/ 디렉토리 Markdown 파일 작성 규칙 (markdownlint)
paths:
- "docs/**/*.md"
- ".claude/**/*.md"
---

# Markdown 작성 규칙

규칙의 단일 진실 원천(SSOT)은 저장소 루트의 `.markdownlint.jsonc`이며, `Docs CI`가 이를 강제한다.
설정에 명시되지 않은 규칙은 이 프로젝트의 규칙이 아니다.

현재 채택 규칙:

- 렌더링: `MD040`(코드 블록 언어 지정), `MD031`(코드 블록 앞뒤 빈 줄), `MD022`(헤딩 앞뒤 빈 줄)
- 위생(자동 수정): `MD047`(파일 끝 단일 개행), `MD012`(연속 빈 줄 금지), `MD007`(중첩 리스트 2칸 들여쓰기)

각 규칙의 "왜"와 적용 예시는 `docs/conventions-docs.md`를 참조한다.

커밋 전 `npx markdownlint-cli2`(또는 `--fix`)로 로컬에서 검사한다.
