#!/bin/bash
set -euo pipefail
export PATH="/usr/bin:/bin:$PATH"

# 애플리케이션 홈 디렉토리
APP_HOME=/opt/coffee-shout

echo "=== [BEFORE_BLOCK_TRAFFIC] Graceful Shutdown 시작 ==="

cd "${APP_HOME}" || {
    echo "❌ 디렉토리 이동 실패: ${APP_HOME}"
    exit 1
}

if [ -f "app/coffee-shout.pid" ]; then
    PID=$(cat app/coffee-shout.pid)

    if ps -p "$PID" > /dev/null 2>&1; then
        echo "   🛑 SIGTERM 신호 전송 (PID: $PID)"
        echo "   ℹ️  Spring Boot의 Graceful Shutdown이 시작됩니다"
        echo "   ℹ️  ApplicationStop 단계에서 종료 여부를 확인합니다"
        kill -SIGTERM "$PID"
    else
        echo "   ℹ️  애플리케이션이 이미 종료되어 있습니다"
        rm -f app/coffee-shout.pid
    fi
else
    echo "   ℹ️  PID 파일이 없습니다"
fi

echo "=== [BEFORE_BLOCK_TRAFFIC] 완료 ==="
