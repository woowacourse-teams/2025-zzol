#!/bin/bash
set -euo pipefail
export PATH="/usr/bin:/bin:$PATH"

# 애플리케이션 홈 디렉토리
APP_HOME=/opt/coffee-shout

echo "=== [BEFORE_INSTALL] 커피빵 게임 서버 배포 준비 ==="

# 기존 애플리케이션 안전하게 종료
if pgrep -f "coffee-shout-backend.jar" > /dev/null; then
    echo "☕ 기존 애플리케이션을 안전하게 종료합니다..."
    pkill -SIGTERM -f "coffee-shout-backend.jar" || true

    # Graceful shutdown 대기 (최대 10초)
    for i in {1..10}; do
        if ! pgrep -f "coffee-shout-backend.jar" > /dev/null; then
            echo "   ✅ 기존 애플리케이션 종료 완료 (${i}초 소요)"
            break
        fi
        sleep 1
    done

    # 여전히 실행 중이면 강제 종료
    if pgrep -f "coffee-shout-backend.jar" > /dev/null; then
        echo "   🔨 강제 종료를 진행합니다..."
        pkill -SIGKILL -f "coffee-shout-backend.jar" || true
        sleep 2
        echo "   ✅ 기존 애플리케이션 강제 종료 완료"
    fi
else
    echo "☕ 실행 중인 애플리케이션이 없습니다"
fi

# PID 파일 정리
rm -f "${APP_HOME}/app/coffee-shout.pid" 2>/dev/null || true

# 배포 디렉토리 생성 및 정리
echo "📁 배포 디렉토리 생성 및 권한 설정..."
mkdir -p "${APP_HOME}"/{app,scripts,logs}
chown -R ubuntu:ubuntu "${APP_HOME}"

# jq 설치 확인 및 설치
if ! command -v jq &> /dev/null; then
    echo "🔧 jq가 설치되어 있지 않습니다. 설치를 시작합니다..."
    if yum install -y jq &>/dev/null 2>&1; then
        echo "✅ jq 설치 완료 (yum)"
    elif apt-get install -y jq &>/dev/null 2>&1; then
        echo "✅ jq 설치 완료 (apt-get)"
    else
        echo "⚠️  jq 설치 실패. JSON 파싱 없이 계속 진행합니다."
    fi
else
    echo "✅ jq가 이미 설치되어 있습니다"
fi

# 기존 JAR 파일 삭제 (새 인스턴스 대응)
if [ -f "${APP_HOME}/app/coffee-shout-backend.jar" ]; then
    echo "🗑️  기존 JAR 파일 삭제..."
    rm -f "${APP_HOME}/app/coffee-shout-backend.jar"
fi

echo "=== [BEFORE_INSTALL] 완료 ==="
