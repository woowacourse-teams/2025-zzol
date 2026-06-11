# Alertmanager + 알림 룰 도입 작업 계획서

> 핸드오프 문서. 이 작업을 수행할 Claude Code는 **3장(해결 방법)을 실행하기 전에 0장의 "작업 전 검증"을 반드시 먼저 수행**할 것. 임계값·라벨 값 등 ⚠️ 표시는 실제 환경 확인 후 확정한다.

---

## 0. 핸드오프 컨텍스트

### 0.1 목적
Prometheus는 메트릭을 수집하고 있으나 **알림이 전혀 없다**(Alertmanager 미배포, rule 0개). 관측 루프가 `메트릭 → 대시보드 → ❌알림 → ❌대응`에서 끊겨 있다. 이를 `메트릭 → 알림 → 대응`까지 완성한다.

### 0.2 배경 인시던트 (이 작업의 직접 계기)
- nginx proxy 블록(`proxy_set_header X-Forwarded-For` 등 + WebSocket `Upgrade`/`Connection`)이 설정 파일에서 초기화/소실됨.
- 결과 ① `/ws/info` WebSocket 핸드셰이크 실패. ② `X-Forwarded-For` 소실로 Spring이 모든 요청을 nginx 컨테이너 내부 IP로 인식 → `IpBlockFilter`가 내부 IP를 BAN → **전 트래픽 차단**.
- `nginx -t`는 통과(문법 유효, 블록이 "없는" 것뿐)하는 **silent failure**. 로그에서 내부 IP BAN을 육안으로 보고서야 발견.
- → "이 사건을 자동으로 잡았을 알림"을 만드는 것이 본 작업의 핵심 가치.

### 0.3 검증된 현재 상태 (사실)
- 서버: OCI 인스턴스, 모니터링 스택은 `~/monitor/docker-compose.yml`.
- Git 정본: `backend/docker/monitoring/`(docker-compose.yml + conf/ + provisioning/), `backend/docker/nginx/`.
- **⚠️ drift 존재**: `backend-cd.yml`은 `backend/docker/${ENV}/docker-compose.yml` + deploy 스크립트 + `.env`만 SCP로 옮긴다. `nginx/`, `monitoring/`는 CD로 동기화되지 않아 서버(`~/monitor`, `~/nginx`)가 Git과 따로 논다. (이 GitOps 동기화 문제는 **별도 작업**이며 본 작업의 범위 밖. 단 충돌하지 않도록 0.6 참고.)
- 가동 중 컨테이너: grafana, prometheus(v2.49.1), tempo, loki, cadvisor, node-exporter, dev/prod mysql-exporter, dev/prod redis-exporter, dev/prod promtail(**prod-promtail unhealthy**).
- **Alertmanager 없음.**
- `prometheus.yml`: scrape job = `dev-app`, `prod-app`(둘 다 blue/green :8080 `/actuator/prometheus`), `cadvisor`, `mysql-exporter`, `redis-exporter`, `node`. **`alerting:` 블록 없음, `rule_files:` 없음.**
- Prometheus 실행 옵션에 `--web.enable-lifecycle` 있음 → `/-/reload` 사용 가능.
- 네트워크: `monitoring-network`(bridge), `dev-network`/`prod-network`/`zzol-network`(external).
- Prometheus는 `expose: 9090`만(호스트 포트 매핑 없음). 컨테이너 내부/네트워크에서만 접근.
- 모니터 compose는 `${GRAFANA_ADMIN_PASSWORD}` 등 env를 쓰며, **모니터 스택 전용 `.env`가 서버 `~/monitor/`에 따로 있다**(앱 `.env`와 별개).

### 0.4 실제로 수집 중인 메트릭 (확인됨)
- `http_server_requests_seconds_count` / `_sum` / `_max` (+ `_active_*`) — Spring actuator. 라벨: `uri`, `status`, `method`, `job` 등.
- `ip_block_new_total` — 신규 BAN 카운터.
- `ip_block_request_blocked_total` — 차단된 요청 카운터.
- `redis_stream_e2e_latency_seconds_bucket` / `_count` / `_sum` / `_max` — **버킷 있음(분위수 계산 가능)**.
- `redis_stream_length`, `redis_stream_threadpool_active_count`, `redis_stream_threadpool_queue_size`.
- `nickname_audit_batch_skipped_total`.
- redis-exporter, mysql-exporter, cadvisor, node-exporter 표준 메트릭.

### 0.5 작업 전 검증 (실행 Claude가 먼저 할 것)
컨테이너 내부에서 Prometheus API로 확인(호스트에서 9090 안 닿음):
```bash
# (1) http_server_requests 버킷 존재 여부 — p95 latency SLO 가능한지 결정
docker exec prometheus wget -qO- 'http://localhost:9090/api/v1/label/__name__/values' \
  | tr ',' '\n' | grep -i 'http_server_requests_seconds_bucket'

# (2) WebSocket 엔드포인트의 실제 uri 라벨 값 (/ws/info? /ws/**? sockjs?)
docker exec prometheus wget -qO- 'http://localhost:9090/api/v1/label/uri/values' \
  | tr ',' '\n' | grep -i 'ws'

# (3) ip_block 메트릭의 라벨 구조 (job, ip_type 유무) + 정상시 베이스라인
docker exec prometheus wget -qO- 'http://localhost:9090/api/v1/query?query=ip_block_request_blocked_total'
docker exec prometheus wget -qO- 'http://localhost:9090/api/v1/query?query=rate(ip_block_request_blocked_total[5m])'

# (4) 모니터 .env 위치/내용 키 (SLACK 웹훅 재사용 가능한지)
ls -la ~/monitor/.env && grep -iE 'slack|webhook' ~/monitor/.env
```
확인 결과로 다음을 확정:
- (1) 버킷 없으면 → p95 latency SLO 제외, 평균 latency(`_sum`/`_count`)만 사용.
- (2) 실제 uri 패턴으로 `WsInfoFailureRate` 룰의 `uri=~` 수정.
- (3) `ip_type` 라벨이 이미 있으면 Phase 2 코드 변경 불필요. 없으면 Phase 1은 "대량 차단 급증"으로 우회, Phase 2에서 라벨 추가.
- (4) Slack webhook 키 이름 확정(없으면 0.6대로 신규 발급).

### 0.6 범위
- **In**: Alertmanager 컨테이너 추가, prometheus 알림 연동, rule/SLO 파일 작성, Slack 라우팅, 검증, Git 정본 반영.
- **Out**: nginx/monitoring 전반의 GitOps 동기화(infra-CD) — 별도 작업. 단 본 작업도 **정본은 Git(`backend/docker/monitoring/`)에 두고**, 서버 반영 시 그 디렉터리를 사용한다(또 다른 drift를 만들지 않기 위해).
- 시크릿(Slack webhook)은 **절대 Git 커밋 금지**.

---

## 1. 문제 (Problem)
1. 알림 부재: 메트릭은 쌓이는데 임계 초과/장애를 **아무도 능동적으로 통지받지 못한다**. 장애를 사람이 로그를 들여다봐야 안다.
2. 배경 인시던트(0.2)가 그 증거: 전면 차단이 발생했는데도 **자동 감지가 0**이었고, `nginx -t` 통과 때문에 1차 오진까지 겹쳤다.
3. SLO/에러버짓 개념 부재: "어느 수준이 정상인지" 정의가 없어 정상/이상 판단 기준이 없다.

## 2. 원인 (Cause)
1. **Alertmanager 미배포** + `prometheus.yml`에 `alerting:`/`rule_files:` 없음 → 룰을 평가·발화·전달할 경로 자체가 없음.
2. IP 차단 메트릭(`ip_block_*`)은 있으나 **위험 케이스(사설/내부 IP BAN)를 식별할 라벨/룰이 없음**.
3. 구성의 silent failure를 잡을 **동작 검증(합성 프로브)·임계 알림이 없음** → 구문 검증(`nginx -t`)만 믿게 됨.

## 3. 해결 방법 (Solution)

### 3.1 전체 구조
```
Prometheus (룰 평가, --web.enable-lifecycle)
   └ rule_files: /etc/prometheus/rules/*.yml
   └ alerting → alertmanager:9093
Alertmanager (그룹화/라우팅/억제)
   └ slack_api_url_file (시크릿은 파일 마운트, Git 미포함)
   └ Slack (#zzol-alerts / #zzol-alerts-critical)
```

### 3.2 파일 변경 목록 (모두 `backend/docker/monitoring/` 기준)
```
conf/prometheus.yml          # (수정) alerting + rule_files 추가
conf/alertmanager.yml        # (신규) 라우팅/리시버
conf/rules/alerts-infra.yml  # (신규) up/타깃 down
conf/rules/alerts-app.yml    # (신규) 5xx/ws/ip_block
conf/rules/alerts-redis-stream.yml # (신규) lag/queue/length
conf/rules/slo.yml           # (신규) 가용성 SLO + burn-rate
docker-compose.yml           # (수정) alertmanager 서비스 + prometheus rules 디렉터리 마운트
secrets/.gitignore           # (신규) 시크릿 디렉터리 통째 제외
```

### 3.3 Phase 1 — Alertmanager 배포 + 기본 알림 (앱 코드 변경 없음)

**(a) docker-compose.yml — alertmanager 서비스 추가**
```yaml
  alertmanager:
    image: prom/alertmanager:v0.27.0
    container_name: alertmanager
    user: "65534:65534"
    restart: unless-stopped
    volumes:
      - ./conf/alertmanager.yml:/etc/alertmanager/alertmanager.yml:ro
      - ./secrets/slack_api_url:/etc/alertmanager/secrets/slack_api_url:ro
      - alertmanager-data:/alertmanager
    networks:
      - monitoring-network
    expose:
      - "9093"
    command:
      - '--config.file=/etc/alertmanager/alertmanager.yml'
      - '--storage.path=/alertmanager'
    environment:
      - TZ=Asia/Seoul
    deploy:
      resources:
        limits: { cpus: '0.1', memory: 128M }
        reservations: { cpus: '0.05', memory: 64M }
    healthcheck:
      test: ["CMD-SHELL", "wget --spider -q http://localhost:9093/-/healthy || exit 1"]
      interval: 30s
      timeout: 10s
      retries: 3
```
그리고 **prometheus 서비스 volumes에 rules 디렉터리 추가**:
```yaml
    volumes:
      - prometheus-data:/prometheus
      - prometheus-logs:/var/log/prometheus
      - ./conf/prometheus.yml:/etc/prometheus/prometheus.yml:ro
      - ./conf/rules:/etc/prometheus/rules:ro   # ← 추가
```
volumes 최상단에 `alertmanager-data: { driver: local }` 추가.

**(b) prometheus.yml — 최상위에 추가** (`scrape_configs`와 동급 레벨)
```yaml
rule_files:
  - /etc/prometheus/rules/*.yml

alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

**(c) conf/alertmanager.yml** (시크릿은 파일 참조 — Alertmanager는 config에서 `${ENV}` 확장 안 함에 주의)
```yaml
global:
  resolve_timeout: 5m
  slack_api_url_file: /etc/alertmanager/secrets/slack_api_url

route:
  receiver: slack-default
  group_by: ['alertname', 'severity']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  routes:
    - matchers: [ 'severity="critical"' ]
      receiver: slack-critical
      repeat_interval: 1h

receivers:
  - name: slack-default
    slack_configs:
      - channel: '#zzol-alerts'          # ⚠️ 실제 채널명 확정
        send_resolved: true
        title: '[{{ .CommonLabels.severity }}] {{ .CommonLabels.alertname }}'
        text: >-
          {{ range .Alerts }}• {{ .Annotations.summary }}
          {{ if .Annotations.description }}{{ .Annotations.description }}{{ end }}
          {{ end }}
  - name: slack-critical
    slack_configs:
      - channel: '#zzol-alerts-critical'  # ⚠️ 실제 채널명 확정
        send_resolved: true
        title: '🚨 [CRITICAL] {{ .CommonLabels.alertname }}'
        text: >-
          {{ range .Alerts }}• {{ .Annotations.summary }}
          {{ if .Annotations.description }}{{ .Annotations.description }}{{ end }}
          {{ end }}
```

**(d) conf/rules/alerts-infra.yml**
```yaml
groups:
  - name: infra
    rules:
      - alert: AppInstanceDown
        expr: up{job=~"prod-app|dev-app"} == 0
        for: 1m
        labels: { severity: critical }
        annotations:
          summary: "{{ $labels.job }} 인스턴스 다운 ({{ $labels.instance }})"
      - alert: ExporterTargetDown
        expr: up{job!~"prod-app|dev-app"} == 0
        for: 5m
        labels: { severity: warning }
        annotations:
          summary: "스크랩 타깃 다운: {{ $labels.job }} ({{ $labels.instance }})"
```

**(e) conf/rules/alerts-app.yml** ⚠️ `uri`/`job` 값은 0.5 검증 후 확정
```yaml
groups:
  - name: app-http
    rules:
      - alert: HighHttp5xxRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5..",job="prod-app"}[5m]))
          / clamp_min(sum(rate(http_server_requests_seconds_count{job="prod-app"}[5m])), 1) > 0.05
        for: 5m
        labels: { severity: critical }
        annotations:
          summary: "prod 5xx 비율 5% 초과"
      - alert: WsHandshakeFailure
        # ⚠️ uri=~ 패턴을 0.5-(2) 결과로 교체 (예: "/ws/info", "/ws/**")
        expr: |
          sum(rate(http_server_requests_seconds_count{uri=~"/ws.*",status!~"2..",job="prod-app"}[5m]))
          / clamp_min(sum(rate(http_server_requests_seconds_count{uri=~"/ws.*",job="prod-app"}[5m])), 1) > 0.2
        for: 3m
        labels: { severity: critical }
        annotations:
          summary: "WebSocket 핸드셰이크 실패율 급증 — nginx Upgrade/Connection 헤더 확인"
  - name: app-ipblock
    rules:
      # 배경 인시던트 직격: 전원 내부 IP 인식 → 대량 차단
      - alert: MassIpBlockingSpike
        expr: sum(rate(ip_block_request_blocked_total{job="prod-app"}[5m])) > 1   # ⚠️ 0.5-(3) 베이스라인으로 임계 튜닝
        for: 2m
        labels: { severity: critical }
        annotations:
          summary: "대량 IP 차단 급증 — nginx X-Forwarded-For 소실/내부 IP BAN 의심"
          description: "정상 트래픽이 단일(내부) IP로 인식돼 차단되는 패턴일 수 있음. nginx proxy 헤더와 conf 파일 상태를 점검."
      - alert: NewIpBanSpike
        expr: sum(rate(ip_block_new_total{job="prod-app"}[5m])) > 0.5   # ⚠️ 튜닝
        for: 2m
        labels: { severity: warning }
        annotations:
          summary: "신규 IP BAN 급증"
```

**(f) conf/rules/alerts-redis-stream.yml** ⚠️ 임계값 튜닝 필요
```yaml
groups:
  - name: redis-stream
    rules:
      - alert: RedisStreamHighE2ELatency
        expr: histogram_quantile(0.95, sum(rate(redis_stream_e2e_latency_seconds_bucket[5m])) by (le)) > 1
        for: 5m
        labels: { severity: warning }
        annotations:
          summary: "Redis Stream E2E p95 latency 1s 초과"
      - alert: RedisStreamQueueBacklog
        expr: redis_stream_threadpool_queue_size > 100
        for: 5m
        labels: { severity: warning }
        annotations:
          summary: "Redis Stream 스레드풀 큐 적체 ({{ $value }})"
      - alert: RedisStreamBacklog
        expr: redis_stream_length > 1000
        for: 10m
        labels: { severity: warning }
        annotations:
          summary: "Redis Stream 미처리 메시지 적체 ({{ $value }})"
```

**(g) conf/rules/slo.yml** — 가용성 SLO 99.5% + multi-window burn-rate (SRE workbook 패턴)
```yaml
groups:
  - name: slo-availability
    rules:
      - record: job:http_error_ratio:5m
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5..",job="prod-app"}[5m]))
          / clamp_min(sum(rate(http_server_requests_seconds_count{job="prod-app"}[5m])), 1)
      - record: job:http_error_ratio:1h
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5..",job="prod-app"}[1h]))
          / clamp_min(sum(rate(http_server_requests_seconds_count{job="prod-app"}[1h])), 1)
      - record: job:http_error_ratio:6h
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5..",job="prod-app"}[6h]))
          / clamp_min(sum(rate(http_server_requests_seconds_count{job="prod-app"}[6h])), 1)
      # SLO=99.5% → 에러버짓 0.5%. 빠른 소진(14.4x): 1시간이면 버짓 2% 소진 수준
      - alert: ErrorBudgetFastBurn
        expr: job:http_error_ratio:5m > (14.4 * 0.005) and job:http_error_ratio:1h > (14.4 * 0.005)
        for: 2m
        labels: { severity: critical }
        annotations:
          summary: "에러버짓 빠른 소진 (가용성 SLO 99.5% 위협)"
      - alert: ErrorBudgetSlowBurn
        expr: job:http_error_ratio:6h > (6 * 0.005)
        for: 15m
        labels: { severity: warning }
        annotations:
          summary: "에러버짓 느린 소진"
```

**(h) 시크릿 처리**
```bash
# 서버 ~/monitor/ 에서 (Git 커밋 금지)
mkdir -p ~/monitor/secrets
echo 'secrets/' > ~/monitor/secrets/.gitignore   # 또는 monitoring/.gitignore에 secrets/
# webhook URL을 파일로 (앞뒤 공백/개행 없이)
printf '%s' "$SLACK_WEBHOOK_URL" > ~/monitor/secrets/slack_api_url
chmod 600 ~/monitor/secrets/slack_api_url
```
> Git에는 `secrets/.gitignore`만 올리고 `slack_api_url`은 올리지 않는다. 채널 고정 webhook이면 `channel:` 값은 무시될 수 있으니 webhook이 어느 채널로 가는지 확인.

### 3.4 Phase 2 — 정밀 알림 (앱 코드 변경, 선택/권장)
배경 인시던트를 **정확히** 잡으려면 "사설/내부 IP가 BAN됨"을 직접 알림화. 0.5-(3)에서 `ip_type` 라벨이 없으면:
- 앱 `IpBlockFilter`의 BAN/차단 Micrometer 카운터에 **`ip_type` 라벨(`private`/`public`)** 추가. 사설 대역(10/8, 172.16/12, 192.168/16, 127/8) 판별로 분류.
- 룰 추가:
```yaml
      - alert: PrivateIpBanned
        expr: increase(ip_block_new_total{ip_type="private"}[5m]) > 0
        for: 0m
        labels: { severity: critical }
        annotations:
          summary: "사설/내부 IP가 차단됨 — 절대 정상 아님 (프록시 헤더 소실 의심)"
```
> 이게 있으면 `MassIpBlockingSpike`(증상 기반)보다 오탐 적고 즉발. 단 앱 변경이라 별도 PR 권장.

### 3.5 Phase 3 — 합성 프로브 (blackbox, 선택)
`nginx -t` 통과해도 실제 동작을 확인하도록 blackbox-exporter로 `/ws/info`·헬스 엔드포인트 외부 프로브 → `probe_success == 0` 알림. (별도 exporter 추가라 후순위)

---

## 4. 검증 (Validation)
```bash
# 1) rule 문법 (로컬/CI, conf 디렉터리 기준)
docker run --rm -v "$PWD/conf:/c" prom/prometheus:v2.49.1 promtool check rules /c/rules/*.yml

# 2) alertmanager 설정 문법
docker run --rm -v "$PWD/conf:/c" prom/alertmanager:v0.27.0 amtool check-config /c/alertmanager.yml

# 3) 배포 후 prometheus 리로드 + 로드 확인 (네트워크 내부)
docker exec prometheus wget -qO- --post-data='' http://localhost:9090/-/reload
docker exec prometheus wget -qO- 'http://localhost:9090/api/v1/rules' | head
# Prometheus UI Status > Rules / Status > Runtime(Alertmanager 연결) 확인

# 4) alertmanager 연결 + 테스트 알림 발화 → Slack 도착 확인
docker exec alertmanager amtool alert add testalert severity=critical \
  summary="alertmanager 연결 테스트" --alertmanager.url=http://localhost:9093

# 5) 실제 룰 의도 검증(예: AppInstanceDown) — dev 컨테이너 하나 잠깐 중지해 발화/해제 확인 (운영영향 없는 dev로)
```

## 5. 롤아웃 (Rollout)
1. `backend/docker/monitoring/`에 위 파일 변경을 **Git 브랜치(`be/infra/add-alertmanager` 같은 단기 feature 브랜치)로 커밋** → PR. (영구 브랜치 금지)
2. 서버 `~/monitor/`에 변경분 반영(현 운영 방식에 맞춰 복사 또는 git pull). secrets 파일은 서버에서 생성(3.3-h).
3. `docker compose up -d alertmanager` + prometheus `/-/reload`.
4. 4장 검증 전부 통과 확인.
5. **롤백**: alertmanager는 독립 컨테이너라 `docker compose stop alertmanager` + prometheus.yml의 `alerting`/`rule_files` 주석 처리 후 reload로 즉시 원복(앱·트래픽 영향 없음).

## 6. 완료 기준 (Acceptance Criteria)
- [ ] alertmanager 컨테이너 healthy, prometheus가 `alertmanager:9093` 연결(Status>Runtime).
- [ ] `promtool check rules` / `amtool check-config` 통과.
- [ ] 테스트 알림이 지정 Slack 채널에 도착(발화·해제 양쪽).
- [ ] critical 룰 중 최소 1개(`AppInstanceDown` 또는 `MassIpBlockingSpike`)가 의도대로 평가됨을 dev에서 확인.
- [ ] 정본은 Git(`backend/docker/monitoring/`)에 있고, **시크릿은 커밋되지 않음**.
- [ ] (Phase 2 수행 시) `PrivateIpBanned` 룰 동작 확인.

## 7. 주의사항 / 함정
- **Alertmanager는 config에서 `${ENV}` 확장을 하지 않는다.** 반드시 `slack_api_url_file`(파일 마운트)로 시크릿 주입. (`-config.expand-env`는 Loki/Tempo 전용, Alertmanager엔 없음)
- 호스트에서 `localhost:9090`/`9093` 안 닿는다(포트 미노출). 항상 `docker exec` 또는 네트워크 내부에서 접근.
- 모든 임계값(`> 1`, `> 100`, `> 1000`, p95 `> 1` 등)은 **추정치**다. 0.5의 베이스라인 쿼리로 정상 범위를 본 뒤 조정. 초기엔 보수적으로(오탐↓) 잡고 운영하며 좁힌다.
- `job="prod-app"`는 blue/green 두 인스턴스 합산 기준. 인스턴스별로 보고 싶으면 `by (instance)` 추가.
- **drift 주의**: 서버에 직접 수정하지 말고 Git→서버 한 방향을 유지(별도 GitOps 작업과 충돌 방지). 본 작업으로 또 다른 수기 표류를 만들지 않는다.
- `nginx -t` 통과 = 동작 정확이 아님(배경 인시던트의 교훈). 가능하면 Phase 3 합성 프로브까지 가야 구성/동작 검증이 분리된다.
- prod-promtail이 unhealthy 상태(발견 당시). 로그 기반 대응 경로 자체가 불안정하므로, 이 작업과 별개로 점검 권장.
