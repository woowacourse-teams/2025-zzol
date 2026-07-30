# CLAUDE.md

**쫄(ZZOL)** — 미니게임 기반 당첨자 추첨 서비스(<https://zzol.site>)의 모노레포다.

| 폴더 | 스택 | 가이드 |
| --- | --- | --- |
| `backend/` | Spring, Gradle 멀티모듈 | 백엔드 작업 시 `backend/CLAUDE.md` 자동 로드 |
| `frontend/` | React, Webpack | 프론트 작업 시 `frontend/CLAUDE.md` 자동 로드 |

## 브랜치 전략 (단일 `dev`)

- **모든 작업(백엔드·프론트·풀스택)은 통합 브랜치 `dev`에서 분기해 `dev`로 PR한다.**
- 브랜치명은 prefix 없이 `{type}/{N}-{slug}` (예: `feat/1502-nunchi-game`). type: `feat`·`fix`·`refactor`·`chore`·`docs`·`test`.
- 영역(BE/FE)은 브랜치가 아니라 **라벨**로 구분한다 — `create-issue`/`create-pr`가 변경 영역을 판별해 `BE`/`FE`(풀스택이면 둘 다) 라벨을 단다.
- 프로덕션 승격은 통합 `prod` 브랜치로의 `dev`→`prod` PR로만 한다. `prod` push가 곧 운영 배포다. 상세는 [git-push-safety](.claude/rules/git-push-safety.md).

## 공통 스킬 (전역)

루트 `.claude/skills/`에 있어 어느 폴더에서 작업하든 사용 가능하다.

| 스킬 | 용도 |
| --- | --- |
| `create-issue` | 이슈 템플릿 기반 생성 + `dev`에서 작업 브랜치 분기 (영역 라벨 확인) |
| `create-pr` | PR 템플릿 기반 생성 (base `dev`, 변경 경로로 영역 라벨 판별) |
| `deep-review` | 리뷰 렌즈 5~6개 병렬 실행 → 신뢰도 채점(80점 컷) → 리포트 1개로 병합. CodeRabbit 대체(#1600), `create-pr`이 자동 호출 |
| `adr` | Architecture Decision Record 작성 (`NNNN`+index, 영역별 `docs/adr/`) |

도메인 전용 스킬·에이전트·규칙은 각 폴더 `.claude/`에 있으며 그 폴더 작업 시 자동 로드된다(백엔드 `impl`·`write-tests`·`commit`·`code-reviewer` 등, 프론트 `frontend-design`·`ws-contract`·`fe-code-reviewer` 등).

`deep-review`의 과설계 렌즈가 쓰는 **ponytail 플러그인은 `.claude/settings.json`에서 프로젝트 전역으로 활성**이다(팀 합의). 첫 세션에 설치·신뢰 프롬프트가 뜬다.

## 보편 작업 원칙

- 요구사항이 모호하거나 해석이 여럿이면 **구현 전에 질문**한다. 추측으로 진행하지 않는다.
- **외과적 변경**: 요청 범위 밖 코드·주석·포맷은 건드리지 않는다.
- 20줄 이상 대량 출력이 예상되는 탐색·분석은 서브에이전트에 위임한다.

## git push 안전

보호 브랜치(`dev`·`main` 등)에는 **직접 push·commit하지 않는다.** 작업 브랜치 upstream을 보호 브랜치로 두지 않고(`git branch --unset-upstream`), push는 명시 refspec(`git push -u origin HEAD:{type}/{N}-{slug}`)으로 한다. 전문은 [.claude/rules/git-push-safety.md](.claude/rules/git-push-safety.md).
