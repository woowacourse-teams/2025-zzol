---
description: docs/ 디렉토리 Markdown 파일 작성 규칙 (markdownlint MD040/MD031/MD022)                                                                                                                    
paths:                                                                                                                                                                              
- "docs/**/*.md"
---

# 전체 컨벤션: `docs/conventions-docs.md`

## MD040 — 코드 블록에 언어 지정 필수

모든 펜스 코드 블록(` ``` `)에 언어를 명시한다. 언어를 특정할 수 없으면 `text`를 사용한다.

자주 쓰는 식별자: `java`, `sql`, `yaml`, `bash`, `json`, `text`

## MD031 — 코드 블록 앞뒤에 빈 줄 필수

펜스 코드 블록 바로 앞과 바로 뒤에 빈 줄이 있어야 한다.

## MD022 — 헤딩 앞뒤에 빈 줄 필수

`#`, `##`, `###` 헤딩 바로 앞과 바로 뒤에 빈 줄을 넣는다. 문서 첫 줄 헤딩의 앞 빈 줄은 생략 가능하다.
