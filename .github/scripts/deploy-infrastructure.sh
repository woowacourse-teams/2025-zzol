#!/bin/bash
set -e

# ============================================
# Deploy Infrastructure (MySQL, Redis)
# ============================================
# DB와 Redis 컨테이너를 관리합니다.
# 최초 배포 시 생성하고, 이미 존재하면 유지합니다.
#
# Usage:
#   ./deploy-infrastructure.sh <environment> <deploy_dir>
#
# Arguments:
#   environment - dev, prod
#   deploy_dir  - docker-compose.yml이 있는 디렉토리
#
# Exit Codes:
#   0 - Success
#   1 - Error
#
# Examples:
#   ./deploy-infrastructure.sh dev ~/dev
#   ./deploy-infrastructure.sh prod ~/prod
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

# ============================================
# 메인 함수
# ============================================

deploy_mysql() {
    local service_name="${ENVIRONMENT}-mysql"

    log_step "📦 MySQL 배포"

    if check_container_running "$service_name"; then
        log_success "MySQL already running: $service_name"

        if check_container_healthy "$service_name"; then
            log_success "MySQL is healthy"
        else
            log_warning "MySQL is running but not healthy yet"

            if wait_for_healthy "$service_name" 30 2; then
                log_success "MySQL became healthy"
            else
                log_error "MySQL failed to become healthy"
                docker compose --env-file .env logs --tail=50 "$service_name"
                return 1
            fi
        fi
    else
        log_info "Starting MySQL: $service_name"
        docker compose --env-file .env up -d "$service_name"

        if wait_for_healthy "$service_name" 30 2; then
            log_success "MySQL deployment completed"
        else
            log_error "MySQL failed to become healthy"
            docker compose --env-file .env logs --tail=50 "$service_name"
            return 1
        fi
    fi

    return 0
}

deploy_redis() {
    local service_name="${ENVIRONMENT}-redis"

    log_step "📦 Redis 배포"

    if check_container_running "$service_name"; then
        log_success "Redis already running: $service_name"

        if check_container_healthy "$service_name"; then
            log_success "Redis is healthy"
        else
            log_warning "Redis is running but not healthy yet"

            if wait_for_healthy "$service_name" 15 2; then
                log_success "Redis became healthy"
            else
                log_error "Redis failed to become healthy"
                docker compose --env-file .env logs --tail=50 "$service_name"
                return 1
            fi
        fi
    else
        log_info "Starting Redis: $service_name"
        docker compose --env-file .env up -d "$service_name"

        if wait_for_healthy "$service_name" 15 2; then
            log_success "Redis deployment completed"
        else
            log_error "Redis failed to become healthy"
            docker compose --env-file .env logs --tail=50 "$service_name"
            return 1
        fi
    fi

    return 0
}

ensure_monitoring_network() {
    if ! docker network inspect monitoring-network &>/dev/null; then
        log_info "Creating monitoring-network..."
        docker network create monitoring-network
        log_success "monitoring-network created"
    else
        log_success "monitoring-network already exists"
    fi
}

main() {
    print_script_info "Deploy Infrastructure" "Deploying MySQL and Redis for $ENVIRONMENT environment"

    cd "$DEPLOY_DIR"

    if [[ ! -f .env ]]; then
        log_error ".env file not found in ${DEPLOY_DIR}"
        exit 1
    fi

    ensure_monitoring_network

    if ! deploy_mysql; then
        log_error "MySQL deployment failed"
        exit 1
    fi

    if ! deploy_redis; then
        log_error "Redis deployment failed"
        exit 1
    fi

    log_step "✅ Infrastructure Deployment Completed"
    log_success "MySQL: ${ENVIRONMENT}-mysql (healthy)"
    log_success "Redis: ${ENVIRONMENT}-redis (healthy)"

    echo ""
    log_info "Current infrastructure status:"
    docker compose --env-file .env ps "${ENVIRONMENT}-mysql" "${ENVIRONMENT}-redis" || log_warning "Status check incomplete"
    exit 0
}

main "$@"
