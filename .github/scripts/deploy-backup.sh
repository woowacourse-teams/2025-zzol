#!/bin/bash
set -e

# ============================================
# Deploy Backup
# ============================================
# 배포 전 현재 상태를 백업합니다.
# - 현재 실행 중인 앱 컨테이너의 이미지 태그 저장
# - .env, docker-compose.yml 백업
#
# Usage:
#   ./deploy-backup.sh <environment> <deploy_dir>
#
# Arguments:
#   environment - dev, prod
#   deploy_dir  - docker-compose.yml이 있는 디렉토리
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-utils.sh"

if [[ $# -lt 2 ]]; then
    log_error "Usage: $0 <environment> <deploy_dir>"
    exit 1
fi

ENVIRONMENT="$1"
DEPLOY_DIR="$2"

if ! validate_environment "$ENVIRONMENT"; then
    exit 1
fi

main() {
    local service_name="${ENVIRONMENT}-app"
    local backup_dir="${DEPLOY_STATE_DIR}/${ENVIRONMENT}"

    mkdir -p "$backup_dir"

    log_step "Backup Current Deployment State"

    # 현재 실행 중인 컨테이너의 이미지 태그 저장
    save_current_image_tag "$service_name" "$ENVIRONMENT"

    # .env 백업
    if [[ -f "${DEPLOY_DIR}/.env" ]]; then
        cp "${DEPLOY_DIR}/.env" "${DEPLOY_DIR}/.env.backup"
        log_success "Backed up .env"
    fi

    # docker-compose.yml 백업
    if [[ -f "${DEPLOY_DIR}/docker-compose.yml" ]]; then
        cp "${DEPLOY_DIR}/docker-compose.yml" "${DEPLOY_DIR}/docker-compose.yml.backup"
        log_success "Backed up docker-compose.yml"
    fi

    log_success "Backup completed for environment: $ENVIRONMENT"
}

main "$@"
