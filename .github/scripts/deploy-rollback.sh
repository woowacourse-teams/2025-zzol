#!/bin/bash
set -e

# ============================================
# Deploy Rollback - Blue-Green Strategy
# ============================================
# 현재 active가 아닌 색상의 이미지(.env 기반)로 롤백합니다.
#
# 상태 파악:
#   - 현재 active 색상: {env}-service.inc 에서 읽음
#   - 롤백 대상 이미지: .env 의 INACTIVE_COLOR_IMAGE_TAG 에서 읽음
#
# 흐름:
#   1. {env}-service.inc → current color (실패한 쪽)
#   2. .env → inactive color 의 이미지 태그 (이전 버전)
#   3. inactive 컨테이너 기동 + readiness 확인
#   4. {env}-service.inc 교체 → nginx reload (트래픽 복원)
#   5. 실패한 컨테이너 중지
#
# Usage:
#   ./deploy-rollback.sh <environment> <deploy_dir>
#
# Arguments:
#   environment - dev, prod
#   deploy_dir  - docker-compose.yml 및 .env가 있는 디렉토리
#
# Exit Codes:
#   0 - Success
#   1 - Error
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-utils.sh"

if [[ $# -lt 2 ]]; then
    log_error "Usage: $0 <environment> <deploy_dir> [target_image_tag]"
    log_error "Example: $0 prod ~/prod"
    log_error "Example: $0 prod ~/prod prod-abc001  # 특정 태그로 롤백"
    exit 1
fi

ENVIRONMENT="$1"
DEPLOY_DIR="$2"
TARGET_TAG="${3:-}"  # 선택: 지정 시 해당 태그로 롤백, 미지정 시 .env 의 이전 태그 사용

if ! validate_environment "$ENVIRONMENT"; then
    exit 1
fi

if [[ ! -d "$DEPLOY_DIR" ]]; then
    log_error "Deploy directory not found: $DEPLOY_DIR"
    exit 1
fi

if ! require_file "${DEPLOY_DIR}/.env"; then
    log_error ".env file missing in ${DEPLOY_DIR}"
    exit 1
fi

main() {
    print_script_info "Blue-Green Rollback" \
        "Environment: $ENVIRONMENT"

    cd "$DEPLOY_DIR"

    # 1. {env}-service.inc 에서 현재 active 색상 확인
    local current_color
    current_color=$(get_active_color "$ENVIRONMENT")

    if [[ -z "$current_color" ]]; then
        log_error "Cannot determine active color: ${NGINX_CONF_BASE}/${ENVIRONMENT}-service.inc not found"
        log_error "No deployment to roll back."
        exit 1
    fi

    local rollback_color
    rollback_color=$(get_inactive_color "$current_color")

    local current_container="${ENVIRONMENT}-app-${current_color}"
    local rollback_container="${ENVIRONMENT}-app-${rollback_color}"

    log_info "Current (failed): $current_color ($current_container)"
    log_info "Rollback target:  $rollback_color ($rollback_container)"

    # 2. rollback 태그 결정
    #    - TARGET_TAG 인자가 있으면 .env 에 직접 기록 후 사용
    #    - 없으면 .env 의 기존 rollback_color 태그 사용
    local rollback_tag
    if [[ -n "$TARGET_TAG" ]]; then
        log_info "Using specified tag: $TARGET_TAG"
        update_env_image_tag ".env" "$rollback_color" "$TARGET_TAG"
        rollback_tag="$TARGET_TAG"
    else
        rollback_tag=$(get_env_image_tag ".env" "$rollback_color")
        if [[ -z "$rollback_tag" || "$rollback_tag" == "placeholder" ]]; then
            log_error "No valid image tag for $rollback_color in .env"
            log_error "Hint: pass a specific tag as 3rd argument, or check .env"
            exit 1
        fi
    fi

    log_info "Rollback image tag: $rollback_tag"

    # 3. rollback 컨테이너 기동
    log_step "Starting $rollback_container (tag: $rollback_tag)"
    docker compose --env-file .env up -d "${ENVIRONMENT}-app-${rollback_color}"

    # 4. Readiness 확인
    log_step "Waiting for $rollback_container to be Ready"
    if ! wait_for_app_ready "$rollback_container"; then
        log_error "$rollback_container failed readiness check during rollback"
        docker compose --env-file .env logs --tail=50 "${ENVIRONMENT}-app-${rollback_color}"
        docker stop "$rollback_container" 2>/dev/null || true
        exit 1
    fi

    # 5. {env}-service.inc 교체 → nginx reload (트래픽 복원)
    log_step "Restoring Nginx Upstream → $rollback_container"
    if ! switch_nginx_upstream "$rollback_container" "$ENVIRONMENT"; then
        log_error "Failed to restore nginx upstream"
         log_info "Attempting to keep upstream on $current_container"
          switch_nginx_upstream "$current_container" "$ENVIRONMENT" \
                  || log_warning "Upstream restore also failed — check nginx manually"
        docker stop "$rollback_container" 2>/dev/null || true
        exit 1
    fi

    # 6. 실패한 컨테이너 중지
    log_step "Stopping Failed Container: $current_container"
    docker stop "$current_container" --timeout 30 \
        || log_warning "Failed to stop $current_container"

    log_step "Rollback Completed"
    log_success "Active:  $rollback_color ($rollback_container) → $rollback_tag"
    log_success "Stopped: $current_color ($current_container)"
}

main "$@"
