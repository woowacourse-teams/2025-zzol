#!/usr/bin/env bash
# Usage: bash fetch-failures.sh <PR번호>
#
# PR head 브랜치의 최근 backend-ci.yml 런에서 실패한 테스트를 추출한다.
#
# 배경(#1521 → #1564): dorny/test-reporter는 "Backend Test Results" 체크런을 만들지
# 않아 annotation을 쓸 수 없었고(#1521), 로그 파싱으로 우회했다. #1564에서 OOM 때문에
# EnricoMi로 롤백하며 체크런이 다시 생성될 수 있으므로, 체크런 annotation을 먼저
# 시도하고 없으면(체크런 미생성·annotation 없음) Actions 런 로그의 gradle JUnit 출력
# 직접 파싱으로 폴백한다. 어느 리포터를 쓰든 동작한다.
set -euo pipefail

PR=${1:-$(gh pr view --json number -q .number 2>/dev/null || true)}
if [ -z "$PR" ]; then
  echo "PR 번호를 찾을 수 없습니다. 인자로 PR 번호를 넘기거나 PR이 있는 브랜치에서 실행하세요." >&2
  exit 1
fi

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner)
PR_INFO=$(gh pr view "$PR" --json headRefName,headRefOid -q '"\(.headRefName)\t\(.headRefOid)"')
IFS=$'\t' read -r BRANCH HEAD_SHA <<< "$PR_INFO"

# PR head 브랜치의 가장 최근 backend-ci.yml 런 (id/상태/결론). 체크런이 없거나
# annotation이 비었을 때 로그 폴백에도 이 RUN_ID를 그대로 쓴다.
RUN_INFO=$(gh run list --branch "$BRANCH" --workflow backend-ci.yml --limit 1 \
  --json databaseId,status,conclusion \
  --jq '.[0] | "\(.databaseId)\t\(.status)\t\(.conclusion)"' 2>/dev/null || true)
IFS=$'\t' read -r RUN_ID RUN_STATUS RUN_CONCLUSION <<< "${RUN_INFO:-}"

if [ -z "${RUN_ID:-}" ] || [ "$RUN_ID" = "null" ]; then
  echo "backend-ci.yml 런을 찾을 수 없습니다. PR에 백엔드 CI가 실행됐는지 확인해주세요." >&2
  exit 1
fi

if [ "$RUN_STATUS" != "completed" ]; then
  echo "CI가 아직 실행 중입니다 (status: ${RUN_STATUS}). 완료 후 다시 시도하세요." >&2
  exit 1
fi

if [ "$RUN_CONCLUSION" = "success" ]; then
  echo "CI가 통과 상태입니다 (conclusion: success)"
  exit 0
fi

if [ "$RUN_CONCLUSION" != "failure" ]; then
  echo "CI 결론: ${RUN_CONCLUSION} — 실패가 아닙니다 (취소·스킵 등). 재실행이 필요할 수 있습니다." >&2
  exit 1
fi

# 1) 체크런 우선: "Backend Test Results" 체크런의 실패 annotation을 시도한다.
CHECK_RUN_ID=$(gh api "repos/${REPO}/commits/${HEAD_SHA}/check-runs" \
  --jq '[.check_runs[] | select(.name == "Backend Test Results")] | first | .id' 2>/dev/null || true)

if [ -n "$CHECK_RUN_ID" ] && [ "$CHECK_RUN_ID" != "null" ]; then
  ANNOTATIONS=$(gh api --paginate "repos/${REPO}/check-runs/${CHECK_RUN_ID}/annotations" \
    --jq '.[] | select(.annotation_level == "failure") | "\(.title)\n  경로: \(.path):\(.start_line)\n  메시지: \(.message)\n"' 2>/dev/null || true)
  if [ -n "$ANNOTATIONS" ]; then
    echo "$ANNOTATIONS"
    exit 0
  fi
fi

# 2) 로그 폴백: 체크런이 없거나 annotation이 비어 있을 때, 실패한 스텝 로그에서
# gradle JUnit 실패를 직접 파싱한다.
# 로그 각 줄은 "<job>\t<step>\t<ISO timestamp> <message>" 형식이라 timestamp까지 벗겨낸다.
# gradle 출력 형식:
#   <Suite> > <testMethod>() FAILED
#       <exception> at <File>.java:<line>
#   NNN tests completed, M failed
LOG=$(gh run view "${RUN_ID}" --log-failed 2>/dev/null || true)

if [ -z "$LOG" ]; then
  echo "런 로그를 가져오지 못했습니다 (만료되었거나 접근 불가). 웹에서 직접 확인해주세요:" >&2
  gh run view "${RUN_ID}" --json url --jq '.url' >&2 || true
  exit 1
fi

PARSED=$(printf '%s\n' "$LOG" | awk '
    { line=$0; sub(/^.*[0-9][0-9]:[0-9][0-9]:[0-9][0-9][.][0-9]+Z /, "", line) }
    # 테스트 메서드 실패만("()" 포함). "> Task :game:test FAILED" 같은 gradle 태스크 줄은 제외.
    line ~ /> .+\(\) FAILED$/ { sub(/ FAILED$/, "", line); n++; test[n]=line; next }
    n>0 && cause[n]=="" && line ~ / at .+\.java:[0-9]+/ { sub(/^[ \t]+/, "", line); cause[n]=line; next }
    line ~ /[0-9]+ tests completed, [0-9]+ failed/ { summary=line }
    END {
      for (i=1;i<=n;i++) {
        printf "[실패 %d] %s\n", i, test[i]
        if (cause[i]!="") printf "  원인: %s\n", cause[i]
      }
      if (summary!="") printf "\n집계: %s\n", summary
      if (n==0) print "__NO_FAILED_TESTS__"
    }')

if [ "$PARSED" = "__NO_FAILED_TESTS__" ]; then
  echo "테스트 실패 라인을 찾지 못했습니다 (컴파일·인프라 등 테스트 외 실패일 수 있음)." >&2
  echo "실패 스텝 로그 마지막 부분:" >&2
  printf '%s\n' "$LOG" | tail -30 >&2
  exit 0
fi

echo "$PARSED"
