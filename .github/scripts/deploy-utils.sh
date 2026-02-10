#!/bin/bash

# ============================================
# Docker Compose Deployment Utilities
# ============================================
# 배포 스크립트에서 공통으로 사용하는 유틸리티 함수 모음
#
# Usage:
#   source .github/scripts/deploy-utils.sh
#
# Functions:
#   - log_info, log_success, log_error, log_warning
#   - check_container_running
#   - wait_for_healthy
#   - pull_image_with_retry
#   - record_deployment, get_previous_image, get_history
# ============================================

# ============================================
# 로깅 함수
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

log_warning() {
    echo "⚠️  $*"
}

log_step() {
    echo ""
    echo "============================================"
    echo "$*"
    echo "============================================"
}

# ============================================
# 컨테이너 상태 확인
# ============================================

check_container_running() {
    local container_name="$1"

    if docker ps --format '{{.Names}}' | grep -q "^${container_name}$"; then
        return 0
    else
        return 1
    fi
}

check_container_healthy() {
    local container_name="$1"

    local health_status
    health_status=$(docker inspect --format='{{.State.Health.Status}}' "$container_name" 2>/dev/null || echo "none")

    if [[ "$health_status" == "healthy" ]]; then
        return 0
    else
        return 1
    fi
}

# ============================================
# 헬스체크 대기
# ============================================

wait_for_healthy() {
    local container_name="$1"
    local max_attempts="${2:-30}"
    local interval="${3:-2}"

    log_info "Waiting for $container_name to be healthy..."

    for i in $(seq 1 "$max_attempts"); do
        if check_container_healthy "$container_name"; then
            log_success "$container_name is healthy!"
            return 0
        fi

        echo "Attempt $i/$max_attempts: $container_name not ready yet..."
        sleep "$interval"
    done

    log_error "$container_name health check timeout after $max_attempts attempts"
    return 1
}

wait_for_container() {
    local container_name="$1"
    local max_attempts="${2:-30}"
    local interval="${3:-2}"

    log_info "Waiting for $container_name to be running..."

    for i in $(seq 1 "$max_attempts"); do
        if check_container_running "$container_name"; then
            log_success "$container_name is running!"
            return 0
        fi

        echo "Attempt $i/$max_attempts: $container_name not running yet..."
        sleep "$interval"
    done

    log_error "$container_name start timeout after $max_attempts attempts"
    return 1
}

# ============================================
# Docker 이미지 관리
# ============================================

pull_image_with_retry() {
    local image="$1"
    local max_attempts="${2:-3}"

    log_info "Pulling Docker image: $image"

    for i in $(seq 1 "$max_attempts"); do
        if docker pull "$image"; then
            log_success "Image pulled successfully: $image"
            return 0
        fi

        log_warning "Attempt $i/$max_attempts failed, retrying..."
        sleep 2
    done

    log_error "Failed to pull image after $max_attempts attempts: $image"
    return 1
}

# ============================================
# Docker Compose 헬퍼
# ============================================

compose_service_exists() {
    local service_name="$1"

    if docker compose config --services 2>/dev/null | grep -q "^${service_name}$"; then
        return 0
    else
        return 1
    fi
}

compose_service_running() {
    local service_name="$1"

    if docker compose ps "$service_name" 2>/dev/null | grep -q "Up"; then
        return 0
    else
        return 1
    fi
}

# ============================================
# 환경 검증
# ============================================

validate_environment() {
    local env="$1"

    if [[ "$env" != "dev" && "$env" != "prod" ]]; then
        log_error "Invalid environment: $env (must be 'dev' or 'prod')"
        return 1
    fi

    return 0
}

require_env_var() {
    local var_name="$1"
    local var_value="${!var_name}"

    if [[ -z "$var_value" ]]; then
        log_error "Required environment variable not set: $var_name"
        return 1
    fi

    return 0
}

# ============================================
# 파일 존재 확인
# ============================================

require_file() {
    local file_path="$1"

    if [[ ! -f "$file_path" ]]; then
        log_error "Required file not found: $file_path"
        return 1
    fi

    return 0
}

# ============================================
# 배포 히스토리 관리
# ============================================

DEPLOY_STATE_DIR="${HOME}/.deploy"

record_deployment() {
    local env="$1"
    local image_tag="$2"
    local commit_sha="${3:-unknown}"
    local status="${4:-success}"

    local history_dir="${DEPLOY_STATE_DIR}/${env}"
    mkdir -p "$history_dir"

    local timestamp
    timestamp=$(date '+%Y-%m-%d %H:%M:%S')

    echo "${timestamp}|${image_tag}|${commit_sha}|${status}" >> "${history_dir}/history.log"
    log_info "Deployment recorded: env=${env}, tag=${image_tag}, status=${status}"
}

get_previous_image() {
    local env="$1"

    local tag_file="${DEPLOY_STATE_DIR}/${env}/previous-image-tag"

    if [[ -f "$tag_file" ]]; then
        cat "$tag_file"
    else
        echo ""
    fi
}

get_history() {
    local env="$1"
    local count="${2:-10}"

    local history_file="${DEPLOY_STATE_DIR}/${env}/history.log"

    if [[ -f "$history_file" ]]; then
        tail -n "$count" "$history_file"
    else
        log_warning "No deployment history found for environment: $env"
    fi
}

# ============================================
# 스크립트 정보 출력
# ============================================

print_script_info() {
    local script_name="$1"
    local description="$2"

    echo "============================================"
    echo "🚀 $script_name"
    echo "============================================"
    echo "$description"
    echo ""
}
