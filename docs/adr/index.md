# ADR 인덱스 (전역)

모노레포 전역·크로스커팅 의사결정 목록. FE/BE 국한 결정은 `frontend/docs/adr/`·`backend/docs/adr/` 를 본다.

> 이 파일은 **생성물**이다. 직접 편집하지 말고 `node docs/adr/gen-index.mjs` 로 갱신한다.
> 내용 검색은 qmd(`qmd query "..." --collection zzol-docs`)를 1차 경로로, 이 표는 상태·카탈로그 조회에 쓴다.

| 번호 | 제목 | 상태 | 영향 범위 | 핵심 제약 |
| --- | --- | --- | --- | --- |
| [#1540](adr-1540-dev-prod-single-branch.md) | dev/prod 단일 브랜치 체계 전환 | 제안 | 브랜치 전략, .github/workflows(CD·CI), git-push-safety | 모든 작업은 dev에서 분기·PR. prod 배포는 단일 통합 prod 경로로 일원화하고 be/*·fe/* 레거시 브랜치는 폐기(전환 완료 후) |
