#!/bin/bash
set -e

# ============================================
# Deploy Infrastructure (MySQL, Redis, Alloy)
# ============================================
# DB·Redis·Alloy(로그 수집기) 컨테이너를 관리합니다.
# 최초 배포 시 생성하고, 이미 존재하면 유지합니다.
#
# Alloy는 로그 수집기라 앱 가용성과 무관하므로, bootstrap(config.alloy)이
# 호스트에 배치되지 않았으면 alloy만 skip하고 배포를 계속합니다(ADR-0028).
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

deploy_alloy() {
    local service_name="${ENVIRONMENT}-alloy"
    local bootstrap="${DEPLOY_DIR}/conf/config.alloy"

    log_step "📦 Alloy 로그 수집기 배포"

    # bootstrap config.alloy는 호스트에 1회 수동 배치된다(ADR-0028, 운영 절차 참조).
    # backend-cd는 conf/를 서버로 전송하지 않으므로, 없으면 alloy만 skip하고
    # 인프라·앱 배포는 계속한다. 로그 수집 부재는 Prometheus AlloyDown 알림이 사후 감지한다.
    # set -e 하에서 bare 호출은 2/3 반환 시 스크립트를 abort시킨다.
    # 판정 결과를 status로 받아 errexit 의존 없이 자체적으로 분기한다.
    local status=0
    classify_alloy_bootstrap "$bootstrap" || status=$?
    case $status in
        2)
            log_warning "config.alloy 가 파일이 아닌 디렉터리임 → alloy 기동 skip: $bootstrap"
            log_warning "Docker가 마운트 소스 부재 시 빈 디렉터리를 자동 생성한 흔적일 수 있음."
            log_warning "복구: rm -rf '$bootstrap' 후 실제 파일 배치 (절차: backend/docs/adr/0028-alloy-pull-based-config-via-git.md)"
            return 0
            ;;
        3)
            log_warning "Alloy bootstrap 누락 → alloy 기동 skip: $bootstrap"
            log_warning "최초 1회 호스트 배치 필요:"
            log_warning "  mkdir -p '${DEPLOY_DIR}/conf'"
            log_warning "  # 레포 backend/docker/${ENVIRONMENT}/conf/config.alloy 를 위 경로로 복사"
            log_warning "절차: backend/docs/adr/0028-alloy-pull-based-config-via-git.md (운영 절차)"
            return 0
            ;;
    esac

    log_info "Starting Alloy: $service_name"
    if ! docker compose --env-file .env up -d "$service_name"; then
        log_warning "Alloy 기동 실패 — 앱 배포는 계속 진행(로그 수집만 영향, AlloyDown으로 감지)"
        return 0
    fi

    if check_container_running "$service_name"; then
        log_success "Alloy 기동 완료: $service_name (health: Prometheus AlloyDown 알림으로 사후 감시)"
    else
        log_warning "Alloy 기동 직후 미실행 — 'docker logs $service_name' 확인 권장"
    fi

    return 0
}

ensure_monitoring_network() {
    log_info "Ensuring monitoring-network exists..."
    docker network create monitoring-network 2>/dev/null || true
    if docker network inspect monitoring-network &>/dev/null; then
        log_success "monitoring-network is ready"
    else
        log_error "monitoring-network could not be created or verified"
        return 1
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

    # Alloy는 로그 수집기 — bootstrap 미배치/기동 실패해도 앱 배포를 막지 않는다(skip+warning).
    deploy_alloy || log_warning "Alloy 단계 예기치 못한 오류 — 무시하고 계속"

    log_step "✅ Infrastructure Deployment Completed"
    log_success "MySQL: ${ENVIRONMENT}-mysql (healthy)"
    log_success "Redis: ${ENVIRONMENT}-redis (healthy)"

    echo ""
    log_info "Current infrastructure status:"
    docker compose --env-file .env ps "${ENVIRONMENT}-mysql" "${ENVIRONMENT}-redis" "${ENVIRONMENT}-alloy" || log_warning "Status check incomplete"
    exit 0
}

main "$@"
