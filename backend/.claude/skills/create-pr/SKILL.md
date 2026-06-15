---
name: create-pr
description: PR 템플릿을 읽어 GitHub Pull Request를 생성한다.
argument-hint: "[PR 제목 (선택)] [--base=브랜치명 (기본: be/dev)]"
allowed-tools: Read, Bash, Glob
---

# create-pr

## 사전 작업

1. base 브랜치를 정한다. `$ARGUMENTS`에 `--base=<브랜치>`가 있으면 그 값, 없으면 `be/dev`. 이후 이 값을 `$BASE`로 쓴다.
2. PR 템플릿은 모노레포 루트에 있다. `REPO_ROOT="$(git rev-parse --show-toplevel)"` 로 루트를 구해 `${REPO_ROOT}/.github/pull_request_template.md`를 Read한다.
3. `git log "$BASE"..HEAD --oneline` 와 `git diff "$BASE"...HEAD --stat` 으로 이번 브랜치의 커밋·변경 파일을 확인한다.

## PR 제목

- 형식: `[type] 한국어 설명` (예: `[fix] 카드 점수 집계 누락 수정`). type: `feat`·`fix`·`refactor`·`chore`·`docs`·`test`
- `$ARGUMENTS`에 제목이 있으면 그대로, 없으면 커밋 내용으로 자동 생성
- type별 제목·본문 예시는 [examples.md](examples.md) 참조

## 라벨 & Assignee

- 라벨: 항상 `BE` + type별 1개 — feat `✨feat` / fix `🐞bug` / refactor `🛠️refactor` / chore `⚙️chore` / docs `📝docs` / test `🧪 test`. 우선순위(`p-*`)는 `$ARGUMENTS`에 있을 때만 추가
- Assignee: `gh api user --jq '.login'` 결과로 자동 지정

## 템플릿 작성

`.github/pull_request_template.md` 섹션을 유지하고 채운다.

- ✅ 체크리스트: `--base` 확인 후 `[x]`
- 🔥 연관 이슈: 이슈 번호가 있으면 `close #N`, 없으면 `없음`
- 🚀 작업 내용: 변경 파일·커밋을 번호 목록으로
- 💬 리뷰 중점사항: 설계 결정·트레이드오프·주의 사항

## 실행

```bash
BASE="be/dev"   # 사전 작업 1의 값
gh pr create \
  --title "[fix] 카드 점수 집계 누락 수정" \
  --base "$BASE" \
  --label "🐞bug,BE" \
  --assignee "$(gh api user --jq '.login')" \
  --body-file - <<'EOF'
<템플릿 채운 내용>
EOF
```

완료 후 PR 본문이 반영됐는지 확인하고 URL을 출력한다.
