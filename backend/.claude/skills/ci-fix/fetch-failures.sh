#!/usr/bin/env bash
# Usage: bash fetch-failures.sh <PR번호>
#
# PR head 브랜치의 최근 backend-ci.yml 런에서 실패한 테스트를 추출한다.
#
# 배경(#1521): dorny/test-reporter는 결과를 job summary에만 쓰고 "Backend Test Results"
# 체크런을 생성하지 않는다(성공·실패 런 모두 없음). 또 job summary(step summary) 텍스트는
# 공개 REST API로 가져올 수 없다. 따라서 체크런 annotation이 아니라 GitHub Actions
# 런 로그의 gradle JUnit 출력을 직접 파싱한다.
set -euo pipefail

PR=${1:-$(gh pr view --json number -q .number 2>/dev/null || true)}
if [ -z "$PR" ]; then
  echo "PR 번호를 찾을 수 없습니다. 인자로 PR 번호를 넘기거나 PR이 있는 브랜치에서 실행하세요." >&2
  exit 1
fi

BRANCH=$(gh pr view "$PR" --json headRefName -q .headRefName)

# PR head 브랜치의 가장 최근 backend-ci.yml 런 (id/상태/결론)
RUN_INFO=$(gh run list --branch "$BRANCH" --workflow backend-ci.yml --limit 1 \
  --json databaseId,status,conclusion \
  --jq '.[0] | "\(.databaseId)\t\(.status)\t\(.conclusion)"' 2>/dev/null || true)
IFS=$'\t' read -r RUN_ID STATUS CONCLUSION <<< "${RUN_INFO:-}"

if [ -z "${RUN_ID:-}" ] || [ "$RUN_ID" = "null" ]; then
  echo "backend-ci.yml 런을 찾을 수 없습니다. PR에 백엔드 CI가 실행됐는지 확인해주세요." >&2
  exit 1
fi

if [ "$STATUS" != "completed" ]; then
  echo "CI가 아직 실행 중입니다 (status: ${STATUS}). 완료 후 다시 시도하세요." >&2
  exit 1
fi

if [ "$CONCLUSION" = "success" ]; then
  echo "CI가 통과 상태입니다 (conclusion: success)"
  exit 0
fi

if [ "$CONCLUSION" != "failure" ]; then
  echo "CI 결론: ${CONCLUSION} — 실패가 아닙니다 (취소·스킵 등). 재실행이 필요할 수 있습니다." >&2
  exit 1
fi

# 실패한 스텝 로그에서 gradle JUnit 실패를 파싱한다.
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
