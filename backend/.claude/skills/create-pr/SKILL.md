---
name: create-pr
description: PR 템플릿을 읽어 GitHub Pull Request를 생성한다.
argument-hint: "[PR 제목 (선택)] [--base=브랜치명 (기본: be/dev)]"
allowed-tools: Read, Bash, Glob
---

# create-pr

## 사전 작업

1. base 브랜치를 정한다. `$ARGUMENTS`에 `--base=<브랜치>`가 있으면 그 값을, 없으면 `be/dev`를 쓴다. 이후 단계는 이 값(`$BASE`)을 기준으로 한다.
2. `.github/pull_request_template.md`는 **모노레포 루트**에 있다. `backend/` 하위가 아니므로 `REPO_ROOT="$(git rev-parse --show-toplevel)"` 로 루트를 구한 뒤 `${REPO_ROOT}/.github/pull_request_template.md` 경로로 Read한다.
3. `git log "$BASE"..HEAD --oneline`으로 이번 브랜치의 커밋 목록을 확인한다.
4. `git diff "$BASE"...HEAD --stat`으로 변경된 파일 목록을 확인한다.

## PR 제목 규칙

- 형식: `[type] 한국어 설명` (예: `[fix] 카드 점수 집계 누락 수정`)
- type 종류: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`
- `$ARGUMENTS`에 제목이 주어지면 그대로 사용, 없으면 커밋 내용을 분석해 자동 생성
- type별 제목·본문 작성 예시는 [examples.md](examples.md)를 참조한다

## 라벨 & Assignee

**라벨** — type에 따라 자동 선택 (중복 가능):

| type | 라벨 |
|------|------|
| feat | `✨feat`, `BE` |
| fix | `🐞bug`, `BE` |
| refactor | `🛠️refactor`, `BE` |
| chore | `⚙️chore`, `BE` |
| docs | `📝docs`, `BE` |
| test | `🧪 test`, `BE` |

`BE` 라벨은 type에 관계없이 **항상** 포함한다.

우선순위 라벨(`p-*`)은 `$ARGUMENTS`에 명시된 경우에만 추가한다.

**Assignee** — `gh api user --jq '.login'`으로 현재 git 사용자 GitHub 로그인을 조회해 자동 지정한다.

## 템플릿 작성 규칙

`.github/pull_request_template.md`의 섹션을 그대로 유지하고 아래 기준으로 채운다.

| 섹션         | 작성 기준                                         |
|------------|-----------------------------------------------|
| ✅ 체크리스트    | `--base` 브랜치 확인 후 `[x]`로 체크                   |
| 🔥 연관 이슈   | `$ARGUMENTS`에 이슈 번호가 있으면 `close #N`, 없으면 `없음` |
| 🚀 작업 내용   | 변경된 파일·커밋을 분석해 번호 목록으로 작성                     |
| 💬 리뷰 중점사항 | 리뷰어가 특히 확인해야 할 설계 결정, 트레이드오프, 주의 사항           |

## 실행

```bash
# base 브랜치 (사전 작업 1에서 정한 값, 기본 be/dev)
BASE="be/dev"

# assignee 조회
gh api user --jq '.login'

# 본문은 --body-file - 로 stdin에서 읽는다 (heredoc)
gh pr create \
  --title "[fix] 카드 점수 집계 누락 수정" \
  --base "$BASE" \
  --label "🐞bug,BE" \
  --assignee "$(gh api user --jq '.login')" \
  --body-file - <<'EOF'
<템플릿 채운 내용>
EOF
```

완료 후 생성된 PR의 본문이 의도대로 반영됐는지 확인하고, PR URL을 사용자에게 출력한다.
