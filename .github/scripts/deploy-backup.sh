#!/bin/bash
set -e

# ============================================
# Deploy Backup
# ============================================
# 배포 전 현재 상태를 백업합니다.
# - .env 백업 (BLUE/GREEN_IMAGE_TAG 포함)
# - docker-compose.yml 백업
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
    log_step "Backup Current Deployment State"

    # .env 백업 (BLUE/GREEN_IMAGE_TAG 가 배포 이력)
    if [[ -f "${DEPLOY_DIR}/.env" ]]; then
        cp "${DEPLOY_DIR}/.env" "${DEPLOY_DIR}/.env.backup"
        log_success "Backed up .env → .env.backup"
    fi

    # docker-compose.yml 백업
    if [[ -f "${DEPLOY_DIR}/docker-compose.yml" ]]; then
        cp "${DEPLOY_DIR}/docker-compose.yml" "${DEPLOY_DIR}/docker-compose.yml.backup"
        log_success "Backed up docker-compose.yml"
    fi

    log_success "Backup completed for environment: $ENVIRONMENT"
}

main "$@"
