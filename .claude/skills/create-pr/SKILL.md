---
name: create-pr
description: PR 템플릿을 읽어 GitHub Pull Request를 생성한다. 백엔드·프론트엔드 공통.
argument-hint: "[PR 제목 (선택)] [--base=브랜치명 (기본: dev)]"
allowed-tools: Read, Bash, Glob, Agent
---

# create-pr

## 사전 작업

1. base 브랜치를 정한다. `$ARGUMENTS`에 `--base=<브랜치>`가 있으면 그 값, 없으면 통합 브랜치 `dev`. 이후 이 값을 `$BASE`로 쓴다.
2. PR 템플릿은 모노레포 루트에 있다. `REPO_ROOT="$(git rev-parse --show-toplevel)"` 로 루트를 구해 `${REPO_ROOT}/.github/pull_request_template.md`를 Read한다.
3. `git log "origin/$BASE"..HEAD --oneline` 와 `git diff "origin/$BASE"...HEAD --stat` 으로 이번 브랜치의 커밋·변경 파일을 확인한다 (로컬 `$BASE`는 stale일 수 있으니 `origin/` 기준).
4. **브랜치를 원격에 올린다 (`gh pr create`의 전제).** 먼저 아래 보호 브랜치·detached HEAD 가드를 실행한다(`ABORT` 출력 시 중단·보고). 보호 목록 SSOT는 `.claude/rules/git-push-safety.md`. 통과하면 **자기 이름 명시 refspec**으로 push한다 (bare `git push` 금지). 이미 올라가 있으면 push는 생략한다.

   ```bash
   PROTECTED="dev prod main master"
   branch="$(git symbolic-ref --short -q HEAD || true)"
   [ -z "$branch" ] && { echo "ABORT: detached HEAD — 작업 브랜치로 전환하세요."; exit 1; }
   case " $PROTECTED " in *" $branch "*) echo "ABORT: 보호 브랜치 '$branch' 직접 push 금지 (git-push-safety). PR로 반영하세요."; exit 1;; esac
   git push -u origin "HEAD:$branch"
   ```

## PR 제목

- 형식: `[type] 한국어 설명` (예: `[fix] 카드 점수 집계 누락 수정`). type: `feat`·`fix`·`refactor`·`chore`·`docs`·`test`
- `$ARGUMENTS`에 제목이 있으면 그대로, 없으면 커밋 내용으로 자동 생성
- type별 제목·본문 예시는 [examples.md](examples.md) 참조

## 라벨 & Assignee

- **type 라벨** (1개): feat `✨feat` / fix `🐞bug` / refactor `🛠️refactor` / chore `⚙️chore` / docs `📝docs` / test `🧪 test`
- **영역 라벨**: 변경 경로로 판별한다. `git diff --name-only "origin/$BASE"...HEAD` 결과가 `backend/` 만이면 `BE`, `frontend/` 만이면 `FE`, 양쪽이 섞였으면 `BE`+`FE`(풀스택). 루트 설정 등 어느 쪽도 아니면 변경 성격으로 판단해 사용자에게 확인한다.
- 우선순위(`p-*`)는 `$ARGUMENTS`에 있을 때만 추가
- Assignee: `gh api user --jq '.login'` 결과로 자동 지정

## 작성 원칙 (본문 공통)

리뷰어가 빠르게 이해하는 것을 최우선으로, 쉬운 용어와 간결한 문장으로 작성한다.

- **쉬운 용어**: 전문 용어·영어 약어(point of use, redundant, hermetic, drift 등)를 피하고 풀어서 쓴다. 꼭 필요한 기술 용어는 한 줄 풀이를 붙인다.
- **간결함**: 한 항목은 1~2문장. "무엇을 왜 바꿨는지"를 먼저 적는다.
- **결정·트레이드오프는 이유와 함께**: 리뷰어가 되묻지 않아도 알 수 있게 적는다.

## 템플릿 작성

`.github/pull_request_template.md` 섹션을 유지하고 채운다.

- ✅ 체크리스트: `--base` 확인 후 `[x]`
- 🔥 연관 이슈: 현재 브랜치명 `<type>/<N>-<slug>`(create-issue가 만든 형식)에서 이슈 번호 `N`을 추출해 `close #N`. 브랜치명에 번호가 없으면 `없음`
- 🚀 작업 내용: 변경 파일·커밋을 번호 목록으로
- 💬 리뷰 중점사항: 설계 결정·트레이드오프·주의 사항

## 실행

```bash
BASE="dev"   # 사전 작업 1의 값 (--base 로 오버라이드 가능)
gh pr create \
  --title "[fix] 카드 점수 집계 누락 수정" \
  --base "$BASE" \
  --label "🐞bug,BE" \
  --assignee "$(gh api user --jq '.login')" \
  --body-file - <<'EOF'
<템플릿 채운 내용>
EOF
```

완료 후 PR 본문이 반영됐는지 확인하고 URL을 `$PR_URL`로 둔다.

## 코드 리뷰 (PR 생성 후, 필수)

CodeRabbit 자동 리뷰를 대체하는 단계다(#1600). PR이 이미 존재하므로 여기서 리뷰를 돌려 발견사항을 PR 코멘트로 게시한다.

1. **리뷰어 선택 (영역 → 에이전트 매핑)**: 영역 라벨과 동일한 판별(`git diff --name-only "origin/$BASE"...HEAD`)로 고른다. **에이전트는 스택 전용이다** — 백엔드는 `code-reviewer`, 프론트엔드는 `fe-code-reviewer`로 반드시 구분해 호출한다.

   | 변경 경로 | 리뷰어 에이전트 (`subagent_type`) | 리뷰 대상 경로 |
   | --- | --- | --- |
   | `backend/` 포함 | **`code-reviewer`** (백엔드) | `backend/**/src/main/java` |
   | `frontend/` 포함 | **`fe-code-reviewer`** (프론트엔드) | `frontend/src` |
   | 양쪽(풀스택) | **둘 다** 각각 호출 | 각 스택 경로 |
   | 루트 설정 등 소스 변경 없음 | 건너뜀 | — |

2. **동기 실행**: 결과 텍스트를 받아 코멘트로 올려야 하므로 `run_in_background: false`로 호출한다. 프롬프트에 **리뷰 대상 diff 범위를 `origin/$BASE...HEAD`로 명시**한다 — 리뷰어 에이전트 기본값이 마지막 커밋(`git diff HEAD~1`)이라 PR 전체 범위와 어긋나므로 반드시 범위를 넘긴다. 스택별로 `subagent_type`과 대상 경로가 다르다.

   ```text
   # 백엔드 변경
   Agent(subagent_type: "code-reviewer", run_in_background: false,
         prompt: "이 PR(브랜치 origin/$BASE...HEAD 전체 diff)의 backend src/main/java 변경을 리뷰하라. 대상 파일은 `git diff --name-only origin/$BASE...HEAD`로 확정하라. 발견사항만 텍스트로 반환한다.")

   # 프론트엔드 변경
   Agent(subagent_type: "fe-code-reviewer", run_in_background: false,
         prompt: "이 PR(브랜치 origin/$BASE...HEAD 전체 diff)의 frontend/src 변경을 리뷰하라. 대상 파일은 `git diff --name-only origin/$BASE...HEAD`로 확정하라. 발견사항만 텍스트로 반환한다.")
   ```

3. **코멘트 정돈·게시**: 에이전트 리포트를 스캔 가능한 코멘트 하나로 정돈한다 — 상단에 심각도별 요약, 그 아래 파일별 발견사항, 긴 개선 제안은 `<details>`로 접는다. **두 리뷰어를 돌린 경우(풀스택) `### 백엔드`·`### 프론트엔드` 섹션으로 나눠 한 코멘트에 합친다**(코멘트 난립 방지). 발견사항이 없으면 "리뷰 통과 — 지적사항 없음"으로 적는다. 리뷰어는 제안만 하고 코드는 수정하지 않으므로 코드 변경은 없다. 정돈한 내용을 PR에 코멘트 1개로 올린다.

   ```bash
   gh pr comment "$PR_URL" --body-file - <<'EOF'
   ## 🤖 클로드 코드 리뷰
   <정돈한 발견사항>
   EOF
   ```

완료 후 PR URL과 리뷰 코멘트 게시 여부를 출력한다.
