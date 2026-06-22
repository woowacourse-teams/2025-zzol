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
3. `git log "origin/$BASE"..HEAD --oneline` 와 `git diff "origin/$BASE"...HEAD --stat` 으로 이번 브랜치의 커밋·변경 파일을 확인한다 (로컬 `$BASE`는 stale일 수 있으니 `origin/` 기준).
4. **브랜치를 원격에 올린다 (`gh pr create`의 전제).** 먼저 `bash .claude/skills/commit/preflight.sh`로 보호 브랜치·detached HEAD를 차단한다(`ABORT` 출력 시 중단·보고 — 보호 브랜치 목록 SSOT, 변경 인벤토리 출력은 무시). 통과하면 현재 브랜치를 **자기 이름 명시 refspec**으로 push한다 — `git push -u origin HEAD:<현재브랜치>` (bare `git push` 금지). 이미 올라가 있으면 생략한다.

## PR 제목

- 형식: `[type] 한국어 설명` (예: `[fix] 카드 점수 집계 누락 수정`). type: `feat`·`fix`·`refactor`·`chore`·`docs`·`test`
- `$ARGUMENTS`에 제목이 있으면 그대로, 없으면 커밋 내용으로 자동 생성
- type별 제목·본문 예시는 [examples.md](examples.md) 참조

## 라벨 & Assignee

- 라벨: 항상 `BE` + type별 1개 — feat `✨feat` / fix `🐞bug` / refactor `🛠️refactor` / chore `⚙️chore` / docs `📝docs` / test `🧪 test`. 우선순위(`p-*`)는 `$ARGUMENTS`에 있을 때만 추가
- Assignee: `gh api user --jq '.login'` 결과로 자동 지정

## 작성 원칙 (본문 공통)

리뷰어가 빠르게 이해하는 것을 최우선으로, 쉬운 용어와 간결한 문장으로 작성한다.

- **쉬운 용어**: 전문 용어·영어 약어(point of use, redundant, hermetic, drift 등)를 피하고 풀어서 쓴다. 꼭 필요한 기술 용어는 한 줄 풀이를 붙인다.
- **간결함**: 한 항목은 1~2문장. "무엇을 왜 바꿨는지"를 먼저 적는다.
- **결정·트레이드오프는 이유와 함께**: 리뷰어가 되묻지 않아도 알 수 있게 적는다.

## 템플릿 작성

`.github/pull_request_template.md` 섹션을 유지하고 채운다.

- ✅ 체크리스트: `--base` 확인 후 `[x]`
- 🔥 연관 이슈: 현재 브랜치명 `be/<type>/<N>-<slug>`(create-issue가 만든 형식)에서 이슈 번호 `N`을 추출해 `close #N`. 브랜치명에 번호가 없으면 `없음`
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
