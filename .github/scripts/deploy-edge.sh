#!/bin/bash
set -e

# ============================================
# Deploy Edge & Monitoring Config (nginx, Prometheus 등)
# ============================================
# 공유 호스트의 nginx(엣지)·monitoring 설정을 Git 기준으로 동기화한다.
# 적용 순서는 "검증 우선, 실패 시 원복" 원칙을 따른다:
#   백업 → sync(*-service.inc 제외) → nginx -t(재생성 전 게이트)
#        → compose up -d → reload → 실제 경로 프로브 → 실패 시 원복
#
# Usage:
#   ./deploy-edge.sh <staging_dir>
#
# Arguments:
#   staging_dir - scp로 전송된 루트. 하위에 backend/docker/{nginx,monitoring}/ 가 있다.
#
# 환경 변수(기본값 오버라이드용):
#   NGINX_DIR(~/nginx) MONITOR_DIR(~/monitor) NGINX_CONTAINER(nginx) PROMETHEUS_CONTAINER(prometheus)
#
# Exit Codes:
#   0 - Success
#   1 - Error (원복 시도됨)
# ============================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-utils.sh"

# ============================================
# 파라미터 / 경로
# ============================================

if [[ $# -lt 1 ]]; then
    log_error "Usage: $0 <staging_dir>"
    exit 1
fi

STAGING_ROOT="$1"
NGINX_SRC="${STAGING_ROOT}/backend/docker/nginx"
MONITOR_SRC="${STAGING_ROOT}/backend/docker/monitoring"

NGINX_DIR="${NGINX_DIR:-${HOME}/nginx}"
MONITOR_DIR="${MONITOR_DIR:-${HOME}/monitor}"
NGINX_CONTAINER="${NGINX_CONTAINER:-nginx}"
PROMETHEUS_CONTAINER="${PROMETHEUS_CONTAINER:-prometheus}"

# 라이브 Blue/Green 업스트림 포인터 — deploy-application.sh가 런타임에 다시 쓰므로 절대 동기화 금지
# 배열로 전개해 비따옴표 글로빙을 막는다.
SERVICE_INC_EXCLUDE=(--exclude='*-service.inc')

BACKUP_DIR=""

# ============================================
# 백업 / 원복
# ============================================

backup_edge() {
    BACKUP_DIR="$(mktemp -d)"
    log_info "현재 설정 백업: ${BACKUP_DIR}"

    if [[ -d "${NGINX_DIR}/conf" ]]; then
        cp -a "${NGINX_DIR}/conf" "${BACKUP_DIR}/nginx-conf"
    fi
    if [[ -f "${NGINX_DIR}/docker-compose.yml" ]]; then
        cp -a "${NGINX_DIR}/docker-compose.yml" "${BACKUP_DIR}/nginx-compose.yml"
    fi
    if [[ -d "${MONITOR_DIR}/conf" ]]; then
        cp -a "${MONITOR_DIR}/conf" "${BACKUP_DIR}/monitor-conf"
    fi
    if [[ -f "${MONITOR_DIR}/docker-compose.yml" ]]; then
        cp -a "${MONITOR_DIR}/docker-compose.yml" "${BACKUP_DIR}/monitor-compose.yml"
    fi
}

# nginx 파일만 원복 (reload 전 실패용 — 옛 워커가 아직 옛 설정을 서빙하므로 reload 불필요)
# rsync --delete로 sync가 추가한 파일까지 제거해 백업 상태와 완전 일치시킨다.
# service.inc는 라이브 상태이므로 백업본으로 덮지 않는다(제외).
restore_nginx_files() {
    log_warning "nginx 설정 원복 (reload 없이 파일만)"
    [[ -d "${BACKUP_DIR}/nginx-conf" ]] && \
        rsync -a --delete "${SERVICE_INC_EXCLUDE[@]}" "${BACKUP_DIR}/nginx-conf/" "${NGINX_DIR}/conf/"
    [[ -f "${BACKUP_DIR}/nginx-compose.yml" ]] && \
        cp -a "${BACKUP_DIR}/nginx-compose.yml" "${NGINX_DIR}/docker-compose.yml"
}

# nginx 원복 + reload + 재프로브 (reload 후 프로브 실패용 — 깨진 설정이 라이브이므로 reload 필수)
restore_nginx_and_reload() {
    log_warning "nginx 설정 원복 + reload (깨진 설정이 라이브 상태)"
    restore_nginx_files
    if docker exec "${NGINX_CONTAINER}" nginx -t 2>&1 && docker exec "${NGINX_CONTAINER}" nginx -s reload; then
        if probe_nginx; then
            log_success "원복 후 프로브 통과 — 이전 정상 상태로 복귀"
        else
            log_error "원복 후에도 프로브 실패 — 수동 점검 필요"
        fi
    else
        log_error "원복 후 nginx reload 실패 — 수동 점검 필요"
    fi
}

# ============================================
# 프로브 (실제 경로 — nginx -t가 못 잡는 라우팅 오류 검증)
# ============================================

# 5xx/연결실패만 실패로 본다. 302·401 등은 nginx가 올바로 라우팅한 정상 응답.
# --resolve로 SNI·Host를 동시에 실제 도메인으로 맞춘다(-H Host만 쓰면 SNI=localhost라
# default_server 블록에 잘못 들어가 '거짓 통과'할 수 있다).
# backend-cd의 blue/green 전환과 동시 실행 시 일시적 502를 거를 수 있어 1회 재시도한다.
# 주의: /ws/info는 app 업스트림에 의존 → 배포 중 app이 죽어 있으면 false 원복 가능(드묾, 원복은 안전).
_probe() {
    local host="$1" path="$2"
    local code attempt
    for attempt in 1 2; do
        code=$(curl -sk -o /dev/null -w '%{http_code}' --max-time 5 \
            --resolve "${host}:443:127.0.0.1" "https://${host}${path}" 2>/dev/null || echo "000")
        if [[ "$code" != "000" && "$code" -lt 500 ]]; then
            log_success "프로브 OK: ${host}${path} → ${code}"
            return 0
        fi
        if [[ "$attempt" -eq 1 ]]; then
            log_warning "프로브 재시도(3s): ${host}${path} → ${code}"
            sleep 3
        fi
    done
    log_error "프로브 실패: ${host}${path} → ${code}"
    return 1
}

probe_nginx() {
    local failed=0
    _probe "api.zzol.site" "/ws/info" || failed=1
    _probe "dev.api.zzol.site" "/ws/info" || failed=1
    _probe "status.zzol.site" "/" || failed=1
    return "$failed"
}

# ============================================
# nginx 적용 (전체 안전 패턴)
# ============================================

apply_nginx() {
    log_step "🌐 nginx 설정 적용"

    if ! check_container_running "${NGINX_CONTAINER}"; then
        log_error "nginx 컨테이너가 실행 중이 아님 — edge-cd는 기존 nginx를 전제로 한다"
        return 1
    fi

    # 1. sync (라이브 service.inc 제외). conf는 :ro 바인드 마운트라 즉시 컨테이너에 반영됨
    if ! rsync -a --delete "${SERVICE_INC_EXCLUDE[@]}" "${NGINX_SRC}/conf/" "${NGINX_DIR}/conf/"; then
        log_error "nginx conf sync 실패 — 원복"
        restore_nginx_files
        return 1
    fi
    cp -a "${NGINX_SRC}/docker-compose.yml" "${NGINX_DIR}/docker-compose.yml"

    # 2. 문법 게이트 — 재생성(up -d) 전에 옛 워커로 새 conf 검증
    if ! docker exec "${NGINX_CONTAINER}" nginx -t 2>&1; then
        log_error "nginx -t 실패 — 적용 중단"
        restore_nginx_files
        return 1
    fi

    # 3. compose 변경 반영 (변경 없으면 no-op)
    if ! ( cd "${NGINX_DIR}" && docker compose up -d ); then
        log_error "docker compose up -d 실패 — 원복"
        restore_nginx_and_reload
        return 1
    fi

    # 4. conf 내용 reload
    if ! docker exec "${NGINX_CONTAINER}" nginx -s reload; then
        log_error "nginx reload 실패"
        restore_nginx_and_reload
        return 1
    fi

    # 5. 실제 경로 프로브
    if ! probe_nginx; then
        log_error "프로브 실패 — 원복"
        restore_nginx_and_reload
        return 1
    fi

    log_success "nginx 설정 적용 완료"
    return 0
}

# ============================================
# monitoring 적용 (비사용자대면 — best-effort)
# ============================================

apply_monitoring() {
    log_step "📊 monitoring 설정 적용"

    rsync -a --delete "${MONITOR_SRC}/conf/" "${MONITOR_DIR}/conf/"
    cp -a "${MONITOR_SRC}/docker-compose.yml" "${MONITOR_DIR}/docker-compose.yml"

    ( cd "${MONITOR_DIR}" && docker compose up -d )

    # Prometheus는 SIGHUP으로 conf 핫리로드 (--web.enable-lifecycle 불필요)
    if check_container_running "${PROMETHEUS_CONTAINER}"; then
        if docker kill -s HUP "${PROMETHEUS_CONTAINER}"; then
            log_success "Prometheus 설정 reload (SIGHUP)"
        else
            log_warning "Prometheus reload 실패 — 수동 점검"
        fi
    fi

    # Loki/Tempo/Grafana conf는 핫리로드 미지원 → compose 변경 시에만 up -d로 재생성됨.
    # conf-only 변경은 후속 PR에서 선택적 restart로 처리(v1 범위 외).
    log_success "monitoring 설정 동기화 완료"
    return 0
}

# ============================================
# 메인
# ============================================

main() {
    print_script_info "Deploy Edge & Monitoring" "공유 호스트의 nginx·monitoring 설정 동기화"

    require_file "${NGINX_SRC}/docker-compose.yml" || exit 1
    require_file "${MONITOR_SRC}/docker-compose.yml" || exit 1

    backup_edge

    if ! apply_nginx; then
        log_error "nginx 적용 실패 (원복됨)"
        exit 1
    fi

    # monitoring 실패는 엣지 트래픽에 영향 없음 → 경고만 하고 전체는 성공 처리
    apply_monitoring || log_warning "monitoring 적용 일부 실패 — 로그 확인"

    log_step "✅ Edge 배포 완료"
    rm -rf "${BACKUP_DIR}"
    exit 0
}

main "$@"
