#!/bin/bash
set -euo pipefail
export PATH="/usr/bin:/bin:$PATH"

# 애플리케이션 홈 디렉토리
APP_HOME=/opt/coffee-shout

echo "=== [APPLICATION_STOP] 애플리케이션 종료 ==="

cd "${APP_HOME}" || {
    echo "❌ 디렉토리 이동 실패: ${APP_HOME}"
    exit 1
}

# ==========================================
# Dev 환경: 단순 종료
# 로드 밸런서가 없으므로 빠른 종료 수행
# ==========================================

echo ""
echo "☕ 1. Spring Boot 애플리케이션 종료 중..."

if [ -f "app/coffee-shout.pid" ]; then
    PID=$(cat app/coffee-shout.pid)

    if ps -p "$PID" > /dev/null 2>&1; then
        echo "   🛑 SIGTERM 신호 전송 (PID: $PID)"
        kill -SIGTERM "$PID"

        # 최대 30초 대기
        echo "   ⏳ Graceful Shutdown 대기 중... (최대 30초)"
        for i in {1..30}; do
            if ! ps -p "$PID" > /dev/null 2>&1; then
                echo "   ✅ 애플리케이션이 정상 종료되었습니다 (${i}초 소요)"
                break
            fi
            sleep 1
        done

        # 여전히 실행 중이면 강제 종료
        if ps -p "$PID" > /dev/null 2>&1; then
            echo "   ⚠️  30초 내에 종료되지 않았습니다"
            echo "   🔨 강제 종료를 수행합니다 (SIGKILL)"
            kill -9 "$PID" 2>/dev/null || true
            sleep 2
            echo "   ✅ 프로세스를 강제 종료했습니다"
        fi
    else
        echo "   ℹ️  애플리케이션이 이미 종료되어 있습니다"
    fi

    # PID 파일 제거
    rm -f app/coffee-shout.pid
else
    echo "   ℹ️  PID 파일이 없습니다"
fi

# 포트 8080 사용 프로세스 강제 종료 (혹시 모를 좀비 프로세스)
# ss 명령어로 포트 확인 (lsof 대체)
JAVA_PROCESS=$(ss -tlnp 2>/dev/null | awk '/:8080/ {match($0, /pid=([0-9]+)/, arr); print arr[1]; exit}' || true)
if [ -n "$JAVA_PROCESS" ]; then
    echo "   🔫 포트 8080을 사용하는 좀비 프로세스 강제 종료 (PID: $JAVA_PROCESS)"
    kill -9 "$JAVA_PROCESS" 2>/dev/null || true
    sleep 1
fi

echo ""
echo "=== [APPLICATION_STOP] 완료 ==="