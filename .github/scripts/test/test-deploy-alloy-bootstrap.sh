#!/bin/bash

# ============================================
# Alloy Bootstrap Classifier Test Suite
# ============================================
# deploy-utils.sh의 classify_alloy_bootstrap() 동작을 검증한다.
# docker 호출 없이 임시 파일/디렉터리만으로 3가지 상태를 확인한다:
#   - 일반 파일 존재 → 0 (정상)
#   - 디렉터리(Docker auto-create 오염) → 2
#   - 누락 → 3
#
# Usage:
#   ./test-deploy-alloy-bootstrap.sh
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
UTILS_SCRIPT="${SCRIPT_DIR}/../deploy-utils.sh"

# ============================================
# 테스트 헬퍼
# ============================================

assert_exit_code() {
    local desc="$1"
    local expected="$2"
    local path="$3"
    ((TESTS_TOTAL++))

    classify_alloy_bootstrap "$path"
    local actual=$?

    if [[ "$actual" -eq "$expected" ]]; then
        echo -e "${GREEN}✅ PASS${NC}: $desc (exit $actual)"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ FAIL${NC}: $desc"
        echo "   Expected exit: $expected"
        echo "   Got exit:      $actual"
        ((TESTS_FAILED++))
    fi
}

# ============================================
# 테스트 실행
# ============================================

run_tests() {
    echo ""
    echo "================================"
    echo "🧪 Testing classify_alloy_bootstrap"
    echo "================================"

    if [[ ! -f "$UTILS_SCRIPT" ]]; then
        echo -e "${RED}Error: utils script not found: $UTILS_SCRIPT${NC}"
        exit 1
    fi

    # shellcheck disable=SC1090
    source "$UTILS_SCRIPT"

    # 임시 작업공간
    local tmp_dir
    tmp_dir="$(mktemp -d)"
    trap 'rm -rf "$tmp_dir"' EXIT

    # 픽스처 준비
    local file_path="${tmp_dir}/config.alloy.file"
    local dir_path="${tmp_dir}/config.alloy.dir"
    local missing_path="${tmp_dir}/config.alloy.missing"
    echo "// bootstrap" > "$file_path"
    mkdir -p "$dir_path"
    # missing_path는 생성하지 않는다

    assert_exit_code "일반 파일 존재 → 0" 0 "$file_path"
    assert_exit_code "디렉터리(오염) → 2" 2 "$dir_path"
    assert_exit_code "누락 → 3" 3 "$missing_path"
}

print_summary() {
    echo ""
    echo "================================"
    echo "📊 Test Summary"
    echo "================================"
    echo "Total Tests:  $TESTS_TOTAL"
    echo -e "Passed:       ${GREEN}$TESTS_PASSED${NC}"
    echo -e "Failed:       ${RED}$TESTS_FAILED${NC}"
    echo ""

    if [[ $TESTS_FAILED -eq 0 ]]; then
        echo -e "${GREEN}✅ All tests passed!${NC}"
        return 0
    else
        echo -e "${RED}❌ Some tests failed${NC}"
        return 1
    fi
}

run_tests
print_summary
