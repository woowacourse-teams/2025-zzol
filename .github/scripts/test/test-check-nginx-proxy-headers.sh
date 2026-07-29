#!/bin/bash

# ============================================
# nginx Proxy Header Check Test Suite
# ============================================
# check-nginx-proxy-headers.py의 판정을 검증한다. 임시 conf 파일만 쓰고 nginx는 띄우지 않는다:
#   - proxy_pass + include        → 0 (통과)
#   - proxy_pass, include 없음    → 1 (누락 탐지)
#   - 상위 server 블록의 include  → 0 (nginx 상속 반영)
#   - 예외 주석                   → 0 (사유 있는 예외 허용)
#   - 실제 저장소 conf            → 0 (현재 설정은 통과해야 함)
#
# Usage:
#   ./test-check-nginx-proxy-headers.sh
#
# Exit Codes:
#   0 - All tests passed
#   1 - One or more tests failed
# ============================================

# Note: set -e는 사용하지 않음 (테스트 실패 시에도 계속 실행해야 함)

RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'

TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECK_SCRIPT="${SCRIPT_DIR}/../check-nginx-proxy-headers.py"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

# ============================================
# 테스트 헬퍼
# ============================================

assert_conf() {
    local desc="$1"
    local expected="$2"
    local conf="$3"
    ((TESTS_TOTAL++))

    local file="${TMP_DIR}/case-${TESTS_TOTAL}.conf"
    printf '%s\n' "${conf}" > "${file}"

    local output
    output=$(python3 "${CHECK_SCRIPT}" "${file}" 2>&1)
    local actual=$?

    if [[ ${actual} -eq ${expected} ]]; then
        echo -e "${GREEN}✓${NC} ${desc}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗${NC} ${desc} (expected ${expected}, got ${actual})"
        echo "${output}" | sed 's/^/    /'
        ((TESTS_FAILED++))
    fi
}

# include 내용 검사는 파일명(proxy-http.inc·proxy-ws.inc)으로 대상을 고르므로 이름을 지정한다
assert_named() {
    local desc="$1"
    local expected="$2"
    local name="$3"
    local conf="$4"
    ((TESTS_TOTAL++))

    local dir="${TMP_DIR}/named-${TESTS_TOTAL}"
    mkdir -p "${dir}"
    printf '%s\n' "${conf}" > "${dir}/${name}"

    local output
    output=$(python3 "${CHECK_SCRIPT}" "${dir}/${name}" 2>&1)
    local actual=$?

    if [[ ${actual} -eq ${expected} ]]; then
        echo -e "${GREEN}✓${NC} ${desc}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗${NC} ${desc} (expected ${expected}, got ${actual})"
        echo "${output}" | sed 's/^/    /'
        ((TESTS_FAILED++))
    fi
}

assert_path() {
    local desc="$1"
    local expected="$2"
    local path="$3"
    ((TESTS_TOTAL++))

    local output
    output=$(python3 "${CHECK_SCRIPT}" "${path}" 2>&1)
    local actual=$?

    if [[ ${actual} -eq ${expected} ]]; then
        echo -e "${GREEN}✓${NC} ${desc}"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}✗${NC} ${desc} (expected ${expected}, got ${actual})"
        echo "${output}" | sed 's/^/    /'
        ((TESTS_FAILED++))
    fi
}

# ============================================
# 테스트 실행
# ============================================

echo "=== nginx 프록시 헤더 검사 테스트 ==="

assert_conf "location에 include가 있으면 통과" 0 'server {
    location / {
        proxy_pass $upstream;
        include /etc/nginx/conf.d/proxy-http.inc;
    }
}'

assert_conf "include 없는 proxy_pass를 탐지" 1 'server {
    location / {
        proxy_pass $upstream;
    }
}'

assert_conf "형제 location의 include는 커버하지 않는다" 1 'server {
    location /a {
        proxy_pass $upstream;
        include /etc/nginx/conf.d/proxy-http.inc;
    }
    location /b {
        proxy_pass $upstream;
    }
}'

assert_conf "상위 server의 include를 상속으로 인정" 0 'server {
    include /etc/nginx/conf.d/proxy-http.inc;
    location / {
        proxy_pass $upstream;
    }
}'

# nginx는 현재 레벨에 proxy_set_header가 있으면 상위 레벨을 통째로 버린다(누적 아님).
assert_conf "하위 블록의 자체 헤더 선언은 상위 상속을 끊는다" 1 'server {
    include /etc/nginx/conf.d/proxy-http.inc;
    location / {
        proxy_set_header X-Custom foo;
        proxy_pass $upstream;
    }
}'

assert_conf "XFF를 직접 선언하면 include 없이도 통과" 0 'server {
    location / {
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_pass $upstream;
    }
}'

assert_conf "한 줄에 여러 디렉티브인 proxy_pass도 탐지" 1 'server {
    location / { proxy_pass http://somewhere; }
}'

assert_conf "문자열 안의 예외 마커는 예외로 인정하지 않는다" 1 'server {
    location / {
        proxy_set_header X-Note "# proxy-header-exempt: nope";
        proxy_pass $upstream;
    }
}'

assert_conf "웹소켓 include도 인정" 0 'server {
    location /ws {
        proxy_pass $upstream;
        include /etc/nginx/conf.d/proxy-ws.inc;
    }
}'

assert_conf "사유 있는 예외 주석은 건너뛴다" 0 'server {
    location / {
        # proxy-header-exempt: 내부 전용, XFF 미사용
        proxy_pass $upstream;
    }
}'

assert_conf "proxy_pass 없는 블록은 통과" 0 'server {
    location / {
        return 404;
    }
}'

assert_conf "주석 처리된 proxy_pass는 무시" 0 'server {
    location / {
        # proxy_pass $upstream;
        return 404;
    }
}'

# ADR-0023이 기록한 실제 회귀: include는 있는데 그 안에서 XFF를 $remote_addr로 덮어썼다.
assert_named "XFF 체인을 덮어쓴 include를 탐지" 1 "proxy-http.inc" 'proxy_set_header Host $host;
proxy_set_header X-Forwarded-For $remote_addr;'

assert_named "체인을 누적하는 include는 통과" 0 "proxy-http.inc" 'proxy_set_header Host $host;
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;'

# 디렉터리 인자는 하위 디렉터리까지 훑어야 한다 — 검사 누락이 "통과"로 보이면 안 된다
mkdir -p "${TMP_DIR}/tree/sub"
cat > "${TMP_DIR}/tree/sub/deep.conf" <<'NGINX'
server {
    location / {
        proxy_pass $upstream;
    }
}
NGINX
assert_path "하위 디렉터리의 conf도 검사한다" 1 "${TMP_DIR}/tree"

assert_path "저장소의 실제 nginx conf는 통과" 0 "${REPO_ROOT}/backend/docker/nginx/conf"

# ============================================
# 결과
# ============================================

echo ""
echo "총 ${TESTS_TOTAL}건 / 성공 ${TESTS_PASSED} / 실패 ${TESTS_FAILED}"
[[ ${TESTS_FAILED} -eq 0 ]]
