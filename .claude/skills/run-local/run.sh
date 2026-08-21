#!/usr/bin/env bash
# 이 워크트리의 앱을 로컬에서 띄운다 (#1660).
#
# 사용: bash run.sh <backend|frontend>
#
# 포그라운드로 실행한다 — 백그라운드·로그 관리는 호출자(사람은 터미널, Claude 는 run-local 스킬)가 정한다.
#
# 포트는 백엔드 8080·프론트 3000 고정이다. 워크트리마다 다른 포트를 쓰려고 했으나 성립하지 않았다 —
# CORS(allowed-origins)·OAuth 콜백·QR prefix·DB URL 이 전부 그 두 포트로 하드코딩돼 있어
# 포트만 바꾸면 화면은 뜨는데 API 가 전부 막힌다. 그래서 한 번에 한 워크트리만 띄우고,
# 포트가 잡혀 있으면 조용히 다른 포트로 새지 말고 시끄럽게 멈춘다.
set -euo pipefail

TARGET="${1:?ABORT: backend 또는 frontend 를 인자로 넘겨야 한다}"

# 워크트리 루트 (이 스크립트는 <root>/.claude/skills/run-local/ 에 있다)
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

BACKEND_PORT=8080
FRONTEND_PORT=3000

require_free_port() {
  local port="$1" what="$2"
  command -v lsof >/dev/null || return 0   # lsof 가 없으면 검사를 건너뛴다(실행은 막지 않는다)
  lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1 || return 0
  echo "ABORT: 포트 $port 가 이미 사용 중이다 ($what)." >&2
  echo "  다른 워크트리에서 띄운 것이라면 먼저 끈다:" >&2
  echo "    lsof -nP -iTCP:$port -sTCP:LISTEN -t | xargs kill" >&2
  echo "  포트를 바꿔 우회하지 않는다 — CORS·OAuth 콜백이 $BACKEND_PORT/$FRONTEND_PORT 고정이라 앱이 반쯤 깨진다." >&2
  exit 1
}

case "$TARGET" in
  backend)
    require_free_port "$BACKEND_PORT" "백엔드"
    cd "$ROOT/backend"
    export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
    echo "backend → http://localhost:$BACKEND_PORT"
    # 인자 없이 돈다. bootRun 의 workingDir 이 backend/ 라서
    #  - springboot4-dotenv 가 backend/.env 를 읽고
    #  - spring.docker.compose.file 이 해석돼 컨테이너가 자동 기동한다
    exec ./gradlew :app:bootRun --console=plain
    ;;
  frontend)
    require_free_port "$FRONTEND_PORT" "프론트엔드"
    cd "$ROOT/frontend"
    # node_modules 는 워크트리마다 따로 필요하다(gitignore 대상이라 worktree add 가 안 가져온다).
    # 워크트리 생성 때 일괄 설치하지 않는 건, 백엔드·문서만 건드리는 작업이 대부분이라
    # 쓰지도 않을 설치에 매번 시간을 물기 때문이다. 실제로 띄울 때만 채운다.
    if [ ! -x node_modules/.bin/webpack ]; then
      echo "node_modules 가 없다 — npm ci 로 설치한다 (최초 1회)"
      npm ci
    fi
    echo "frontend → http://localhost:$FRONTEND_PORT"
    exec npm run dev
    ;;
  *)
    echo "ABORT: 알 수 없는 대상 '$TARGET' — backend 또는 frontend"; exit 1
    ;;
esac
