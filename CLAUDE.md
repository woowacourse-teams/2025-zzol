# CLAUDE.md

**쫄(ZZOL)** — 미니게임 기반 당첨자 추첨 서비스(<https://zzol.site>)의 모노레포다. `backend/`·`frontend/` 2개 영역.

## 브랜치 전략 (단일 `dev`)

- **모든 작업(백엔드·프론트·풀스택)은 통합 브랜치 `dev`에서 분기해 `dev`로 PR한다.**
- 브랜치명은 prefix 없이 `{type}/{N}-{slug}` (예: `feat/1502-nunchi-game`). type: `feat`·`fix`·`refactor`·`chore`·`docs`·`test`.
- 영역(BE/FE)은 브랜치가 아니라 **라벨**로 구분한다 — `create-issue`/`create-pr`가 변경 영역을 판별해 `BE`/`FE`(풀스택이면 둘 다) 라벨을 단다.
- 프로덕션 승격은 통합 `prod` 브랜치로의 `dev`→`prod` PR로만 한다. `prod` push가 곧 운영 배포다. 상세는 [git-push-safety](.claude/rules/git-push-safety.md).

## 스킬·에이전트·규칙

루트 `.claude/skills/`의 공통 스킬은 어느 폴더에서 작업하든 사용 가능하다. 도메인 전용 스킬·에이전트·규칙은 각 폴더 `.claude/`에 있으며 그 폴더 작업 시 자동 로드된다.

`deep-review`의 과설계 렌즈가 쓰는 **ponytail 플러그인은 `.claude/settings.json`에서 프로젝트 전역으로 활성**이다(팀 합의). 첫 세션에 설치·신뢰 프롬프트가 뜬다.

## 보편 작업 원칙

- 요구사항이 모호하거나 해석이 여럿이면 **구현 전에 질문**한다. 추측으로 진행하지 않는다.
- **외과적 변경**: 요청 범위 밖 코드·주석·포맷은 건드리지 않는다.
- 20줄 이상 대량 출력이 예상되는 탐색·분석은 서브에이전트에 위임한다.

## git push 안전

보호 브랜치(`dev`·`prod`·`main`·`master`)에는 **직접 push·commit하지 않는다.** 작업 브랜치 upstream을 보호 브랜치로 두지 않고(`git branch --unset-upstream`), push는 명시 refspec(`git push -u origin HEAD:{type}/{N}-{slug}`)으로 한다. 전문은 [.claude/rules/git-push-safety.md](.claude/rules/git-push-safety.md).
