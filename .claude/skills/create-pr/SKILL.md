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

**독자는 이 저장소를 처음 보는 1년차 팀원이다.** 그 사람이 위에서부터 읽어 내려가며 "무엇이 되는 변경인가 → 어떤 흐름으로 도는가 → 어디부터 열어봐야 하는가"를 알 수 있으면 잘 쓴 PR이다.

### 밀도를 낮춘다 — 짧게 쓰는 것과 다르다

읽기 어려운 PR은 길어서가 아니라 **한 줄에 사실을 서너 개씩 밀어넣어서** 그렇다. 줄 수를 줄이려고 괄호·볼드·화살표로 압축하면 정보는 다 있는데 아무도 못 읽는다. 사실을 빼서 줄이지 말고, **줄을 늘려서 푼다.**

- **한 문장에 사실 하나.** 부연은 괄호가 아니라 다음 문장으로 뺀다.
- **볼드는 한 문단에 하나까지.** 다 굵으면 강조가 사라진다.
- **약어·기호·영어 용어는 처음 나올 때 한 번 푼다.** 예: `transient 전송` → "재접속 복구용으로 저장하지 않고 그때만 보내는 방식".
- **수식·상수·측정값의 근거는 본문에 늘어놓지 않는다.** 결론 한 줄만 적고 상세는 `<details>` 접기나 설계 문서 링크로 민다.
- 클래스·필드 이름을 그대로 쓸 땐 **그게 무슨 역할인지 한 조각 붙인다.** 이름만으로는 처음 보는 사람이 못 읽는다.

### 흐름은 글이 아니라 그림으로 (mermaid)

GitHub은 `mermaid` 코드블록을 그림으로 그려준다. 이미지 업로드도, 외부 도구도 필요 없다.

**요청 하나가 여러 컴포넌트를 거치거나 상태가 순서대로 바뀌면 그림을 하나 넣는다.** 클래스 이름을 나열해 흐름을 설명하려 들지 않는다.

| 무엇을 보여주나 | 쓸 것 |
| --- | --- |
| 요청이 컴포넌트를 타고 흐른다 | `sequenceDiagram` |
| 상태가 순서대로 바뀐다 | `stateDiagram-v2` |
| 그 외 관계·구조 | `flowchart LR` |

- 노드는 **5~8개**로 줄인다. 이번 PR에서 바뀐 지점만 그리고 주변은 한 덩어리로 묶거나 생략한다.
- 그림이 diff보다 복잡해지면 안 그리는 게 낫다.
- 오타·상수 하나·의존성 bump처럼 한 줄로 끝나는 변경에는 넣지 않는다.

### 내보내기 전 자체 점검

세 가지를 확인하고, 하나라도 아니면 고쳐 쓴다.

1. 맨 위 3줄만 읽고 **무엇이 되는 변경인지** 알 수 있나?
2. 처음 보는 팀원이 **어느 파일부터 열면 되는지** 알 수 있나?
3. 한 번에 이해가 안 돼 **되읽어야 하는 문장**이 있나?

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
- 🚀 작업 내용: 아래 세 덩어리를 이 순서로 적는다.
  1. **한 줄 요약** — 이 PR이 머지되면 무엇이 되는가. 클래스명 없이, 쓰는 사람 입장의 말로 적는다.
  2. **흐름 그림** — mermaid 하나(위 작성 원칙 참조). 흐름이 안 바뀌는 변경이면 생략한다.
  3. **변경 목록** — 번호 목록. 항목마다 *무엇을 바꿨는지* 먼저, *왜 그랬는지* 다음. 파일·클래스명은 그 뒤에 붙인다.
- 💬 리뷰 중점사항: 리뷰어가 **판단해주길 바라는 것**만 적는다. 내린 결정과 그 이유, 확신이 덜 선 트레이드오프, 놓치기 쉬운 부분. 이미 확실한 사실은 여기 적지 않는다 — 그건 작업 내용에 있다.

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
