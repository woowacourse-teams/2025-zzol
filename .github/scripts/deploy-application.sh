#!/bin/bash
set -e

# ============================================
# Deploy Application (Spring Boot)
# ============================================
# Spring Boot 애플리케이션 컨테이너를 배포합니다.
# 이미지를 pull하고, 컨테이너를 재시작하며, 헬스체크를 수행합니다.
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

# .env 파일 존재 여부 확인 (중요)
if ! require_file "${DEPLOY_DIR}/.env"; then
    log_error ".env file missing in ${DEPLOY_DIR}. Please ensure .env is generated and transferred."
    exit 1
fi

# .env 파일 로드 (변수 사용을 위해)
# set -a; source "${DEPLOY_DIR}/.env"; set +a
# 다만, docker-compose가 자동으로 .env를 읽으므로 굳이 source 할 필요는 없으나,
# 아래에서 이미지 이름을 조합하거나 할 때 필요할 수 있음.
# 여기서는 docker-compose에 맡깁니다.

# ============================================
# 변수 설정
# ============================================

SERVICE_NAME="${ENVIRONMENT}-app"
# 이미지 이름은 docker-compose가 .env를 보고 결정하므로 여기서는 로깅용으로만 추정하거나,
# 확실하게 하려면 docker-compose config를 통해 알아내야 함.
# 단순화를 위해 로그 메시지는 일반화합니다.

# ============================================
# 메인 함수
# ============================================

pull_application_image() {
    log_step "🐳 Application Image Pull"

    # .env 파일이 있으므로 docker-compose가 알아서 변수 치환하여 pull 함
    log_info "Pulling images using docker-compose..."

    if docker-compose pull "$SERVICE_NAME"; then
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

    log_info "Deploying service: $SERVICE_NAME"

    # 기존 컨테이너 정보 저장 (롤백용)
    local old_container_id=""
    if check_container_running "$SERVICE_NAME"; then
        old_container_id=$(docker ps -q -f name="$SERVICE_NAME")
        log_info "Found existing container: $old_container_id"
    fi

    # 애플리케이션 재시작 (강제 재생성, DB 의존성 무시)
    # Note: --no-deps는 안전함 (이미 verify_dependencies()에서 확인했음)
    log_info "Starting new container..."
    docker-compose up -d --force-recreate --no-deps "$SERVICE_NAME"

    # 컨테이너 시작 대기
    sleep 5

    # 새 컨테이너 ID 확인
    local new_container_id
    new_container_id=$(docker ps -q -f name="$SERVICE_NAME")

    if [[ -z "$new_container_id" ]]; then
        log_error "Container failed to start: $SERVICE_NAME"
        return 1
    fi

    log_success "Container started: $new_container_id"

    return 0
}

health_check() {
    log_step "🏥 Application Health Check"

    local max_attempts=40
    local interval=1

    log_info "Waiting for application to be healthy (max ${max_attempts}s)..."

    for i in $(seq 1 "$max_attempts"); do
        # /actuator/health 엔드포인트 체크 (curl -f: HTTP 오류 시 실패)
        if docker exec "$SERVICE_NAME" curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_success "Application is healthy!"
            return 0
        fi

        echo "Attempt $i/$max_attempts: Application not ready yet..."
        sleep "$interval"
    done

    log_error "Health check failed after ${max_attempts} attempts"

    # 실패 시 로그 출력
    echo ""
    log_error "Recent application logs:"
    docker-compose logs --tail=100 "$SERVICE_NAME"

    return 1
}

show_deployment_status() {
    log_step "📊 Deployment Status"

    # 컨테이너 상태
    echo ""
    log_info "Container status:"
    docker-compose ps "$SERVICE_NAME"

    # 최근 로그
    echo ""
    log_info "Recent application logs:"
    docker-compose logs --tail=30 "$SERVICE_NAME"

    # 이미지 정보
    echo ""
    log_info "Deployed image:"
    docker inspect "$SERVICE_NAME" --format='{{.Config.Image}}' 2>/dev/null || echo "N/A"
}

main() {
    print_script_info "Deploy Application" "Deploying Spring Boot application for $ENVIRONMENT environment"

    log_info "Environment: $ENVIRONMENT"
    log_info "Service: $SERVICE_NAME"
    log_info "Deploy Directory: $DEPLOY_DIR"

    # 배포 디렉토리로 이동
    cd "$DEPLOY_DIR"

    # 1. .env 파일은 이미 존재해야 함 (사전 체크 완료됨)

    # 2. 의존성 확인
    if ! verify_dependencies; then
        log_error "Dependency verification failed"
        exit 1
    fi

    # 3. 이미지 Pull
    if ! pull_application_image; then
        log_error "Image pull failed"
        exit 1
    fi

    # 4. 애플리케이션 배포
    if ! deploy_application; then
        log_error "Application deployment failed"
        exit 1
    fi

    # 5. 헬스체크
    if ! health_check; then
        log_error "Health check failed"
        exit 1
    fi

    # 6. 배포 상태 확인
    show_deployment_status

    log_step "✅ Application Deployment Completed"
    log_success "Service: $SERVICE_NAME"
    log_success "Status: Healthy"

    exit 0
}

# 스크립트 실행
main "$@"
