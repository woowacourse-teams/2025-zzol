---
name: create-pr
description: PR 템플릿을 읽어 GitHub Pull Request를 생성한다. 백엔드·프론트엔드 공통.
argument-hint: "[PR 제목 (선택)] [--base=브랜치명 (기본: dev)]"
allowed-tools: Read, Bash, Glob, Agent, Skill
---

# create-pr

## 사전 작업

1. base 브랜치를 정한다. `$ARGUMENTS`에 `--base=<브랜치>`가 있으면 그 값, 없으면 통합 브랜치 `dev`. 이후 이 값을 `$BASE`로 쓴다.
2. PR 템플릿은 모노레포 루트에 있다. `REPO_ROOT="$(git rev-parse --show-toplevel)"` 로 루트를 구해 `${REPO_ROOT}/.github/pull_request_template.md`를 Read한다.
3. `git log "origin/$BASE"..HEAD --oneline` 와 `git diff "origin/$BASE"...HEAD --stat` 으로 이번 브랜치의 커밋·변경 파일을 확인한다 (로컬 `$BASE`는 stale일 수 있으니 `origin/` 기준).
3-1. **경로 확정** — 커밋이 다 올라온 지금이 [issue-workflow](../../rules/issue-workflow.md)의 경로를 확정할 시점이다. `scope.sh` 출력으로 판정한다.

   ```bash
   bash "$(git rev-parse --show-toplevel)/.claude/skills/deep-review/scope.sh" "$BASE"
   ```

   `SRC_EMPTY=1`이거나 (`SRC_LINES` < 20 이고 `SRC_BINARY`가 없고 동작이 안 바뀜)이면 경량, 아니면 전체다. **이슈 없이 시작했는데 전체로 확정되면 지금 이슈를 만들고** `close #N`을 본문에 직접 적는다. 이미 이슈가 있으면 그대로 진행한다.
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
- 🔥 연관 이슈: 현재 브랜치명 `<type>/<N>-<slug>`(create-issue가 만든 형식)에서 이슈 번호 `N`을 추출해 `close #N`.

  **추출은 반드시 브랜치명 맨 앞에 앵커한다.** 아무 숫자나 주우면 `chore/no-issue-1587-dependabot-...` 같은 경량 경로 브랜치에서 `1587`을 뽑아 **무관한 이슈를 자동으로 닫는다**.

  ```bash
  N="$(git branch --show-current | sed -n 's|^[a-z][a-z]*/\([0-9][0-9]*\)-.*|\1|p')"
  [ -n "$N" ] && echo "close #$N" || echo "없음"
  ```

  번호가 없으면 `없음`이라 적고 사유를 괄호로 덧붙인다(예: `없음 (경량 경로)`). 작업 도중 경량에서 전체로 바뀌어 이슈를 뒤늦게 만든 경우([issue-workflow](../../rules/issue-workflow.md)의 경로 선택)는 브랜치명에 번호가 없으므로 `close #N`을 **직접 적는다**.
- 🚀 작업 내용: 변경 파일·커밋을 번호 목록으로
- 💬 리뷰 중점사항: 설계 결정·트레이드오프·주의 사항

## 실행

`gh pr create`는 생성된 PR URL을 stdout으로 출력하므로 그대로 캡처한다.

```bash
BASE="dev"   # 사전 작업 1의 값 (--base 로 오버라이드 가능)
PR_URL="$(gh pr create \
  --title "[fix] 카드 점수 집계 누락 수정" \
  --base "$BASE" \
  --label "🐞bug,BE" \
  --assignee "$(gh api user --jq '.login')" \
  --body-file - <<'EOF'
<템플릿 채운 내용>
EOF
)"
[ -z "$PR_URL" ] && { echo "ABORT: PR 생성/URL 조회 실패"; exit 1; }
echo "$PR_URL"
```

Bash 툴은 호출마다 새 셸이라 `$PR_URL`은 블록 간 유지되지 않는다. 아래 코드 리뷰 단계의 코멘트 블록은 URL을 **다시 조회**한다.

## 코드 리뷰 (PR 생성 후, 필수)

CodeRabbit 자동 리뷰를 대체하는 단계다(#1600). PR이 이미 존재하므로 여기서 리뷰를 돌려 발견사항을 PR 코멘트로 게시한다.

리뷰 로직은 `deep-review` 스킬에 있다. 그대로 호출한다 — 렌즈 선택·병렬 실행·채점·코멘트 게시를 스킬이 처리한다.

```text
Skill("deep-review", "--base=$BASE --comment")
```

완료 후 PR URL과 리뷰 코멘트 게시 여부를 출력한다.
