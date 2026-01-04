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

# ============================================
# 롤백 지원 함수
# ============================================

get_container_image_tag() {
    local container_name="$1"
    docker inspect "$container_name" --format='{{.Config.Image}}' 2>/dev/null || return 1
}

save_deployment_checkpoint() {
    local deploy_dir="$1"
    local environment="$2"
    local service_name="${environment}-app"
    local checkpoint_file="${deploy_dir}/.deployment-checkpoint"

    # 컨테이너가 실행 중인지 확인
    if ! check_container_running "$service_name"; then
        log_warning "No running container found (first deployment?)"
        return 0  # 첫 배포는 에러 아님
    fi

    # 현재 이미지 태그 캡처
    local current_image
    current_image=$(get_container_image_tag "$service_name") || {
        log_error "Failed to capture current image"
        return 1
    }

    # Checkpoint 파일 저장
    {
        echo "ENVIRONMENT=$environment"
        echo "SERVICE_NAME=$service_name"
        echo "PREVIOUS_IMAGE=$current_image"
        echo "CHECKPOINT_TIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)"
    } > "$checkpoint_file"

    log_success "Deployment checkpoint saved: $checkpoint_file"
    return 0
}

restore_from_checkpoint() {
    local deploy_dir="$1"
    local environment="$2"
    local checkpoint_file="${deploy_dir}/.deployment-checkpoint"

    if [[ ! -f "$checkpoint_file" ]]; then
        log_error "No checkpoint file found at: $checkpoint_file"
        return 1
    fi

    # Checkpoint 로드
    source "$checkpoint_file"

    if [[ -z "$PREVIOUS_IMAGE" ]]; then
        log_error "Invalid checkpoint: PREVIOUS_IMAGE not found"
        return 1
    fi

    log_warning "Rolling back to previous image: $PREVIOUS_IMAGE"
    return 0
}

rollback_to_image() {
    local environment="$1"
    local service_name="${environment}-app"
    local previous_image="$2"
    local deploy_dir="$3"

    log_step "ROLLBACK: Reverting to previous image"
    log_info "Previous image: $previous_image"

    # .env 파일에서 IMAGE_TAG 업데이트
    local previous_tag="${previous_image##*:}"  # 태그 부분만 추출
    {
        grep -v "^IMAGE_TAG=" "$deploy_dir/.env"
        echo "IMAGE_TAG=${previous_tag}"
    } > "$deploy_dir/.env.tmp"
    mv "$deploy_dir/.env.tmp" "$deploy_dir/.env"

    # 실패한 컨테이너 중지 및 제거
    log_info "Stopping failed container..."
    docker compose --env-file .env stop "$service_name" 2>/dev/null || true
    docker compose --env-file .env rm -f "$service_name" 2>/dev/null || true

    # 이전 이미지로 재시작
    log_info "Starting container with previous image..."
    docker compose --env-file .env up -d --no-deps "$service_name"

    sleep 5

    # 컨테이너 시작 확인
    if ! check_container_running "$service_name"; then
        log_error "Failed to start container with previous image"
        return 1
    fi

    log_success "Container restarted with previous image"
    return 0
}

verify_rollback_health() {
    local service_name="$1"
    local max_attempts=30
    local interval=2

    log_info "Verifying rolled back container health..."

    for i in $(seq 1 "$max_attempts"); do
        if docker exec "$service_name" curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; then
            log_success "Rollback successful - previous version is healthy"
            return 0
        fi
        sleep "$interval"
    done

    log_error "Rolled back container is also unhealthy"
    return 1
}

perform_rollback() {
    local environment="$1"
    local deploy_dir="$2"
    local service_name="${environment}-app"

    log_error "Initiating automatic rollback..."

    # 실패 전 로그 출력
    echo ""
    log_error "Recent application logs (before rollback):"
    docker compose --env-file .env logs --tail=50 "$service_name" 2>/dev/null || true
    echo ""

    # Checkpoint 복원
    if ! restore_from_checkpoint "$deploy_dir" "$environment"; then
        log_error "Cannot rollback - no previous deployment checkpoint"
        return 1
    fi

    # 롤백 실행
    if ! rollback_to_image "$environment" "$PREVIOUS_IMAGE" "$deploy_dir"; then
        log_error "Rollback execution failed"
        return 1
    fi

    # 롤백된 컨테이너 Health check
    if ! verify_rollback_health "$service_name"; then
        return 1
    fi

    return 0
}
