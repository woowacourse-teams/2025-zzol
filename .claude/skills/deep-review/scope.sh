#!/usr/bin/env bash
# deep-review 1단계: 리뷰 범위·렌즈 조건을 판별해 key=value 로 출력한다.
# 사용법: scope.sh [base-branch]   (기본 dev)
set -uo pipefail

BASE="${1:-dev}"
REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT" || exit 1
git fetch -q origin "$BASE" || { echo "ABORT: origin/$BASE fetch 실패"; exit 1; }

CHANGED="$(git diff --name-only "origin/$BASE"...HEAD)"
DIRTY="$(git status --porcelain)"

echo "REPO_ROOT=$REPO_ROOT"
echo "RANGE=origin/$BASE...HEAD"

if [ -z "$CHANGED" ]; then
  echo "NO_CHANGE=1"
  [ -n "$DIRTY" ] && echo "DIRTY=1  # 커밋 안 된 변경만 있다 — 커밋 후 다시 실행"
  exit 0
fi

# 문서·에이전트 정의·워크플로우만 바뀐 경우를 걸러낸다 (소스 렌즈를 헛돌리지 않기 위해)
DOC_RE='(^|/)\.claude/|(^|/)docs/|\.md$|(^|/)\.github/'
SRC="$(printf '%s\n' "$CHANGED" | grep -Ev "$DOC_RE" || true)"

# 문서를 뺀 변경 줄 수 — issue-workflow 의 경량 경로 판정이 쓴다(문서 길이에 휘둘리지 않게).
# numstat 은 "추가<TAB>삭제<TAB>경로" 라 경로가 줄 시작이 아니다.
# grep 으로 줄 전체를 걸러내면 DOC_RE 의 ^ 앵커가 빗나가 문서 변경까지 세어진다 — 경로 필드($3)로만 판정한다.
SRC_LINES=0
[ -n "$SRC" ] && SRC_LINES="$(git diff --numstat "origin/$BASE"...HEAD \
  | DOC_RE="$DOC_RE" awk -F'\t' '$3 !~ ENVIRON["DOC_RE"] {s+=$1+$2} END{print s+0}')"
echo "SRC_LINES=$SRC_LINES"

[ -z "$SRC" ] && echo "SRC_EMPTY=1"
printf '%s\n' "$SRC" | grep -q '^backend/'  && echo "HAS_BE=1"
printf '%s\n' "$SRC" | grep -q '^frontend/' && echo "HAS_FE=1"
# 보안 렌즈 조건 — 인증·비밀값·입력검증·외부 노출 경로
printf '%s\n' "$SRC" | grep -qEi 'security|jwt|auth|token|credential|\.env|application.*\.ya?ml|filter|interceptor' \
  && echo "NEEDS_SECURITY=1"
[ -n "$DIRTY" ] && echo "DIRTY=1  # 커밋 안 된 변경은 리뷰 범위에서 빠진다"

echo "--- CHANGED ---"
printf '%s\n' "$CHANGED"
echo "--- SRC ---"
printf '%s\n' "$SRC"
