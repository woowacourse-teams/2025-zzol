#!/usr/bin/env bash
# deep-review 5단계: 정돈한 리뷰 본문을 stdin 으로 받아 PR 에 코멘트 1개로 게시한다.
# 사용법: post-comment.sh [PR번호|URL] < body.md   (생략하면 현재 브랜치의 PR)
set -uo pipefail

PR_URL="${1:-$(gh pr view --json url -q .url 2>/dev/null)}"
[ -z "$PR_URL" ] && { echo "ABORT: PR URL 조회 실패 — PR이 생성됐는지 확인하세요."; exit 1; }

BODY="$(cat)"
[ -z "$BODY" ] && { echo "ABORT: 코멘트 본문이 비었다."; exit 1; }

printf '## 🤖 클로드 코드 리뷰\n\n%s\n' "$BODY" | gh pr comment "$PR_URL" --body-file -
echo "게시 완료: $PR_URL"
