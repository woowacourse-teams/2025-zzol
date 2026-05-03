---
name: create-pr
description: PR 템플릿을 읽어 GitHub Pull Request를 생성한다.
argument-hint: "[PR 제목 (선택)] [--base=브랜치명 (기본: be/dev)]"
allowed-tools: Read, Bash, Glob
---

# create-pr

## 사전 작업

1. `.github/pull_request_template.md`를 Read로 읽어 템플릿 구조를 파악한다.
2. `git log be/dev..HEAD --oneline`으로 이번 브랜치의 커밋 목록을 확인한다.
3. `git diff be/dev...HEAD --stat`으로 변경된 파일 목록을 확인한다.

## PR 제목 규칙

- 형식: `[type] 한국어 설명`
- type 종류: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`
- `$ARGUMENTS`에 제목이 주어지면 그대로 사용, 없으면 커밋 내용을 분석해 자동 생성

## 라벨 & Assignee

**라벨** — type에 따라 자동 선택 (중복 가능):

| type | 라벨 |
|------|------|
| feat | `✨feat`, `BE` 또는 `FE` |
| fix | `🐞bug`, `BE` 또는 `FE` |
| refactor | `🛠️refactor`, `BE` 또는 `FE` |
| chore | `⚙️chore` |
| docs | `📝docs` |
| test | `🧪 test` |

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
# assignee 조회
gh api user --jq '.login'

gh pr create \
  --title "[type] 제목" \
  --base be/dev \
  --label "라벨1,라벨2" \
  --assignee "$(gh api user --jq '.login')" \
  --body "$(cat <<'EOF'
<템플릿 채운 내용>
EOF
)"
```

완료 후 PR URL을 사용자에게 출력한다.
