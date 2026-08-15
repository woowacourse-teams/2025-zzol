#!/usr/bin/env bash
# 새 워크트리를 로컬 실행 가능한 상태로 만든다 (#1660).
#
#   1. 주 저장소의 gitignore 된 env 파일을 심볼릭 링크로 건다
#   2. 이 워크트리 전용 포트 쌍을 잡아 .ports 에 기록한다
#
# 사용: bash worktree-setup.sh <워크트리 경로>
#
# env 파일은 열지 않는다 — 경로만 다루므로 비밀값이 로그·출력에 남지 않는다.
set -euo pipefail

WT="${1:?ABORT: 워크트리 경로를 인자로 넘겨야 한다}"
[ -d "$WT" ] || { echo "ABORT: 워크트리 경로가 없다: $WT"; exit 1; }

MAIN="$(git -C "$WT" worktree list --porcelain | sed -n '1s/^worktree //p')"
[ -n "$MAIN" ] || { echo "ABORT: 주 저장소 경로를 찾지 못했다"; exit 1; }

# 주 저장소를 대상으로 돌리면 ln -sfn 이 자기 자신을 가리키는 링크를 만드는데,
# ln -f 는 타깃을 먼저 지우므로 원본 .env 가 파괴된다(자기참조 dangling 링크만 남는다).
# gitignore 대상이라 백업도 없어 복구가 불가능하다.
[ "$(cd "$WT" && pwd -P)" != "$(cd "$MAIN" && pwd -P)" ] || {
  echo "ABORT: 주 저장소에서는 돌리지 않는다 — 워크트리 경로를 넘겨라"; exit 1; }

# ── 1. env 심볼릭 링크 ────────────────────────────────────────────────
# 사본이 아니라 링크다. 원본이 하나만 존재해 오래된 사본이 남지 않고,
# 비밀값이 디스크에 한 벌만 있다. 워크트리별로 다른 값이 필요하면 그 링크만 cp 로 바꾼다.
#
# git 은 통째로 무시된 디렉터리를 "dir/" 한 줄로 접어서 내놓는다. 다른 워크트리가 그렇게 걸리므로
# pathspec 에 기대지 않고 명시적으로 거른다 — 안 거르면 워크트리 안에 워크트리를 링크하려다 죽는다.
#
# 프로세스 치환(`done < <(git ...)`)은 set -e·pipefail 어느 쪽에도 걸리지 않는다.
# git 이 실패하면 루프가 0회 돌고 exit 0 으로 끝나, 호출부의 `|| 경고` 가드가 감지하지 못한다.
# 그러면 이 스크립트가 막으려던 "env 없는 워크트리"가 조용히 만들어진다. 먼저 받아서 실패를 드러낸다.
env_files="$(git -C "$MAIN" ls-files --others --ignored --exclude-standard -- '*.env' '*.env.*')" \
  || { echo "ABORT: env 파일 목록 조회 실패"; exit 1; }

linked=0
while IFS= read -r f; do
  [ -n "$f" ] || continue
  case "$f" in
    .claude/worktrees/*) continue ;;   # 다른 워크트리의 사본
    */) continue ;;                    # 접힌 디렉터리
  esac
  [ -f "$MAIN/$f" ] || continue        # 실제 파일만
  # 링크가 아닌 실파일이 이미 있으면 워크트리 전용 값을 일부러 둔 것이다(위 주석의 cp 사용법).
  # 덮어쓰면 그 값이 말없이 사라진다.
  if [ -e "$WT/$f" ] && [ ! -L "$WT/$f" ]; then
    echo "  keep: $f (워크트리 전용 실파일)"
    linked=$((linked + 1))
    continue
  fi
  mkdir -p "$WT/$(dirname "$f")"
  ln -sfn "$MAIN/$f" "$WT/$f"
  echo "  link: $f"
  linked=$((linked + 1))
done <<< "$env_files"

[ "$linked" -gt 0 ] || echo "  (연결할 env 파일 없음)"

# ── 2. 워크트리 전용 포트 ─────────────────────────────────────────────
# 심볼릭 링크한 env 는 모든 워크트리가 공유하므로 포트를 거기 둘 수 없다.
# 여기서 잡은 값을 런처가 환경변수로 export 하면, dotenv 계열은 이미 있는
# 환경변수를 덮지 않으므로 링크된 파일 값을 이긴다.
port_taken() {
  local port="$1"
  lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1 && return 0
  grep -rqs "=${port}\$" "$MAIN/.claude/worktrees"/*/.ports 2>/dev/null && return 0
  return 1
}

find_free_port() {
  local port="$1"
  while port_taken "$port"; do
    port=$((port + 1))
  done
  echo "$port"
}

if [ -f "$WT/.ports" ]; then
  echo "  .ports 이미 있음 — 유지한다"
else
  BACKEND_PORT="$(find_free_port 8080)"
  FRONTEND_PORT="$(find_free_port 3000)"
  cat > "$WT/.ports" <<EOF
# 이 워크트리 전용 포트 (#1660). 런처가 읽어 환경변수로 export 한다.
BACKEND_PORT=$BACKEND_PORT
FRONTEND_PORT=$FRONTEND_PORT
EOF
  echo "  ports: backend=$BACKEND_PORT frontend=$FRONTEND_PORT"
fi
