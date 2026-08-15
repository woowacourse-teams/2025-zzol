#!/usr/bin/env bash
# 이 워크트리의 앱을 로컬에서 띄운다 (#1660).
#
# 사용: bash run.sh <backend|frontend>
#
# 포그라운드로 실행한다 — 백그라운드·로그 관리는 호출자(사람은 터미널, Claude 는 run-local 스킬)가 정한다.
# 포트는 .ports 에서 읽어 환경변수로 export 한다. dotenv 계열은 이미 있는 환경변수를
# 덮지 않으므로, 공유 심볼릭 링크된 .env 값보다 이 값이 이긴다.
set -euo pipefail

TARGET="${1:?ABORT: backend 또는 frontend 를 인자로 넘겨야 한다}"

# 워크트리 루트 (이 스크립트는 <root>/.claude/skills/run-local/ 에 있다)
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"

if [ -f "$ROOT/.ports" ]; then
  # shellcheck disable=SC1091
  . "$ROOT/.ports"
else
  echo "경고: .ports 가 없다 — 기본 포트를 쓴다. worktree-setup.sh 를 돌리면 워크트리 전용 포트가 잡힌다." >&2
fi

BACKEND_PORT="${BACKEND_PORT:-8080}"
FRONTEND_PORT="${FRONTEND_PORT:-3000}"

case "$TARGET" in
  backend)
    cd "$ROOT/backend"
    export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-local}"
    export SERVER_PORT="$BACKEND_PORT"
    echo "backend → http://localhost:$BACKEND_PORT"
    # 인자 없이 돈다. bootRun 의 workingDir 이 backend/ 라서
    #  - springboot4-dotenv 가 backend/.env 를 읽고
    #  - spring.docker.compose.file 이 해석돼 컨테이너가 자동 기동한다
    exec ./gradlew :app:bootRun --console=plain
    ;;
  frontend)
    cd "$ROOT/frontend"
    # node_modules 는 워크트리마다 따로 필요하다(gitignore 대상이라 worktree add 가 안 가져온다).
    # 워크트리 생성 때 일괄 설치하지 않는 건, 백엔드·문서만 건드리는 작업이 대부분이라
    # 쓰지도 않을 설치에 매번 시간을 물기 때문이다. 실제로 띄울 때만 채운다.
    if [ ! -x node_modules/.bin/webpack ]; then
      echo "node_modules 가 없다 — npm ci 로 설치한다 (최초 1회)"
      npm ci
    fi
    export PORT="$FRONTEND_PORT"
    # 프론트가 '자기 워크트리의' 백엔드를 가리키게 한다.
    # 이게 없으면 공유 .env.development 값을 따라가 다른 워크트리의 백엔드에 붙는다.
    export API_URL="http://localhost:$BACKEND_PORT"
    echo "frontend → http://localhost:$FRONTEND_PORT (API_URL=$API_URL)"
    exec npm run dev
    ;;
  *)
    echo "ABORT: 알 수 없는 대상 '$TARGET' — backend 또는 frontend"; exit 1
    ;;
esac
