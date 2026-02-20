#!/bin/bash
set -e

# ============================================
# Deploy Application - Blue-Green Strategy
# ============================================
# Blue-Green 무중단 배포를 수행합니다.
#
# 상태 관리:
#   - 트래픽 기준: ~/nginx/conf/{env}-service.inc
#   - 이미지 기준: {deploy_dir}/.env 의 BLUE/GREEN_IMAGE_TAG
#
# 흐름:
#   1. {env}-service.inc 읽어 current/target 색상 결정
#   2. .env 의 TARGET_COLOR_IMAGE_TAG를 새 태그로 업데이트
#   3. docker compose up -d (--env-file .env 로 이미지 결정)
#   4. readiness 확인
#   5. {env}-service.inc 교체 → nginx reload (트래픽 전환)
#   6. 구버전 컨테이너 graceful stop (300초)
#
# Usage:
#   ./deploy-application.sh <environment> <deploy_dir> <new_image_tag>
#
# Arguments:
#   environment    - dev, prod
#   deploy_dir     - docker-compose.yml 및 .env가 있는 디렉토리
#   new_image_tag  - 배포할 이미지 태그 (GitHub Actions에서 전달)
#
# Exit Codes:
#   0 - Success
#   1 - Error
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-utils.sh"

# ============================================
# 파라미터 검증
# ============================================

if [[ $# -lt 3 ]]; then
    log_error "Usage: $0 <environment> <deploy_dir> <new_image_tag>"
    log_error "Example: $0 prod ~/prod prod-abc123"
    exit 1
fi

ENVIRONMENT="$1"
DEPLOY_DIR="$2"
NEW_TAG="$3"

if ! validate_environment "$ENVIRONMENT"; then
    exit 1
fi

if [[ ! -d "$DEPLOY_DIR" ]]; then
    log_error "Deploy directory not found: $DEPLOY_DIR"
    exit 1
fi

if ! require_file "${DEPLOY_DIR}/docker-compose.yml"; then
    exit 1
fi

if ! require_file "${DEPLOY_DIR}/.env"; then
    log_error ".env file missing in ${DEPLOY_DIR}"
    exit 1
fi

# ============================================
# 의존성 확인
# ============================================

verify_dependencies() {
    log_info "Verifying infrastructure dependencies..."

    local mysql_service="${ENVIRONMENT}-mysql"
    if ! check_container_healthy "$mysql_service"; then
        log_error "MySQL is not healthy: $mysql_service"
        return 1
    fi
    log_success "MySQL is healthy: $mysql_service"

    local redis_service="${ENVIRONMENT}-redis"
    if ! check_container_running "$redis_service"; then
        log_error "Redis is not running: $redis_service"
        return 1
    fi
    log_success "Redis is running: $redis_service"

    return 0
}

# ============================================
# Bootstrap: 최초 배포 (blue로 시작)
# ============================================

bootstrap_deployment() {
    log_step "Bootstrap: First Blue-Green Deployment (blue)"

    local blue_container="${ENVIRONMENT}-app-blue"

    if ! verify_dependencies; then
        log_error "Dependency verification failed"
        exit 1
    fi

    # .env에 BLUE_IMAGE_TAG 기록
    update_env_image_tag "${DEPLOY_DIR}/.env" "blue" "$NEW_TAG"

    log_info "Pulling image: $NEW_TAG"
    docker compose --env-file .env pull "${ENVIRONMENT}-app-blue"

    log_info "Starting $blue_container..."
    docker compose --env-file .env up -d "${ENVIRONMENT}-app-blue"

    if ! wait_for_app_ready "$blue_container"; then
        log_error "Bootstrap readiness check failed"
        docker compose --env-file .env logs --tail=50 "${ENVIRONMENT}-app-blue"
        docker stop "$blue_container" 2>/dev/null || true
        exit 1
    fi

    # {env}-service.inc 생성 → nginx 트래픽 연결
    if ! switch_nginx_upstream "$blue_container" "$ENVIRONMENT"; then
        log_error "Failed to set initial nginx upstream"
        docker stop "$blue_container" 2>/dev/null || true
        exit 1
    fi

    log_step "Bootstrap Completed"
    log_success "Active: blue ($blue_container) → $NEW_TAG"
}

# ============================================
# Blue-Green 배포
# ============================================

deploy_blue_green() {
    local current_color="$1"
    local target_color
    target_color=$(get_inactive_color "$current_color")

    local current_container="${ENVIRONMENT}-app-${current_color}"
    local target_container="${ENVIRONMENT}-app-${target_color}"

    log_info "Current: $current_color ($current_container)"
    log_info "Target:  $target_color ($target_container) → $NEW_TAG"

    if ! verify_dependencies; then
        log_error "Dependency verification failed"
        exit 1
    fi

    # 1. .env에 target 색상의 이미지 태그 기록
    update_env_image_tag "${DEPLOY_DIR}/.env" "$target_color" "$NEW_TAG"

    # 2. 이미지 pull
    log_step "Pulling Image: $NEW_TAG"
    docker compose --env-file .env pull "${ENVIRONMENT}-app-${target_color}"

    # 3. target 컨테이너 기동
    log_step "Starting $target_container"
    docker compose --env-file .env up -d "${ENVIRONMENT}-app-${target_color}"

    # 4. Readiness 확인
    log_step "Waiting for $target_container to be Ready"
    if ! wait_for_app_ready "$target_container"; then
        log_error "$target_container failed readiness check"
        docker compose --env-file .env logs --tail=50 "${ENVIRONMENT}-app-${target_color}"
        log_info "Stopping failed container: $target_container"
        docker stop "$target_container" 2>/dev/null || true
        exit 1
    fi

    # 5. {env}-service.inc 교체 → nginx reload (트래픽 전환)
    log_step "Switching Nginx Upstream → $target_container"
    if ! switch_nginx_upstream "$target_container" "$ENVIRONMENT"; then
        log_error "Failed to switch nginx upstream"
        log_info "Restoring upstream to $current_container"
        switch_nginx_upstream "$current_container" "$ENVIRONMENT" \
            || log_warning "Upstream restore also failed — check nginx manually"
        docker stop "$target_container" 2>/dev/null || true
        exit 1
    fi

    # 6. 구버전 graceful shutdown (300초 = 5분, 게임 1라운드 기준)
    log_step "Graceful Shutdown: $current_container (timeout: 300s)"
    docker stop "$current_container" --time 300 \
        || log_warning "Graceful stop of $current_container timed out or failed"

    log_step "Deployment Completed"
    log_success "Active:   $target_color ($target_container) → $NEW_TAG"
    log_success "Stopped:  $current_color ($current_container)"
}

# ============================================
# 메인
# ============================================

main() {
    print_script_info "Blue-Green Deploy Application" \
        "Environment: $ENVIRONMENT | Tag: $NEW_TAG"

    cd "$DEPLOY_DIR"

    # {env}-service.inc 로 현재 색상 확인
    local current_color
    current_color=$(get_active_color "$ENVIRONMENT")

    # Bootstrap 조건: service.inc 없거나 active 컨테이너 미실행
    if [[ -z "$current_color" ]] || ! check_container_running "${ENVIRONMENT}-app-${current_color}"; then
        if [[ -n "$current_color" ]]; then
            log_warning "${ENVIRONMENT}-app-${current_color} is not running. Re-bootstrapping with blue."
        fi
        bootstrap_deployment
        exit 0
    fi

    deploy_blue_green "$current_color"
}

main "$@"
