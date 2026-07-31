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
# 문서 경로 안이라도 실행 스크립트는 소스다. 이걸 문서로 세면 리뷰를 돌릴지 결정하는 코드(이 파일 자신)가
# 렌즈를 하나도 통과하지 않고 머지된다.
SCRIPT_RE='\.sh$'
SRC="$(printf '%s\n' "$CHANGED" | grep -Ev "$DOC_RE" || true)"
SRC_SH="$(printf '%s\n' "$CHANGED" | grep -E "$DOC_RE" | grep -E "$SCRIPT_RE" || true)"
SRC="$(printf '%s\n%s\n' "$SRC" "$SRC_SH" | grep -v '^[[:space:]]*$' | sort -u || true)"

# 문서를 뺀 변경 줄 수 — issue-workflow 의 경량 경로 판정이 쓴다(문서 길이에 휘둘리지 않게).
# numstat 은 "추가<TAB>삭제<TAB>경로" 라 경로가 줄 시작이 아니다. grep 으로 줄 전체를 거르면
# DOC_RE 의 ^ 앵커가 빗나가 문서 변경까지 세어지므로 경로 필드($3)로만 판정한다. 두 가지가 더 있다:
#  - rename 은 $3 가 "옛경로 => 새경로"(또는 "a/{b => c}/d") 한 필드다. 한쪽만 보면 docs/ → frontend/src/
#    이동이 문서로 분류돼 실코드 변경이 통째로 빠진다. 양쪽 중 하나라도 소스면 소스로 센다(보수적).
#  - 바이너리는 추가/삭제가 "-" 라 줄 수가 없다. 0 으로 더하면 이미지 교체가 "0 줄" 이 되어 경량으로
#    분류되므로, 더하지 않고 SRC_BINARY=1 로 따로 알린다(줄 수로 잴 수 없다는 뜻).
EVAL="$(git diff --numstat "origin/$BASE"...HEAD | DOC_RE="$DOC_RE" SCRIPT_RE="$SCRIPT_RE" awk -F'\t' '
function is_src(p,   i, n, a, t) {
  n = split(p, a, / => /)
  for (i = 1; i <= n; i++) {
    t = a[i]; gsub(/[{}]/, "", t)
    if (t ~ ENVIRON["SCRIPT_RE"] || t !~ ENVIRON["DOC_RE"]) return 1
  }
  return 0
}
{ if (!is_src($3)) next
  if ($1 == "-" || $2 == "-") { bin = 1; next }
  s += $1 + $2 }
END { print (s+0) "\t" (bin+0) }')"
SRC_LINES="${EVAL%%	*}"
[ -n "$SRC_LINES" ] || SRC_LINES=0
echo "SRC_LINES=$SRC_LINES"
[ "${EVAL##*	}" = "1" ] && echo "SRC_BINARY=1"

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
