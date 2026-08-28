## 이슈 기반 작업 흐름

작업은 **정찰 → 이슈 → 워크트리·브랜치 → 설계 → PR → 리뷰 반영 → merge** 순으로 진행한다.

### 왜 이 규칙을 두는가

**1. git 작업을 순서대로 위임하기 위해.** 지금까지는 이슈를 만들고, 브랜치를 파고, PR을 열고, 리뷰를 반영하는 각 단계를 사람이 매번 따로 지시해야 했다. 어디까지 진행됐는지도 사람이 기억해야 했다. 순서와 판단 기준을 규칙으로 고정하면 "이 작업 해줘" 한 번으로 흐름 전체를 맡길 수 있다 — 사람은 확인 지점에서만 개입한다.

**2. 작업을 동시에 여러 개 돌리기 위해.** 로컬 브랜치 하나를 체크아웃해 쓰면 한 번에 한 작업만 가능하다. 다른 작업을 시작하려면 지금 것을 커밋하거나 치워야 하고, 세션이 둘이면 서로의 체크아웃을 갈아엎는다. **작업마다 워크트리를 분리하면 독립된 디렉터리에서 동시에 진행할 수 있다** — 이게 2단계에서 워크트리를 필수로 두는 이유다.

실행 절차는 스킬에 있다 — 이슈·브랜치는 [`/create-issue`](../skills/create-issue/SKILL.md), PR·리뷰 실행은 [`/create-pr`](../skills/create-pr/SKILL.md), 리뷰 로직은 [`deep-review`](../skills/deep-review/SKILL.md). 이 문서는 스킬이 다루지 않는 **판단 기준**만 정한다. 절차를 여기 옮겨 적지 않는다.

push·merge 안전은 [git-push-safety](git-push-safety.md)가 SSOT다.

### 경로 선택 — 잠정으로 시작하고 커밋 후 확정한다

모든 변경이 전체 흐름을 탈 필요는 없다. 다만 **시작 시점에는 기계로 판별할 수 없다** — 아직 고친 게 없어 diff가 비어 있고, `scope.sh`는 변경이 없으면 `NO_CHANGE=1`만 내고 끝난다. 그래서 두 번에 나눠 정한다.

#### 시작할 때 — 지시 내용으로 잠정 판정

| 지시 내용 | 잠정 |
| --- | --- |
| 오타·주석·포맷·문서 수정, 설정 한 줄 같은 **동작이 안 바뀌는 작업** | **경량(잠정)** — 이슈 없이 바로 워크트리·브랜치로 간다 |
| 그 외 | **전체** — 1단계부터 이슈를 만든다 |

애매하면 전체로 잠정한다. 판단이 갈리는 변경은 대개 전체가 필요한 변경이다.

#### 커밋한 뒤 — `scope.sh`로 확정

PR을 열기 직전에 확정한다(`/create-pr`이 확인한다). 판별은 `deep-review`의 [`scope.sh`](../skills/deep-review/scope.sh)가 한다. **여기서 따로 세지 않는다** — 판별을 두 벌 두면 정규식이 갈라져 같은 변경을 다르게 분류하게 된다.

```bash
bash "$(git rev-parse --show-toplevel)/.claude/skills/deep-review/scope.sh" dev
```

| 출력 | 확정 |
| --- | --- |
| `SRC_EMPTY=1` (문서·`.github/`·`docs/` 전용) | **경량** |
| `SRC_LINES` < 20 이고 `SRC_BINARY`가 없고 동작이 안 바뀜 | **경량** |
| 그 외 (`SRC_BINARY=1` 포함) | **전체** |

`SRC_LINES`는 **문서를 뺀 변경 줄 수**다. 전체 diff로 세면 규칙·문서를 길게 쓰면서 코드는 한 줄만 건드린 변경까지 이슈를 강제하게 된다. `SRC_BINARY=1`은 줄 수로 잴 수 없는 변경(이미지·폰트 등)이 섞였다는 뜻이라 크기를 알 수 없으므로 전체로 본다.

#### 잠정과 확정이 어긋나면

- **잠정 경량 → 확정 전체**: 그 시점에 이슈를 만든다. 워크트리·브랜치는 이미 있으므로 `/create-issue`를 이슈 생성까지만 쓰고, 브랜치명은 그대로 둔 채 PR 본문 `🔥 연관 이슈`에 `close #N`을 **직접 적는다**(브랜치명에 번호가 없어 자동 추출이 안 된다).
- **잠정 전체 → 확정 경량**: 그대로 둔다. 이미 만든 이슈는 지우지 않는다.

#### 경량 경로의 브랜치명·이슈 표기

브랜치명은 `{type}/no-issue-{slug}`. `{type}/` 바로 뒤가 숫자가 아니어야 `/create-pr`의 이슈 번호 추출이 이 브랜치를 건너뛴다 — `no-issue-`로 시작하므로 slug에 숫자가 들어가도 안전하다(추출 규칙은 [create-pr](../skills/create-pr/SKILL.md)).

PR 템플릿의 `🔥 연관 이슈`에는 `없음`이라 적고 사유를 괄호로 덧붙인다(예: `없음 (경량 경로)`). 라벨은 그대로 단다.

### 0. 정찰 — `origin/dev` 기준으로 읽는다

이슈의 완료 조건을 쓰려면 코드를 먼저 봐야 한다. 이때 **로컬 체크아웃 파일을 읽지 않는다** — 로컬은 `origin/dev`보다 뒤처져 있을 수 있고, 낡은 파일을 근거로 "그 클래스는 없다"고 판단하면 이슈부터 틀린다.

```bash
git fetch origin dev
git log --oneline origin/dev -5   # 실제 최신 상태

# 파일은 이렇게 읽는다 (backend는 멀티모듈 — 모듈명이 경로에 들어간다)
git show origin/dev:backend/game/src/main/java/coffeeshout/…/Foo.java
git show origin/dev:frontend/src/apis/rest/api.ts
```

읽기만 한다. 변경은 브랜치가 생긴 뒤(2단계)부터.

### 1. 이슈 — 완료 조건까지만 쓴다

`/create-issue`가 템플릿·라벨·사용자 확인을 처리한다.

**설계 방향은 이 단계에서 쓰지 않는다.** 코드를 제대로 보기 전에 확정한 설계는 어차피 다시 쓴다. 이슈에는 "무엇이 되면 끝인가"(성공 기준)만 남기고, "어떻게 할 것인가"는 3단계에서 채운다.

### 2. 워크트리·브랜치 — 현재 디렉터리에서 브랜치를 갈아타지 않는다

**작업마다 워크트리를 새로 만들고 그 안에서 브랜치를 판다.** 현재 디렉터리에서 `git switch`로 갈아타면 같은 저장소를 보는 다른 세션의 작업을 덮어쓴다. `/create-issue` 6단계가 처리한다.

```bash
MAIN="$(git worktree list --porcelain | sed -n '1s/^worktree //p')"   # 주 저장소 경로
git fetch origin dev
git worktree add -b "{type}/{N}-{slug}" "$MAIN/.claude/worktrees/{type}-{N}" origin/dev
git -C "$MAIN/.claude/worktrees/{type}-{N}" branch --unset-upstream 2>/dev/null || true

# env 심볼릭 링크 + 워크트리 전용 포트 (#1660)
bash "$MAIN/.claude/skills/create-issue/worktree-setup.sh" "$MAIN/.claude/worktrees/{type}-{N}"
```

그 뒤 `EnterWorktree`에 `path`로 그 경로를 넘겨 세션을 옮긴다.

- **디렉터리명(`{type}-{N}`)과 브랜치명(`{type}/{N}-{slug}`)은 다르다.** 디렉터리에는 `/`를 쓸 수 없고, 브랜치명은 `create-pr`이 `{N}`을 뽑아 `close #N`을 채우는 근거라 규약을 지켜야 한다.
- **`EnterWorktree`를 `name`으로 부르지 않는다.** 그러면 브랜치명이 `worktree-<name>`이 되어 규약을 깨고 이슈 번호 추출이 실패한다. 브랜치는 위처럼 `git worktree add -b`로 만들고, `EnterWorktree`는 `path`로 들어가기만 한다.
- `.claude/worktrees/`는 `.gitignore` 대상이다 — 워크트리가 저장소를 더럽히지 않는다.
- **`.env` 계열은 `.gitignore` 대상이라 `git worktree add`가 가져오지 않는다.** 위 `worktree-setup.sh`가 주 저장소에서 심볼릭 링크하고, 워크트리 전용 포트를 `.ports`에 잡는다. 이걸 건너뛰면 백엔드는 OAuth 없이 뜨고 프론트는 `API_URL`이 `undefined`인 채로 조용히 백엔드에 못 붙는다(#1660). 실행은 [`run-local`](../skills/run-local/SKILL.md).
- 워크트리는 작업이 끝나도 자동으로 지우지 않는다. PR이 merge된 뒤 정리한다.

  ```bash
  git worktree remove "$MAIN/.claude/worktrees/{type}-{N}"
  git branch -D "{type}/{N}-{slug}"    # -d 가 아니라 -D
  ```

  **`-d`는 항상 거부된다.** PR을 squash로 합치므로(git-push-safety) 작업 브랜치 커밋은 `dev`에서 reachable하지 않고, git은 "머지 안 된 브랜치"로 본다. `-d`를 쓰면 워크트리만 지워지고 브랜치가 남아, 같은 이슈로 다시 작업할 때 브랜치명 충돌로 워크트리 생성이 실패한다. **PR이 merge된 것을 확인한 뒤에만** `-D`를 쓴다.

### 3. 설계 — 브랜치 파일을 직접 보고 이슈에 채운다

브랜치가 생겼으면 그 안의 파일을 직접 확인한다. 0단계 정찰은 개요를 잡는 용도고, 실제 구조는 여기서 본다.

정리한 접근 방식을 이슈 본문 `### 🔧 TODO` 위에 `### 🧭 설계 방향`으로 덧붙인다.

```bash
gh issue view {N} --json body -q .body > /tmp/issue.md   # 기존 본문 보존
# 편집 후
gh issue edit {N} --body-file /tmp/issue.md
```

변경이 크면 Plan 모드로 먼저 설계한다. `fix` 타입은 수정 코드보다 **먼저** 회귀 테스트를 작성해 버그를 실패로 재현한 뒤 고친다(테스트 위치는 `backend/.claude/rules/testing.md`).

### 4. PR

`/create-pr`이 push 가드·템플릿·라벨·`close #N`을 처리하고, 생성 직후 `deep-review`를 돌려 발견사항을 PR 코멘트로 게시한다.

PR을 만들기 전에 **연결된 이슈를 다시 읽어 성공 기준이 충족됐는지 확인한다.** 어긋나면 5단계 표의 기본값에 따른다.

**base가 `dev`·`prod`가 아니면 CI가 돌지 않는다** — 워크플로가 `pull_request: branches: [dev, prod]`로 제한돼 있다. 통합 브랜치를 두고 단계별 PR을 쌓았다면 테스트 게이트는 통합→dev PR에서 **처음** 걸린다. 그 PR을 열기 전에 `./gradlew spotlessCheck pmdMain pmdTest pmdTestFixtures`와 테스트를 로컬에서 먼저 돌린다.

### 5. 리뷰 반영 — 지적마다 답한다

리뷰가 달렸는데 커밋만 올리고 끝내지 않는다. 리뷰어는 답이 없으면 무엇이 반영됐는지 알 수 없고, PR 본문 수정은 알림이 가지 않아 갈음이 안 된다.

답은 **반영 여부를 맨 앞에 밝히고**(`✅ 반영` / `❌ 반려`) 그 뒤에 내용을 적는다 — 반영했으면 무엇을 어떻게 고쳤는지, 반려했으면 이유를.

**반려 이유는 추측이 아니라 근거로 적는다.** lint·타입 규칙이면 실제로 돌려본 출력을, 코드 동작이면 확인한 `파일:줄`을 든다. "문제 없어 보임"은 근거가 아니다.

어디에 다는지는 리뷰 형태에 따라 다르다.

| 리뷰 형태 | 답하는 곳 |
| --- | --- |
| 사람이 코드 줄에 단 리뷰 코멘트 | **그 스레드에 하나씩** 답글. 요약 코멘트 하나로 몰지 않는다 — 지적과 답이 떨어져 있으면 리뷰어가 눈으로 짝지어야 한다 |
| `deep-review`가 게시한 코멘트 1개 | 그 코멘트에 답글 하나. 단 **항목별로 줄을 나눠** `✅/❌ + 근거`를 적는다 |

줄 단위 스레드에 답글을 다는 절차:

```bash
PR=1234
REPO="$(gh repo view --json nameWithOwner -q .nameWithOwner)"

# 아직 답하지 않은 스레드의 최상위 코멘트 id 목록.
#  --paginate: 없으면 30개까지만 와서 그 뒤 지적이 통째로 안 보인다(답을 다 했다고 착각하게 된다).
#  isResolved 는 REST 에 없다 — GraphQL reviewThreads 로만 알 수 있다.
gh api graphql --paginate -F pr="$PR" -F owner="${REPO%/*}" -F name="${REPO#*/}" -f query='
query($owner:String!,$name:String!,$pr:Int!,$endCursor:String){
  repository(owner:$owner,name:$name){ pullRequest(number:$pr){
    reviewThreads(first:100,after:$endCursor){
      pageInfo{ hasNextPage endCursor }
      nodes{ isResolved comments(first:1){ nodes{ databaseId path line body } } } } } } }' \
  --jq '.data.repository.pullRequest.reviewThreads.nodes[]
        | select(.isResolved | not) | .comments.nodes[0] | "\(.databaseId)\t\(.path):\(.line)"'

# 해당 스레드에 답글 (databaseId 를 그대로 쓴다)
gh api "repos/$REPO/pulls/$PR/comments/$COMMENT_ID/replies" -f body='✅ 반영 — …'
```

`pulls/$PR/comments`를 그냥 부르면 **이미 해결된 스레드와 과거 답글까지 섞여** 나온다. 그대로 답글을 달면 끝난 대화에 중복 답글이 붙어 알림만 시끄러워진다. 위 GraphQL은 미해결 스레드의 **첫 코멘트만** 뽑는다.

여러 지적을 관통하는 설명이 필요할 때만 `gh pr comment $PR`로 전체 코멘트를 **덧붙인다**(답글을 대신하지 않는다).

### 6. merge — Claude는 하지 않는다

`dev`·`prod`는 보호 브랜치다. **Claude는 `gh pr merge`를 실행하지 않는다** — 리뷰 반영이 끝났다는 자체 판단만으로 진행하지 않고, 사용자가 명시적으로 지시했을 때만 실행한다. 상세는 [git-push-safety](git-push-safety.md#merge-권한).

### 사용자 확인과 부재 시 기본값

`/create-issue`·`/create-pr`은 실제 생성 전에 사용자 확인 단계를 거친다. 다만 **응답을 기다리다 작업 전체가 멈추면 안 된다.** 응답이 없으면 아래 기본값으로 진행하고, 가정한 내용을 눈에 보이는 곳에 남긴다.

| 확인 지점 | 응답이 없을 때 |
| --- | --- |
| 이슈 성공 기준·영역 라벨 (1단계) | 가정한 내용을 이슈 본문에 `> 가정: …` 인용으로 명시하고 생성. 영역 라벨은 변경 경로로 판별 |
| 첫 push (4단계) | 자기 이름 브랜치로의 명시 refspec push는 그대로 진행 |
| PR 오픈 (4단계) | `--draft`로 연다 |
| 이슈 성공 기준과 어긋남 (4단계) | draft 유지 + PR 본문 `💬 리뷰 중점사항`에 어긋난 항목을 목록으로 명시 |
| **merge (6단계)** | **진행하지 않는다.** 명시 지시 없이는 어떤 경우에도 merge하지 않는다 |

멈춰서 물어야 하는 건 merge 하나뿐이다. 나머지는 가정을 밝히고 진행하는 쪽이 낫다 — 가정이 틀렸으면 draft PR에서 고치면 되지만, 멈춰 있으면 고칠 대상 자체가 없다.
