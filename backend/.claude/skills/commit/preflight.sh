#!/usr/bin/env bash
# commit 스킬 프리플라이트: ① 보호 브랜치/detached HEAD 가드 ② 변경 인벤토리(tracked/untracked 분리)
#
# 출력 계약(호출 측이 파싱):
#   - 보호 브랜치·detached → "ABORT: <사유>" 출력 후 exit 1 (스킬은 즉시 중단·보고)
#   - 변경 없음            → "NO_CHANGES" 출력 후 exit 0
#   - 정상                 → "BRANCH_OK: <브랜치>" + TRACKED/UNTRACKED 섹션, exit 0
#
# 테스트용: PREFLIGHT_BRANCH 를 set 하면(빈 값 포함) 브랜치 감지를 오버라이드한다.
set -u

PROTECTED="dev be/dev be/prod fe/dev fe/prod main master"

# 현재 브랜치 (detached 면 빈 문자열). PREFLIGHT_BRANCH 가 set 돼 있으면 그 값을 쓴다.
if [ -n "${PREFLIGHT_BRANCH+set}" ]; then
  branch="$PREFLIGHT_BRANCH"
else
  branch="$(git symbolic-ref --short -q HEAD || true)"
fi

if [ -z "$branch" ]; then
  echo "ABORT: detached HEAD 상태입니다. 작업 브랜치로 전환한 뒤 커밋하세요."
  exit 1
fi

for p in $PROTECTED; do
  if [ "$branch" = "$p" ]; then
    echo "ABORT: 보호 브랜치 '$branch' 에는 직접 커밋할 수 없습니다 (git-push-safety 규칙). 작업 브랜치에서 PR로 반영하세요."
    exit 1
  fi
done

# 변경 인벤토리
status="$(git status --porcelain)"
if [ -z "$status" ]; then
  echo "NO_CHANGES"
  exit 0
fi

echo "BRANCH_OK: $branch"
echo "=== TRACKED (staged/modified — 커밋 대상) ==="
echo "$status" | grep -vE '^\?\?' || echo "(없음)"
echo "=== UNTRACKED (생성물일 수 있음 — 사용자 확인 전 git add 금지) ==="
echo "$status" | grep -E '^\?\?' || echo "(없음)"
exit 0
