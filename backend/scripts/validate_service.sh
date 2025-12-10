#!/bin/bash
set -euo pipefail
export PATH="/usr/bin:/bin:$PATH"

# 애플리케이션 홈 디렉토리
APP_HOME=/opt/coffee-shout

echo "=== [VALIDATE_SERVICE] 서비스 상태 검증 ==="

cd "${APP_HOME}" || {
    echo "❌ 디렉토리 이동 실패: ${APP_HOME}"
    exit 1
}

# 헬스체크 (Spring Boot Actuator)
health_check() {
    local max_attempts=30
    local attempt=1

    while [ "$attempt" -le "$max_attempts" ]; do
        # Spring Boot Actuator 헬스체크 엔드포인트 확인
        RESPONSE=$(curl -s -w "\n%{http_code}" --max-time 5 http://localhost:8080/actuator/health 2>/dev/null || echo "")
        HTTP_CODE=$(echo "$RESPONSE" | tail -1)
        RESPONSE_BODY=$(echo "$RESPONSE" | sed '$d')

        if [ "$HTTP_CODE" = "200" ]; then
            echo "✅ 서버 헬스체크 성공 (시도: $attempt/$max_attempts)"

            # jq를 사용한 안전한 파싱
            if command -v jq &> /dev/null; then
                HEALTH_STATUS=$(echo "$RESPONSE_BODY" | jq -r '.status // "UNKNOWN"' 2>/dev/null || echo "UNKNOWN")
                if [ "$HEALTH_STATUS" = "UP" ]; then
                    echo "✅ 애플리케이션 상태: UP"
                    return 0
                else
                    echo "⚠️  애플리케이션 상태: $HEALTH_STATUS (재시도...)"
                fi
            else
                echo "✅ 애플리케이션 상태: HTTP 200 (jq 미설치)"
                return 0
            fi
        elif [ "$HTTP_CODE" = "503" ]; then
            echo "⏳ 서버가 시작 중입니다 (HTTP 503)... (시도: $attempt/$max_attempts)"
        else
            echo "⏳ 서버 응답 대기 중 (HTTP $HTTP_CODE)... (시도: $attempt/$max_attempts)"
        fi

        sleep 2
        attempt=$((attempt + 1))
    done

    echo "❌ 서버 헬스체크 실패 (최대 시도 횟수 초과)"
    return 1
}

if health_check; then
    echo "🎉 커피빵 게임 서버 배포 완료!"
    echo ""
    echo "=== 서비스 정보 ==="
    echo "포트: 8080"
    echo "프로파일: ${SPRING_PROFILES_ACTIVE:-unknown}"
    if [ -f "app/coffee-shout.pid" ]; then
        PID=$(cat app/coffee-shout.pid)
        echo "PID: $PID"
    fi
    echo ""
    echo "=== 프로세스 정보 ==="
    pgrep -fa coffee-shout-backend.jar || echo "프로세스 정보를 찾을 수 없습니다"
else
    echo "💥 헬스체크 실패!"
    exit 1
fi

echo "=== [VALIDATE_SERVICE] 완료 ==="
