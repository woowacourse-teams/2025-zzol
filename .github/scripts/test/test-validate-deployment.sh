#!/bin/bash

# ============================================
# Deployment Validator Test Suite
# ============================================
# validate-deployment.sh의 동작을 검증하는 테스트 스크립트
#
# Usage:
#   ./test-validate-deployment.sh
#
# Exit Codes:
#   0 - All tests passed
#   1 - One or more tests failed
# ============================================

# Note: set -e는 사용하지 않음 (테스트 실패 시에도 계속 실행해야 함)

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# 테스트 결과 카운터
TESTS_PASSED=0
TESTS_FAILED=0
TESTS_TOTAL=0

# 스크립트 경로
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
VALIDATOR_SCRIPT="${SCRIPT_DIR}/validate-deployment.sh"

# ============================================
# 테스트 헬퍼 함수
# ============================================

print_header() {
    echo ""
    echo "================================"
    echo "$1"
    echo "================================"
}

print_test() {
    echo ""
    echo "Test: $1"
}

assert_success() {
    local desc="$1"
    shift
    ((TESTS_TOTAL++))

    print_test "$desc"

    local exit_code=0
    output=$("$@" 2>&1) || exit_code=$?

    if [[ $exit_code -eq 0 ]]; then
        echo -e "${GREEN}✅ PASS${NC}: $desc"
        echo "   Output: $output"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ FAIL${NC}: $desc"
        echo "   Expected: Success (exit 0)"
        echo "   Got: Failure (exit $exit_code)"
        echo "   Output: $output"
        ((TESTS_FAILED++))
    fi
}

assert_failure() {
    local desc="$1"
    local expected_code="${2:-1}"
    shift 2
    ((TESTS_TOTAL++))

    print_test "$desc"

    local actual_code=0
    output=$("$@" 2>&1) || actual_code=$?

    if [[ $actual_code -eq 0 ]]; then
        echo -e "${RED}❌ FAIL${NC}: $desc"
        echo "   Expected: Failure (exit $expected_code)"
        echo "   Got: Success (exit 0)"
        echo "   Output: $output"
        ((TESTS_FAILED++))
    elif [[ $actual_code -eq $expected_code ]]; then
        echo -e "${GREEN}✅ PASS${NC}: $desc"
        echo "   Output: $output"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ FAIL${NC}: $desc"
        echo "   Expected: exit $expected_code"
        echo "   Got: exit $actual_code"
        echo "   Output: $output"
        ((TESTS_FAILED++))
    fi
}

assert_output_contains() {
    local desc="$1"
    local expected="$2"
    shift 2
    ((TESTS_TOTAL++))

    print_test "$desc"

    local exit_code=0
    output=$("$@" 2>&1) || exit_code=$?

    # 성공/실패 여부와 관계없이 출력에 특정 문자열이 포함되어 있는지 검증
    if echo "$output" | grep -qF "$expected"; then
        echo -e "${GREEN}✅ PASS${NC}: $desc"
        echo "   Output contains: '$expected'"
        ((TESTS_PASSED++))
    else
        echo -e "${RED}❌ FAIL${NC}: $desc"
        echo "   Expected output to contain: '$expected'"
        echo "   Got: $output"
        ((TESTS_FAILED++))
    fi
}

# ============================================
# 테스트 케이스
# ============================================

run_tests() {
    print_header "🧪 Testing validate-deployment.sh"

    # 스크립트 존재 확인
    if [[ ! -f "$VALIDATOR_SCRIPT" ]]; then
        echo -e "${RED}Error: Validator script not found: $VALIDATOR_SCRIPT${NC}"
        exit 1
    fi

    # 실행 권한 부여
    chmod +x "$VALIDATOR_SCRIPT"

    # ============================================
    # 1. 정상 케이스 - Push Event
    # ============================================
    print_header "Test Suite 1: Push Event (정상 케이스)"

    assert_success "push event with be/dev branch" \
        "$VALIDATOR_SCRIPT" push refs/heads/be/dev

    assert_output_contains "push event with be/dev outputs 'dev'" "dev" \
        "$VALIDATOR_SCRIPT" push refs/heads/be/dev

    assert_success "push event with prod branch" \
        "$VALIDATOR_SCRIPT" push refs/heads/prod

    assert_output_contains "push event with prod outputs 'prod'" "prod" \
        "$VALIDATOR_SCRIPT" push refs/heads/prod

    # ============================================
    # 2. 정상 케이스 - Workflow Dispatch
    # ============================================
    print_header "Test Suite 2: Workflow Dispatch (정상 케이스)"

    assert_success "workflow_dispatch with be/dev + dev env" \
        "$VALIDATOR_SCRIPT" workflow_dispatch refs/heads/be/dev dev

    assert_success "workflow_dispatch with prod + prod env" \
        "$VALIDATOR_SCRIPT" workflow_dispatch refs/heads/prod prod

    # ============================================
    # 3. 에러 케이스 - Push Event
    # ============================================
    print_header "Test Suite 3: Push Event (에러 케이스)"

    assert_failure "push event with unsupported branch (main)" 1 \
        "$VALIDATOR_SCRIPT" push refs/heads/main

    assert_failure "push event with unsupported branch (feature/test)" 1 \
        "$VALIDATOR_SCRIPT" push refs/heads/feature/test

    assert_failure "push event with invalid branch format" 1 \
        "$VALIDATOR_SCRIPT" push invalid-ref

    assert_failure "push event with legacy be/prod branch" 1 \
        "$VALIDATOR_SCRIPT" push refs/heads/be/prod

    # ============================================
    # 4. 에러 케이스 - Workflow Dispatch
    # ============================================
    print_header "Test Suite 4: Workflow Dispatch (에러 케이스)"

    assert_failure "workflow_dispatch with mismatched branch-env (dev branch + prod env)" 1 \
        "$VALIDATOR_SCRIPT" workflow_dispatch refs/heads/be/dev prod

    assert_failure "workflow_dispatch with mismatched branch-env (prod branch + dev env)" 1 \
        "$VALIDATOR_SCRIPT" workflow_dispatch refs/heads/prod dev

    assert_failure "workflow_dispatch without selected_env" 2 \
        "$VALIDATOR_SCRIPT" workflow_dispatch refs/heads/be/dev

    assert_failure "workflow_dispatch with invalid env" 1 \
        "$VALIDATOR_SCRIPT" workflow_dispatch refs/heads/be/dev staging

    # ============================================
    # 5. 에러 케이스 - Invalid Arguments
    # ============================================
    print_header "Test Suite 5: Invalid Arguments"

    assert_failure "missing all arguments" 2 \
        "$VALIDATOR_SCRIPT"

    assert_failure "missing branch_ref argument" 2 \
        "$VALIDATOR_SCRIPT" push

    assert_failure "unsupported event type" 2 \
        "$VALIDATOR_SCRIPT" pull_request refs/heads/be/dev

    # ============================================
    # 6. Edge Cases
    # ============================================
    print_header "Test Suite 6: Edge Cases"

    assert_failure "empty event_name" 2 \
        "$VALIDATOR_SCRIPT" "" refs/heads/be/dev

    assert_failure "empty branch_ref" 2 \
        "$VALIDATOR_SCRIPT" push ""

    assert_success "extra arguments are ignored" \
        "$VALIDATOR_SCRIPT" push refs/heads/be/dev extra args here

    # ============================================
    # 7. Output Validation
    # ============================================
    print_header "Test Suite 7: Output Validation"

    assert_output_contains "success message includes environment" "dev 환경 배포" \
        "$VALIDATOR_SCRIPT" push refs/heads/be/dev

    assert_output_contains "error message for unsupported branch" "지원하지 않는 브랜치" \
        "$VALIDATOR_SCRIPT" push refs/heads/main

    assert_output_contains "error message for branch-env mismatch" "브랜치에서만 배포 가능" \
        "$VALIDATOR_SCRIPT" workflow_dispatch refs/heads/be/dev prod
}

# ============================================
# 결과 출력
# ============================================

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
        echo ""
        return 0
    else
        echo -e "${RED}❌ Some tests failed!${NC}"
        echo ""
        return 1
    fi
}

# ============================================
# 메인 실행
# ============================================

main() {
    echo "🧪 Deployment Validator Test Suite"
    echo "Script: $VALIDATOR_SCRIPT"

    run_tests

    if print_summary; then
        exit 0
    else
        exit 1
    fi
}

main "$@"
