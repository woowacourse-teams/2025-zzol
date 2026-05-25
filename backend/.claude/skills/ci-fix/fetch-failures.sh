#!/usr/bin/env bash
# Usage: bash fetch-failures.sh <PR번호>
set -euo pipefail

PR=${1:-$(gh pr view --json number -q .number)}

CONCLUSION=$(gh pr checks "$PR" --json name,status,conclusion 2>/dev/null \
  | jq -r '.[] | select(.name == "Backend Test Results") | .conclusion')

if [ "$CONCLUSION" != "failure" ]; then
  echo "CI가 통과 상태입니다 (conclusion: ${CONCLUSION})"
  exit 0
fi

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
HEAD_SHA=$(gh pr view "$PR" --json headRefOid -q .headRefOid)

CHECK_RUN_ID=$(gh api "repos/${REPO}/commits/${HEAD_SHA}/check-runs" \
  --jq '.check_runs[] | select(.name == "Backend Test Results") | .id')

ANNOTATIONS=$(gh api "repos/${REPO}/check-runs/${CHECK_RUN_ID}/annotations" \
  --jq '.[] | "\(.title)\n  경로: \(.path):\(.start_line)\n  메시지: \(.message)\n"')

if [ -n "$ANNOTATIONS" ]; then
  echo "$ANNOTATIONS"
  exit 0
fi

# annotations 없음 → CI 런 로그 폴백
BRANCH=$(gh pr view "$PR" --json headRefName -q .headRefName)
RUN_ID=$(gh run list --branch "$BRANCH" \
  --workflow backend-ci.yml --limit 1 --json databaseId -q '.[0].databaseId')

gh run view "${RUN_ID}" --log-failed 2>/dev/null \
  | grep -E "(FAILED|MethodSource|expected:|but was:)" | head -80
