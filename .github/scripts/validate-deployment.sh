#!/bin/bash
set -e

# ============================================
# Deployment Environment Validator
# ============================================
# 브랜치와 환경의 일치성을 검증하고 배포 환경을 결정합니다.
#
# Usage:
#   ./validate-deployment.sh <event_name> <branch_ref> [selected_env]
#
# Arguments:
#   event_name    - GitHub event name (push, workflow_dispatch)
#   branch_ref    - Git reference (refs/heads/be/dev)
#   selected_env  - (Optional) User selected environment for workflow_dispatch
#
# Exit Codes:
#   0 - Success (환경 결정 완료)
#   1 - Validation Error (브랜치-환경 불일치)
#   2 - Invalid Arguments
#
# Examples:
#   ./validate-deployment.sh push refs/heads/be/dev
#   ./validate-deployment.sh workflow_dispatch refs/heads/be/prod prod
# ============================================

# 브랜치-환경 매핑 (중앙 집중식 관리)
# 새로운 환경 추가 시 여기에만 추가하면 됩니다
declare -A BRANCH_ENV_MAP=(
    ["refs/heads/be/dev"]="dev"
    ["refs/heads/be/prod"]="prod"
)

# 입력 파라미터
EVENT_NAME="${1}"
BRANCH_REF="${2}"
SELECTED_ENV="${3:-}"

# ============================================
# 유틸리티 함수
# ============================================

log_info() {
    echo "ℹ️  $*"
}

log_success() {
    echo "✅ $*"
}

log_error() {
    echo "❌ $*" >&2
}

get_branch_name() {
    local ref="$1"
    echo "${ref#refs/heads/}"
}

# ============================================
# 검증 함수
# ============================================

validate_arguments() {
    if [[ -z "$EVENT_NAME" ]] || [[ -z "$BRANCH_REF" ]]; then
        log_error "Error: Missing required arguments"
        echo "Usage: $0 <event_name> <branch_ref> [selected_env]" >&2
        echo "" >&2
        echo "Arguments:" >&2
        echo "  event_name    - push, workflow_dispatch" >&2
        echo "  branch_ref    - refs/heads/be/dev, refs/heads/be/prod" >&2
        echo "  selected_env  - dev, prod (required for workflow_dispatch)" >&2
        return 1
    fi
    return 0
}

validate_branch_env_match() {
    local env="$1"
    local branch="$2"
    local expected_branch=""

    # 환경에 맞는 브랜치 찾기
    for key in "${!BRANCH_ENV_MAP[@]}"; do
        if [[ "${BRANCH_ENV_MAP[$key]}" == "$env" ]]; then
            expected_branch="$key"
            break
        fi
    done

    if [[ -z "$expected_branch" ]]; then
        log_error "Error: Unknown environment: $env"
        log_error "Supported environments: ${BRANCH_ENV_MAP[*]}"
        return 1
    fi

    if [[ "$branch" != "$expected_branch" ]]; then
        log_error "Error: ${env} 환경은 $(get_branch_name "$expected_branch") 브랜치에서만 배포 가능합니다!"
        log_error "현재 브랜치: $(get_branch_name "$branch")"
        log_error "필요 브랜치: $(get_branch_name "$expected_branch")"
        return 1
    fi

    return 0
}

get_env_from_branch() {
    local branch="$1"

    if [[ -v BRANCH_ENV_MAP[$branch] ]]; then
        echo "${BRANCH_ENV_MAP[$branch]}"
        return 0
    else
        log_error "Error: 지원하지 않는 브랜치입니다!"
        log_error "현재 브랜치: $(get_branch_name "$branch")"
        # 지원 브랜치 목록 출력
        local supported_branches=""
        for key in "${!BRANCH_ENV_MAP[@]}"; do
            supported_branches+="$(get_branch_name "$key") "
        done
        log_error "지원 브랜치: ${supported_branches}"
        return 1
    fi
}

# ============================================
# 메인 로직
# ============================================

main() {
    local ENVIRONMENT=""

    # 파라미터 검증
    if ! validate_arguments; then
        exit 2
    fi

    case "$EVENT_NAME" in
        workflow_dispatch)
            # 수동 실행: 브랜치-환경 검증
            if [[ -z "$SELECTED_ENV" ]]; then
                log_error "Error: workflow_dispatch requires selected_env argument"
                log_error "Usage: $0 workflow_dispatch <branch_ref> <selected_env>"
                exit 2
            fi

            log_info "검증 중: 브랜치=$(get_branch_name "$BRANCH_REF"), 선택환경=${SELECTED_ENV}"

            if validate_branch_env_match "$SELECTED_ENV" "$BRANCH_REF"; then
                ENVIRONMENT="$SELECTED_ENV"
                log_success "검증 완료: ${ENVIRONMENT} 환경 배포 진행"
            else
                exit 1
            fi
            ;;

        push)
            # 자동 실행: 브랜치에서 환경 추출
            log_info "브랜치 감지: $(get_branch_name "$BRANCH_REF")"

            if ENVIRONMENT=$(get_env_from_branch "$BRANCH_REF"); then
                log_success "$(get_branch_name "$BRANCH_REF") 브랜치 감지: ${ENVIRONMENT} 환경 배포"
            else
                exit 1
            fi
            ;;

        *)
            log_error "Error: Unsupported event type: $EVENT_NAME"
            log_error "Supported events: push, workflow_dispatch"
            exit 2
            ;;
    esac

    # GitHub Actions 환경변수 설정 (옵션)
    if [[ -n "${GITHUB_ENV:-}" ]]; then
        echo "ENVIRONMENT=${ENVIRONMENT}" >> "$GITHUB_ENV"
        log_info "GITHUB_ENV updated: ENVIRONMENT=${ENVIRONMENT}"
    fi

    # 결과 출력
    log_success "Deploying to ${ENVIRONMENT} environment from branch $(get_branch_name "$BRANCH_REF")"

    # stdout로 환경 반환 (워크플로우에서 캡처 가능)
    echo "${ENVIRONMENT}"

    exit 0
}

# 스크립트 실행
main "$@"
