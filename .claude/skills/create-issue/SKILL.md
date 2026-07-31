---
name: create-issue
description: GitHub 이슈를 템플릿 기반으로 생성하고, 이슈 번호로 dev에서 작업 워크트리·브랜치를 만들어 진입한다. 백엔드·프론트엔드 공통.
argument-hint: "[type] 이슈 제목 — type: feat | fix | refactor | chore | docs | test"
allowed-tools: Bash, EnterWorktree
---

# create-issue

## 1. 인자 파싱

`$ARGUMENTS` 형식: `[type] 이슈 제목 및 설명`

- type이 명시된 경우: 첫 단어를 type으로, 나머지를 제목/설명으로 사용
- type이 없거나 인식 불가한 경우: 내용을 분석해 자동 결정

| type | 이슈 템플릿 | type 라벨 |
|------|-----------|------|
| feat | feature-template | `✨feat` |
| fix  | bug_report | `🐞bug` |
| refactor | feature-template | `🛠️refactor` |
| chore | feature-template | `⚙️chore` |
| docs | feature-template | `📝docs` |
| test | feature-template | `🧪 test` |

영역 라벨(`BE`/`FE`)은 type 라벨과 **별도**로 부여한다 — 3단계에서 함께 확인한다.

## 2. 이슈 템플릿 읽기

type에 맞는 템플릿 **하나만** 읽어 본문 골격으로 삼는다 — `fix`는 `bug_report.md`, 그 외는 `feature-template.md` (두 템플릿은 제목 줄만 다르고 섹션 구조는 동일하다).

템플릿은 **모노레포 루트** `.github/ISSUE_TEMPLATE/`에 있다(`backend/` 하위 아님). worktree는 자체 `.github/`를 가지므로 `git rev-parse --show-toplevel`로 루트를 앵커한다(하드코딩·`../` 금지):

```bash
cat "$(git rev-parse --show-toplevel)/.github/ISSUE_TEMPLATE/<bug_report|feature-template>.md"
```

## 3. 사용자 확인 (필수 — 이 단계를 건너뛰지 않는다)

`gh issue create`를 실행하기 전에 **반드시** 사용자에게 아래 두 항목을 질문한다.
`$ARGUMENTS`에서 충분히 추론 가능한 항목이라도 확인 또는 보완을 요청한다.

**질문 형식 (한 번에 같이 묻는다):**

> 이슈를 생성하기 전에 두 가지를 확인할게요.
>
> 1. **왜 지금 이걸 하는가?**
>    (비즈니스 이유, ADR 연관, 다른 기능의 사전 조건, 긴급도 등)
>    _현재 파악한 내용: {$ARGUMENTS에서 추론한 동기 또는 "명확하지 않음"}_
>
> 2. **완료를 어떻게 검증할 수 있나요? (성공 기준)**
>    (테스트 통과, 특정 동작 확인, 수치 목표 등 — 체크리스트 형태로 알려주세요)
>    _현재 파악한 내용: {$ARGUMENTS에서 추론한 성공 기준 또는 "명확하지 않음"}_
>
> 3. **백엔드 작업인가요, 프론트 작업인가요? (영역 라벨)**
>    (`BE` / `FE` / 둘 다 — 풀스택). 작업 설명에서 추론해 제안하되 확인받는다.
>    _현재 파악한 내용: {$ARGUMENTS에서 추론한 영역 또는 "명확하지 않음"}_

사용자 응답이 돌아온 뒤에만 다음 단계로 진행한다. 3번 답이 영역 라벨(`BE`/`FE`/둘 다)을 결정한다.

## 4. 템플릿 채우기

frontmatter(`---`로 감싼 부분)는 제거하고 본문 섹션만 사용한다.

| 섹션 | 작성 기준 |
|------|---------|
| `### 어떤 이슈인가요?` | `$ARGUMENTS`의 설명을 바탕으로 1~3문장 작성 |
| `### 🎯 왜 지금 이걸 하는가` | 3단계에서 사용자가 답한 내용으로 작성 |
| `### ✅ 성공 기준` | 3단계에서 사용자가 답한 내용으로 체크리스트 작성 |
| `### 연관 이슈` | 언급이 없으면 `없음` |
| `### 작업 마감일` / `### PR 마감일` | 언급이 없으면 `미정` |
| `### 🔧 TODO` | 예상 작업 항목을 체크리스트로 작성 |

## 5. 이슈 생성

라벨은 **type 라벨 1개 + 영역 라벨**(`BE`/`FE`, 풀스택이면 둘 다)을 함께 단다.

```bash
gh issue create \
  --title "[type] 제목" \
  --label "✨feat,BE" \
  --assignee "$(gh api user --jq '.login')" \
  --body "$(cat <<'EOF'
<채운 템플릿 본문>
EOF
)"
```

생성 후 출력된 URL에서 이슈 번호를 추출한다.

## 6. 워크트리 생성 및 진입

**현재 디렉터리에서 `git switch`로 브랜치를 갈아타지 않는다.** 같은 저장소를 보는 다른 세션의 작업을 덮어쓴다. 작업마다 워크트리를 새로 만들어 동시에 진행할 수 있게 한다([issue-workflow](../../rules/issue-workflow.md)).

통합 브랜치 `dev`를 **체크아웃하지 않고** `origin/dev`에서 직접 분기한다. `dev` 위에서 분기하면 autoSetupMerge가 새 브랜치 upstream을 `dev`로 잡아 이후 push·IDE Sync가 `dev`로 직행한다(#1404 사고 원인) — 금지. 영역(BE/FE)과 무관하게 브랜치 prefix는 붙이지 않는다.

```bash
MAIN="$(git worktree list --porcelain | sed -n '1s/^worktree //p')"   # 주 저장소 경로 (워크트리 안에서 실행해도 안전)
WT="$MAIN/.claude/worktrees/{type}-{issue-number}"
git fetch origin dev
git worktree add -b "{type}/{issue-number}-{slug}" "$WT" origin/dev
git -C "$WT" branch --unset-upstream 2>/dev/null || true   # ★ autoSetupMerge가 잡은 dev upstream 제거 (git-push-safety)
echo "$WT"
```

그 뒤 `EnterWorktree`에 **`path`로 위 경로를 넘겨** 세션을 옮긴다.

```text
EnterWorktree(path: "<WT 경로>")
```

- **`EnterWorktree`를 `name`으로 부르지 않는다.** 브랜치명이 `worktree-<name>`이 되어 `{type}/{N}-{slug}` 규약을 깨고, `create-pr`의 이슈 번호 추출(`close #N`)이 실패한다. 브랜치는 위 `git worktree add -b`로 만들고 `EnterWorktree`는 진입에만 쓴다.
- 디렉터리명은 `{type}-{issue-number}`(`/` 불가), 브랜치명은 `{type}/{issue-number}-{slug}`로 서로 다르다.
- `{slug}`: 이슈 제목을 소문자 + 하이픈으로 변환, 최대 40자
- 한국어 단어는 의미를 유지하는 영문으로 변환
- 이미 같은 경로의 워크트리가 있으면 `git worktree add`가 실패한다. 그때는 새로 만들지 말고 `EnterWorktree(path:)`로 기존 것에 들어간다.

## 7. 완료 출력

```text
✅ 이슈 생성: https://github.com/.../issues/{N}
🌿 브랜치:   {type}/{N}-{slug}
📁 워크트리: .claude/worktrees/{type}-{N}
```
