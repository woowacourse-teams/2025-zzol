#!/usr/bin/env bash
# Usage: bash fetch-failures.sh <PR번호>
set -euo pipefail

PR=${1:-$(gh pr view --json number -q .number 2>/dev/null || true)}
if [ -z "$PR" ]; then
  echo "PR 번호를 찾을 수 없습니다. 인자로 PR 번호를 넘기거나 PR이 있는 브랜치에서 실행하세요." >&2
  exit 1
fi

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
HEAD_SHA=$(gh pr view "$PR" --json headRefOid -q .headRefOid)

CHECK_RUN_ID=$(gh api "repos/${REPO}/commits/${HEAD_SHA}/check-runs" \
  --jq '[.check_runs[] | select(.name == "Backend Test Results")] | first | .id')

if [ -z "$CHECK_RUN_ID" ] || [ "$CHECK_RUN_ID" = "null" ]; then
  echo "Backend Test Results 체크를 찾을 수 없습니다." >&2
  exit 1
fi

CONCLUSION=$(gh api "repos/${REPO}/check-runs/${CHECK_RUN_ID}" --jq '.conclusion')

if [ "$CONCLUSION" = "success" ]; then
  echo "CI가 통과 상태입니다 (conclusion: success)"
  exit 0
elif [ "$CONCLUSION" = "null" ] || [ -z "$CONCLUSION" ]; then
  echo "CI 상태: 아직 완료되지 않은 상태입니다." >&2
  exit 1
elif [ "$CONCLUSION" != "failure" ]; then
  echo "CI 상태: ${CONCLUSION} — 실패가 아닙니다." >&2
  exit 1
fi

ANNOTATIONS=$(gh api --paginate "repos/${REPO}/check-runs/${CHECK_RUN_ID}/annotations" \
  --jq '.[] | select(.annotation_level == "failure") | "\(.title)\n  경로: \(.path):\(.start_line)\n  메시지: \(.message)\n"')

if [ -n "$ANNOTATIONS" ]; then
  echo "$ANNOTATIONS"
  exit 0
fi

# annotations 없음 → CI 런 로그 폴백
BRANCH=$(gh pr view "$PR" --json headRefName -q .headRefName)
RUN_ID=$(gh run list --branch "$BRANCH" \
  --workflow backend-ci.yml --limit 1 --json databaseId -q '.[0].databaseId' || true)

if [ -z "$RUN_ID" ] || [ "$RUN_ID" = "null" ]; then
  echo "Annotations에서 실패 정보를 가져올 수 없습니다. 로그를 직접 확인해주세요."
  exit 0
fi

LOG_LINES=$(gh run view "${RUN_ID}" --log-failed 2>/dev/null \
  | grep -E "(FAILED|MethodSource|expected:|but was:)" || true)

if [ -z "$LOG_LINES" ]; then
  echo "Annotations에서 실패 정보를 가져올 수 없습니다. 로그를 직접 확인해주세요."
  exit 0
fi

echo "$LOG_LINES" | head -80
