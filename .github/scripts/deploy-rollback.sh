#!/bin/bash
set -e

# ============================================
# Deploy Rollback
# ============================================
# 이전 버전 또는 지정된 버전으로 롤백합니다.
#
# Usage:
#   ./deploy-rollback.sh <environment> <deploy_dir> [image_tag]
#
# Arguments:
#   environment - dev, prod
#   deploy_dir  - docker-compose.yml이 있는 디렉토리
#   image_tag   - (optional) 롤백할 이미지 태그. 미지정 시 직전 버전으로 롤백
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-utils.sh"

if [[ $# -lt 2 ]]; then
    log_error "Usage: $0 <environment> <deploy_dir> [image_tag]"
    exit 1
fi

ENVIRONMENT="$1"
DEPLOY_DIR="$2"
TARGET_TAG="${3:-}"

if ! validate_environment "$ENVIRONMENT"; then
    exit 1
fi

if [[ ! -d "$DEPLOY_DIR" ]]; then
    log_error "Deploy directory not found: $DEPLOY_DIR"
    exit 1
fi

main() {
    local service_name="${ENVIRONMENT}-app"

    log_step "Rollback Deployment"

    # 롤백 대상 태그 결정
    if [[ -z "$TARGET_TAG" ]]; then
        TARGET_TAG=$(get_previous_image "$ENVIRONMENT")
        if [[ -z "$TARGET_TAG" ]]; then
            log_error "No previous image tag found and no tag specified"
            log_error "Cannot rollback without a target version"
            exit 1
        fi
        log_info "Rolling back to previous version: $TARGET_TAG"
    else
        log_info "Rolling back to specified version: $TARGET_TAG"
    fi

    cd "$DEPLOY_DIR"

    # .env의 IMAGE_TAG를 롤백 대상으로 교체
    if [[ -f ".env" ]]; then
        sed -i "s/^IMAGE_TAG=.*/IMAGE_TAG=${TARGET_TAG}/" .env
        log_success "Updated .env IMAGE_TAG to: $TARGET_TAG"
    else
        log_error ".env file not found in $DEPLOY_DIR"
        exit 1
    fi

    # 이미지 pull
    log_info "Pulling rollback image..."
    if ! docker compose --env-file .env pull "$service_name"; then
        log_error "Failed to pull rollback image"
        record_deployment "$ENVIRONMENT" "$TARGET_TAG" "unknown" "rollback-failed"
        exit 1
    fi

    # 서비스 재시작
    log_info "Restarting service with rollback image..."
    docker compose --env-file .env up -d

    sleep 5

    # 헬스체크
    log_info "Checking rollback health..."
    local max_attempts=150
    local healthy=false

    for i in $(seq 1 "$max_attempts"); do
        if docker exec "$service_name" wget --quiet --spider http://localhost:8080/actuator/health 2>/dev/null; then
            healthy=true
            break
        fi
        log_info "Rollback health check $i/$max_attempts..."
        sleep 1
    done

    if [[ "$healthy" == "true" ]]; then
        log_success "Rollback successful! Service is healthy with tag: $TARGET_TAG"
        record_deployment "$ENVIRONMENT" "$TARGET_TAG" "unknown" "rollback-success"
        exit 0
    else
        log_error "Rollback health check failed for tag: $TARGET_TAG"
        log_error "Recent application logs:"
        docker compose --env-file .env logs --tail=100 "$service_name"
        record_deployment "$ENVIRONMENT" "$TARGET_TAG" "unknown" "rollback-failed"
        exit 1
    fi
}

main "$@"
