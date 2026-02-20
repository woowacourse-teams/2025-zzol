#!/bin/bash

# ============================================
# Docker Compose Deployment Utilities
# ============================================
# 배포 스크립트에서 공통으로 사용하는 유틸리티 함수 모음
#
# 상태 관리 방식:
#   - 트래픽 기준: ~/nginx/conf/{env}-service.inc  (nginx가 읽는 파일 자체)
#   - 이미지 기준: ~/prod(dev)/.env 의 BLUE/GREEN_IMAGE_TAG (docker compose가 읽는 파일 자체)
#   별도 상태 파일 없음 - 각 시스템이 자신의 상태를 직접 보관
#
# Usage:
#   source .github/scripts/deploy-utils.sh
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
# Readiness probe (actuator/health/readiness)
# ============================================

wait_for_app_ready() {
    local container_name="$1"
    local max_attempts="${2:-150}"
    local interval="${3:-1}"

    log_info "Waiting for $container_name to be ready (max ${max_attempts}s)..."

    for i in $(seq 1 "$max_attempts"); do
        if docker exec "$container_name" wget --quiet --spider http://localhost:8080/actuator/health/readiness 2>/dev/null; then
            log_success "$container_name is ready!"
            return 0
        fi

        log_info "Attempt $i/$max_attempts: $container_name not ready yet..."
        sleep "$interval"
    done

    log_error "$container_name readiness check timeout after $max_attempts attempts"
    return 1
}

# ============================================
# Blue-Green 색상 관리
# 진실의 근거: {env}-service.inc 파일
# ============================================

# nginx 컨테이너 이름 (기본값)
NGINX_CONTAINER="${NGINX_CONTAINER:-nginx}"

# 서버 호스트의 nginx conf 디렉토리 (기본값)
NGINX_CONF_BASE="${NGINX_CONF_BASE:-${HOME}/nginx/conf}"

# {env}-service.inc 를 읽어 현재 active 색상 반환
# 파일이 없으면 "" 반환 → Bootstrap 필요
get_active_color() {
    local env="$1"
    local nginx_inc="${NGINX_CONF_BASE}/${env}-service.inc"

    if [[ ! -f "$nginx_inc" ]]; then
        echo ""
        return
    fi

    grep -oiE 'app-(blue|green)\b' "$nginx_inc" | sed -E 's/^app-//' | head -1
}

get_inactive_color() {
    local active="$1"

    if [[ "$active" == "blue" ]]; then
        echo "green"
    else
        echo "blue"
    fi
}

# ============================================
# Nginx upstream 전환
# 진실의 근거: {env}-service.inc 파일 자체를 교체
# ============================================

switch_nginx_upstream() {
    local container_name="$1"
    local environment="$2"

    local nginx_inc="${NGINX_CONF_BASE}/${environment}-service.inc"
    local new_line="set \$upstream http://${container_name}:8080;"

    log_info "Switching nginx upstream to: $container_name"

    # 1. 사전 검증: 변경 전 현재 설정이 유효한지 확인
    if ! docker exec "$NGINX_CONTAINER" nginx -t 2>&1; then
        log_error "Nginx config is already invalid before upstream change — aborting"
        return 1
    fi

    # 2. 롤백을 위해 현재 inc 내용 저장
    local prev_line=""
    if [[ -f "$nginx_inc" ]]; then
        prev_line=$(cat "$nginx_inc")
    fi

    # 3. inc 파일 교체 (atomic write)
    echo "$new_line" > "$nginx_inc"

    # 4. 사후 검증: 새 설정이 문법상 유효한지 확인
    if ! docker exec "$NGINX_CONTAINER" nginx -t 2>&1; then
        log_error "Nginx config test failed after upstream change — restoring previous config"
        if [[ -n "$prev_line" ]]; then
            echo "$prev_line" > "$nginx_inc"
        else
            rm -f "$nginx_inc"
        fi
        return 1
    fi

    # 5. Graceful reload
    if ! docker exec "$NGINX_CONTAINER" nginx -s reload; then
        log_error "Nginx reload failed — restoring previous config"
        if [[ -n "$prev_line" ]]; then
            echo "$prev_line" > "$nginx_inc"
        else
            rm -f "$nginx_inc"
        fi
        return 1
    fi

    # 6. Reload 완료 대기: 새 워커가 요청을 받을 때까지 nginx -t 로 폴링
    local max_wait=10
    local interval=1
    for i in $(seq 1 "$max_wait"); do
        if docker exec "$NGINX_CONTAINER" nginx -t 2>/dev/null; then
            log_success "Nginx upstream switched to $container_name (reload confirmed in ${i}s)"
            return 0
        fi
        log_info "Waiting for nginx reload... ($i/${max_wait}s)"
        sleep "$interval"
    done

    log_warning "Nginx reload signal sent but could not confirm within ${max_wait}s — check nginx manually"
    return 0
}

# ============================================
# .env 이미지 태그 관리
# 진실의 근거: .env 파일의 BLUE/GREEN_IMAGE_TAG
# ============================================

# .env 파일에서 색상별 이미지 태그 읽기
get_env_image_tag() {
    local env_file="$1"
    local color="$2"
    local var_name="${color^^}_IMAGE_TAG"

    grep "^${var_name}=" "$env_file" 2>/dev/null | cut -d'=' -f2- | xargs
}

# .env 파일의 색상별 이미지 태그를 원자적으로 수정
# tmp 파일에 먼저 쓴 뒤 mv로 교체 → 쓰기 도중 실패해도 원본 보존
update_env_image_tag() {
    local env_file="$1"
    local color="$2"
    local tag="$3"
    local var_name="${color^^}_IMAGE_TAG"
    local escaped_tag="${tag//&/\\&}"

    # tmp 파일은 같은 디렉토리에 생성 (mv가 atomic하려면 같은 파일시스템이어야 함)
    local tmp_file
    tmp_file="$(dirname "$env_file")/.env.write.$$"

    if grep -q "^${var_name}=" "$env_file"; then
        sed "s#^${var_name}=.*#${var_name}=${escaped_tag}#" "$env_file" > "$tmp_file"
    else
        cp "$env_file" "$tmp_file"
        echo "${var_name}=${tag}" >> "$tmp_file"
    fi

    mv "$tmp_file" "$env_file"
    log_success "Updated ${var_name}=${tag} in $(basename "$env_file")"
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
