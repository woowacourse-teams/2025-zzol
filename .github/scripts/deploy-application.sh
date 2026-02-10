#!/bin/bash
set -e

# ============================================
# Deploy Application (Spring Boot)
# ============================================
# docker-compose.yml에 정의된 모든 서비스를 배포합니다.
# - Application 이미지를 pull하고 재시작
# - Exporter 등 다른 서비스는 없으면 시작, 변경사항이 있으면 재시작
# - 변경사항이 없는 서비스(MySQL, Redis 등)는 그대로 유지
#
# Usage:
#   ./deploy-application.sh <environment> <deploy_dir>
#
# Arguments:
#   environment - dev, prod
#   deploy_dir  - docker-compose.yml이 있는 디렉토리
#
# Note:
#   이 스크립트를 실행하기 전에 .env 파일이 deploy_dir에 존재해야 합니다.
#
# Exit Codes:
#   0 - Success
#   1 - Error
#
# Examples:
#   ./deploy-application.sh dev ~/dev
#   ./deploy-application.sh prod ~/prod
# ============================================

# 스크립트 디렉토리
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 공통 유틸리티 로드
source "${SCRIPT_DIR}/deploy-utils.sh"

# ============================================
# 파라미터 검증
# ============================================

if [[ $# -lt 2 ]]; then
    log_error "Usage: $0 <environment> <deploy_dir>"
    log_error "Example: $0 dev ~/dev"
    exit 1
fi

ENVIRONMENT="$1"
DEPLOY_DIR="$2"

# 환경 검증
if ! validate_environment "$ENVIRONMENT"; then
    exit 1
fi

# 디렉토리 검증
if [[ ! -d "$DEPLOY_DIR" ]]; then
    log_error "Deploy directory not found: $DEPLOY_DIR"
    exit 1
fi

# docker-compose.yml 검증
if ! require_file "${DEPLOY_DIR}/docker-compose.yml"; then
    exit 1
fi

# .env 파일 존재 여부 확인
if ! require_file "${DEPLOY_DIR}/.env"; then
    log_error ".env file missing in ${DEPLOY_DIR}"
    exit 1
fi

# ============================================
# 변수 설정
# ============================================

SERVICE_NAME="${ENVIRONMENT}-app"

# ============================================
# 메인 함수
# ============================================

pull_application_image() {
    log_step "🐳 Application Image Pull"

    if docker compose --env-file .env pull "$SERVICE_NAME"; then
        log_success "Image pull completed"
    else
        log_error "Failed to pull image for service: $SERVICE_NAME"
        return 1
    fi

    return 0
}

verify_dependencies() {
    log_info "Verifying infrastructure dependencies..."

    # MySQL 확인
    local mysql_service="${ENVIRONMENT}-mysql"
    if ! check_container_healthy "$mysql_service"; then
        log_error "MySQL is not healthy: $mysql_service"
        log_error "Cannot deploy application without healthy database"
        return 1
    fi
    log_success "MySQL is healthy: $mysql_service"

    # Redis 확인
    local redis_service="${ENVIRONMENT}-redis"
    if ! check_container_running "$redis_service"; then
        log_error "Redis is not running: $redis_service"
        log_error "Cannot deploy application without Redis"
        return 1
    fi
    log_success "Redis is running: $redis_service"

    return 0
}

deploy_application() {
    log_step "🚢 Application Deployment"

    log_info "Deploying all services with docker-compose up -d"
    log_info "Note: Only changed services will be restarted"

    # 전체 서비스 up (변경사항이 있는 서비스만 재시작됨)
    # - 이미 실행 중인 MySQL, Redis는 변경사항이 없으면 그대로 유지
    # - Application은 새 이미지로 재시작
    # - Exporter는 없으면 시작, 있으면 변경사항 확인 후 처리
    docker compose --env-file .env up -d

    # 컨테이너 시작 대기
    sleep 5

    # 애플리케이션 컨테이너 ID 확인
    local new_container_id
    new_container_id=$(docker ps -q -f name="^${SERVICE_NAME}$" 2>/dev/null) || true

    if [[ -z "$new_container_id" ]]; then
        log_error "Application container failed to start: $SERVICE_NAME"
        docker compose --env-file .env logs --tail=50 "$SERVICE_NAME"
        return 1
    fi

    log_success "Application container started: $new_container_id"

    # 배포된 모든 서비스 상태 확인
    echo ""
    log_info "All services status:"
    docker compose --env-file .env ps

    return 0
}

health_check() {
    log_step "🏥 Application Health Check"

    local max_attempts=150
    local interval=1

    log_info "Waiting for application to be healthy (max ${max_attempts}s)..."

    for i in $(seq 1 "$max_attempts"); do
        # /actuator/health 엔드포인트 체크 (wget: Alpine 기본 내장)
        if docker exec "$SERVICE_NAME" wget --quiet --spider http://localhost:8080/actuator/health 2>/dev/null; then
            log_success "Application is healthy!"
            return 0
        fi

        log_info "Attempt $i/$max_attempts: Application not ready yet..."
        sleep "$interval"
    done

    log_error "Health check failed after ${max_attempts} attempts"

    # 실패 시 로그 출력
    echo ""
    log_error "Recent application logs:"
    docker compose --env-file .env logs --tail=100 "$SERVICE_NAME"

    return 1
}

show_deployment_status() {
    log_step "📊 Deployment Status"

    # 컨테이너 상태
    echo ""
    log_info "Container status:"
    docker compose --env-file .env ps "$SERVICE_NAME"

    # 최근 로그
    echo ""
    log_info "Recent application logs:"
    docker compose --env-file .env logs --tail=30 "$SERVICE_NAME"

    # 이미지 정보
    echo ""
    log_info "Deployed image:"
    docker inspect "$SERVICE_NAME" --format='{{.Config.Image}}' 2>/dev/null || log_info "Unable to retrieve image information"
}

attempt_rollback() {
    log_step "🔄 Attempting Automatic Rollback"

    if [[ -x "${DEPLOY_DIR}/deploy-rollback.sh" ]]; then
        if "${DEPLOY_DIR}/deploy-rollback.sh" "$ENVIRONMENT" "$DEPLOY_DIR"; then
            log_success "Automatic rollback succeeded"
            return 0
        else
            log_error "Automatic rollback also failed"
            return 1
        fi
    else
        log_error "Rollback script not found or not executable: ${DEPLOY_DIR}/deploy-rollback.sh"
        return 1
    fi
}

main() {
    print_script_info "Deploy Application" "Deploying Spring Boot application for $ENVIRONMENT environment"

    cd "$DEPLOY_DIR"

    # 배포 전 현재 상태 백업
    if [[ -x "${DEPLOY_DIR}/deploy-backup.sh" ]]; then
        log_step "📦 Backing Up Current State"
        "${DEPLOY_DIR}/deploy-backup.sh" "$ENVIRONMENT" "$DEPLOY_DIR" || log_warning "Backup failed, continuing with deployment"
    fi

    if ! verify_dependencies; then
        log_error "Dependency verification failed"
        exit 1
    fi

    if ! pull_application_image; then
        log_error "Image pull failed"
        exit 1
    fi

    if ! deploy_application; then
        log_error "Application deployment failed"
        if ! attempt_rollback; then
            log_error "Deployment and rollback both failed"
        fi
        exit 1
    fi

    if ! health_check; then
        log_error "Health check failed, triggering rollback"
        if ! attempt_rollback; then
            log_error "Deployment and rollback both failed"
        fi
        exit 1
    fi

    # 배포 성공 시 히스토리 기록
    local image_tag
    image_tag=$(grep -E '^IMAGE_TAG=' "${DEPLOY_DIR}/.env" | cut -d= -f2 || echo "unknown")
    local commit_sha
    commit_sha=$(echo "$image_tag" | grep -oE '[a-f0-9]{7}$' || echo "unknown")
    record_deployment "$ENVIRONMENT" "$image_tag" "$commit_sha" "success"

    show_deployment_status

    log_step "✅ Application Deployment Completed"
    log_success "Service: $SERVICE_NAME"
    log_success "Status: Healthy"

    exit 0
}

# 스크립트 실행
main "$@"
