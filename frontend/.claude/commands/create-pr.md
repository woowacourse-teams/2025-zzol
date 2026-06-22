---
description: PR 템플릿을 읽어 GitHub Pull Request를 생성한다.
argument-hint: '[PR 제목 (선택)] [--base=브랜치명 (기본: fe/dev)]'
allowed-tools: Read, Bash, Glob
---

# create-pr

## 사전 작업

1. `../.github/pull_request_template.md`를 Read로 읽어 템플릿 구조를 파악한다.
2. `git log fe/dev..HEAD --oneline`으로 이번 브랜치의 커밋 목록을 확인한다.
3. `git diff fe/dev...HEAD --stat`으로 변경된 파일 목록을 확인한다.

## PR 제목 규칙

- 형식: `[type] 한국어 설명`
- type 종류: `feat`, `fix`, `refactor`, `chore`, `docs`, `test`
- `$ARGUMENTS`에 제목이 주어지면 그대로 사용, 없으면 커밋 내용을 분석해 자동 생성

## 라벨 & Assignee

**라벨** — type에 따라 자동 선택 (중복 가능):

| type     | 라벨               |
| -------- | ------------------ |
| feat     | `✨feat`, `FE`     |
| fix      | `🐞bug`, `FE`      |
| refactor | `🛠️refactor`, `FE` |
| chore    | `⚙️chore`          |
| docs     | `📝docs`           |
| test     | `🧪 test`          |

우선순위 라벨(`p-*`)은 `$ARGUMENTS`에 명시된 경우에만 추가한다.

**Assignee** — `gh api user --jq '.login'`으로 현재 GitHub 로그인을 조회해 자동 지정한다.

## 작성 원칙 (본문 공통)

리뷰어가 빠르게 이해하는 것을 최우선으로, 쉬운 용어와 간결한 문장으로 작성한다.

- **쉬운 용어**: 전문 용어·영어 약어(point of use, redundant, hermetic, drift 등)를 피하고 풀어서 쓴다. 꼭 필요한 기술 용어는 한 줄 풀이를 붙인다.
- **간결함**: 한 항목은 1~2문장. "무엇을 왜 바꿨는지"를 먼저 적는다.
- **결정·트레이드오프는 이유와 함께**: 리뷰어가 되묻지 않아도 알 수 있게 적는다.

## 템플릿 작성 규칙

`../.github/pull_request_template.md`의 섹션을 그대로 유지하고 아래 기준으로 채운다.

| 섹션             | 작성 기준                                                    |
| ---------------- | ------------------------------------------------------------ |
| ✅ 체크리스트    | base 브랜치 확인 후 `[x]`로 체크                             |
| 🔥 연관 이슈     | `$ARGUMENTS`에 이슈 번호가 있으면 `close #N`, 없으면 `없음`  |
| 🚀 작업 내용     | 변경된 파일·커밋을 분석해 번호 목록으로 작성                 |
| 💬 리뷰 중점사항 | 리뷰어가 특히 확인해야 할 설계 결정, 트레이드오프, 주의 사항 |

## 실행

```bash
# assignee 조회
gh api user --jq '.login'

gh pr create \
  --title "[type] 제목" \
  --base fe/dev \
  --label "라벨1,라벨2" \
  --assignee "$(gh api user --jq '.login')" \
  --body "$(cat <<'EOF'
<템플릿 채운 내용>
EOF
)"
```

완료 후 PR URL을 사용자에게 출력한다.
