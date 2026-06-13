# 0026. Alertmanager 단일 알림 엔진 채택 — Grafana Alerting 통합과 2단계 롤아웃

- 날짜: 2026-06-11
- 상태: 제안

## 컨텍스트

이 작업의 직접 계기는 [ADR-0023](0023-gitops-edge-config-reconciliation.md)을 촉발한 것과 동일한 인시던트다. nginx proxy 블록(`X-Forwarded-For` + WebSocket `Upgrade`/`Connection`)이 설정 파일에서 소실되어, ① `/ws/info` 핸드셰이크가 실패하고 ② `X-Forwarded-For` 소실로 Spring이 전 요청을 nginx 컨테이너 내부 IP로 인식 → `IpBlockFilter`가 그 내부 IP를 BAN → **전 트래픽이 차단**되었다. 그런데 `nginx -t`는 통과(블록이 "없는" 것은 문법 오류가 아님)하는 **silent failure**였고, 외부로 전달되는 알림이 없어 사람이 로그에서 내부 IP BAN을 육안으로 보고서야 발견했다. ADR-0023은 마지막에 "향후 Alertmanager 도입은 이 워크플로우(edge-cd) 위에서 진행한다"고 본 작업을 예고했다.

작업을 시작하며 현재 상태를 실측하자, 출발 전제를 수정해야 했다. **알림이 "전혀 없는" 것이 아니라, 알림 엔진은 이미 있으나 전달 채널이 비어 있었다.**

- `backend/docker/monitoring/provisioning/alerting/`에 **Grafana Alerting 룰 8개가 provisioning-as-code로 존재**한다(WS 연결 실패, WS 처리 지연, JVM 힙, DB 커넥션 풀, Redis 지연, 서킷브레이커 OPEN, 디스크, CPU). 임계값은 부하 테스트 실측 기반으로 문서화되어 있다.
- 그러나 `contact-points.yml`은 `contactPoints: []`이고("외부 알림 채널 없음"), `notification-policies.yml`은 `grafana-default-email`로 라우팅한다. **즉 룰은 평가되어 Grafana UI에만 뜨고 외부로는 아무것도 전달되지 않는다.**
- 한편 인시던트를 직접 잡을 룰(내부 IP 대량 BAN, WS 핸드셰이크 실패율, 가용성 SLO 에러버짓)은 **두 엔진 어디에도 없다** — 새로 작성해야 한다.

정리하면 공백은 둘이다: **(a) 전달 채널 부재**, **(b) 인시던트 전용 룰·SLO 부재**. 그리고 이미 Grafana Alerting이라는 엔진이 코드로 깔려 있으므로, 핵심 결정은 "알림 룰을 어디서 평가·전달하는가 — 엔진을 몇 개 둘 것인가"가 된다.

현재 수집 중인(확인된) 메트릭은 다음과 같다. 알림 룰은 이 위에서만 성립한다.

- `http_server_requests_seconds_count`/`_sum`/`_max` — Spring actuator. 라벨: `uri`, `status`, `method`, `job`.
- `ip_block_new_total`(신규 BAN), `ip_block_request_blocked_total`(차단된 요청).
- `redis_stream_e2e_latency_seconds_bucket`(버킷 있음 → 분위수 계산 가능), `redis_stream_length`, `redis_stream_threadpool_*`.
- 기존 Grafana 룰이 쓰는 메트릭: `jvm_memory_*`, `hikaricp_connections_active`, `resilience4j_circuitbreaker_state`, `node_*`, `websocket_connections_failed_total`, `websocket_message_inbound_time_seconds`.

## 결정

**Alertmanager를 단일 알림 엔진으로 채택하고, 기존 Grafana Alerting을 그 위로 통합한다. 단 커버리지 공백이 생기지 않도록 2단계로 롤아웃한다.**

1. **알림 엔진을 Alertmanager + Prometheus 룰 파일로 일원화한다.** Grafana Alerting을 남긴 채 Alertmanager를 얹으면 **같은 datasource를 읽는 두 엔진**이 되어 룰셋·알림 설정이 이중화된다(초기 검토안의 약점). 대신 Grafana는 시각화 본업으로 환원하고, 알림 룰은 모두 `conf/rules/*.yml`에 PromQL 텍스트로 둔다. 근거: PromQL `expr`는 Grafana 룰 모델(룰당 refId A/B/C 약 40줄)보다 간결하고, `promtool check rules`/`amtool check-config`로 CI·로컬 사전 검증이 가능하며, SLO 레코딩 룰과 multi-window burn-rate가 네이티브이고, Alertmanager의 그룹화·억제(inhibition)·silence가 인시던트 대응에서 더 강하다. 기존 8룰의 임계값(부하 테스트 실측)은 버리지 않고 PromQL로 이식한다.

2. **2단계 롤아웃으로 어느 시점에도 알림 커버리지 공백을 만들지 않는다.**
   - **Phase A (이슈 #1399):** Alertmanager 기동 + Slack 연동 + **두 엔진 어디에도 없던 신규 인시던트 룰·SLO**(대량 IP 차단, WS 핸드셰이크 실패, 에러버짓 burn-rate)를 Prometheus 룰로 작성한다.
   - **Phase B (별도 이슈):** 8룰을 Prometheus 룰로 이관하고, 룰별 발화 동치를 검증한 뒤 Grafana Alerting provisioning을 제거한다. 신중한 검증을 동반한 통합 단계다.

   > **실측에 의한 Phase A 범위 수정(2026-06-11):** 당초 Phase A는 "기존 Grafana Alerting에 Slack contact point를 추가해 8룰을 *즉시* 전달"하려 했고, 이 구간은 엔진이 둘이어도 둘 다 전달되므로 인시던트 루프가 곧 닫힌다고 봤다. 그러나 구현 중 서버를 실측하자 두 가지가 드러났다. ① Grafana provisioning의 `alerting/`이 `alerting_bak`으로 rename돼 **코드 관리가 끊긴 상태**다(8룰은 Grafana DB에 남아 평가·UI 표시는 되나 전달 채널이 비어 아무 데도 안 감). ② 레포의 `provisioning/alerting/`은 컨테이너 마운트 소스(`data/grafana/provisioning`, gitignore)와 edge-cd 동기화(`conf/`만) **어디에도 닿지 않는 orphan**이다. 8룰은 Phase B에서 Prometheus로 이관·삭제될 예정이므로 **곧 지울 Grafana provisioning 배관을 Phase A에서 복구하지 않기로 한다.** Phase A는 Alertmanager 신규 룰만 활성화하고, 8룰 커버리지 복원은 Phase B의 Prometheus 이관으로 일원화한다. (8룰을 당장 전달해야 하면 Grafana UI에서 contact point를 수동 추가하는 운영 조치로 대응하고 코드는 건드리지 않는다.)

3. **이관의 핵심 제약: `noDataState: Alerting`은 Prometheus에 등가물이 없다.** 기존 룰 중 JVM 힙·WS 지연·서킷브레이커·CPU는 `noDataState: Alerting`(데이터가 없으면 발화) 의미를 가진다. Prometheus는 시계열이 사라지면 룰을 **그냥 발화하지 않는다.** 그대로 이관하면 이 4개가 **조용히 알림을 멈춘다** — 본 프로젝트가 반응하는 바로 그 silent failure다. 따라서 Phase B에서 이들은 반드시 `absent()` 동반 룰을 명시적으로 추가해야 동치가 된다. `noDataState: OK`(WS 실패·DB 풀·Redis 지연·디스크)는 단순 이식이다.

4. **시크릿(Slack webhook)은 파일 마운트로 주입한다.** `alertmanager.yml`의 `global.slack_api_url_file`로 참조하고 실제 파일은 서버에서 생성·`chmod 600`한다. **Alertmanager는 config에서 `${ENV}` 확장을 하지 않으므로**(`-config.expand-env`는 Loki/Tempo 전용) compose env·`${SLACK_URL}` 치환은 동작하지 않는다. Git에는 `secrets/.gitignore`만 올리고 webhook URL은 커밋하지 않는다(ADR-0023 monitoring `.env` 정책과 동일).

5. **인시던트 룰은 증상 기반(우선)에서 라벨 기반(정밀)으로 발전시킨다.** 앱 코드 변경 없이 기존 메트릭만으로 `MassIpBlockingSpike`(`ip_block_request_blocked_total` 급증 — 전 트래픽이 단일 내부 IP로 인식돼 차단되는 패턴), `WsHandshakeFailure`(ws uri 실패율 급증)를 먼저 만든다. 이후 별도 PR(앱 변경)로 `IpBlockFilter`의 Micrometer 카운터에 `ip_type`(private/public) 라벨을 추가해 `PrivateIpBanned`(사설/내부 IP가 BAN되면 즉발)로 정밀화한다. 사설 IP BAN은 정상이 아니므로 증상 기반보다 오탐이 적다. (이 라벨 추가는 ws 핸드셰이크와 함께 기존 `websocket_connections_failed_total` 룰과 중복되지 않는지 확인한 뒤 작성한다.)

6. **정본은 Git에 두고 배포는 edge-cd 위에서 진행한다(ADR-0023 계승).** 진실 공급원은 `backend/docker/monitoring/`이며 서버 `~/monitor/`를 직접 수기 수정하지 않는다. 단 0023 기준 monitoring은 아직 CD 동기화 대상이 아니므로, **이 provisioning이 서버에 실제로 떠 있는지는 배포 시점에 확인**한다(미확인 상태).

## 고려한 대안

| 대안                                            | 장점                                           | 단점                                                         |
|-----------------------------------------------|----------------------------------------------|------------------------------------------------------------|
| Grafana Alerting 확장(단일·Grafana 유지)            | 새 컨테이너 없음, 기존 8룰 재사용                         | 룰 모델 verbose(룰당 ~40줄), `promtool` 검증 불가, burn-rate 작성 까다로움 |
| Alertmanager 추가 + Grafana Alerting 유지(2엔진 병렬) | 각 엔진 강점 활용                                   | 같은 datasource 두 번 평가, 룰셋·알림 설정 이중화·중복 — 초기안, 기각            |
| 빅뱅 통합(한 PR에 신규룰+이관+제거)                        | 끝나면 완전 일원화                                   | 범위 3~4배 폭발, 이관 중 커버리지 공백·검증 부담이 인시던트 대응을 인질로 잡음            |
| Alertmanager 단일화 + 2단계 롤아웃 (채택)               | 일원화의 일관성 + 어느 시점에도 전달 공백 없음, 검증을 Phase B로 격리 | 일시적 2엔진 구간(중복 알림 가능), 이관 검증 비용                             |
| 외부 SaaS(PagerDuty 등)                          | 운영 부담 최소                                     | 비용·외부 의존, OCI 단일 인스턴스 규모에 과함                               |
| 시크릿을 compose env(`${SLACK_URL}`)로 주입          | 익숙한 방식                                       | Alertmanager가 config env 확장을 안 해 **동작 불가**                 |

## 트레이드오프

- **Phase A~B 사이 일시적으로 엔진이 둘이다.** 둘 다 전달되므로 공백은 없지만 겹치는 관심사(예: WS 관련)에서 중복 알림이 날 수 있다. 이 비용을 받아들이는 대신 인시던트 루프를 가장 빨리 닫고, Phase B 완료로 중복을 해소한다. 빅뱅이 회피하는 중복을 잠깐 허용하는 대가로 커버리지 공백 위험을 없앤다.
- **이관 검증은 룰별로 비싸다.** "이관이 동작을 보존했는가"는 각 룰을 실제로 발화시켜 확인해야 하고, `noDataState: Alerting` 4개는 `absent()` 동반 룰까지 짜야 한다. Phase B의 수용 기준이 그만큼 높다.
- **모든 신규 임계값은 추정치다.** 대량 BAN·WS 실패·burn-rate 임계는 베이스라인 미확인 상태의 보수적 추정이다. 배포 전 Prometheus API로 정상 범위를 측정해 조정하고, 초기엔 오탐을 줄이는 방향(임계 높게)으로 잡은 뒤 좁힌다.
- **`job="prod-app"`는 blue/green 합산 기준이며, 블루-그린은 평소 한 색만 활성이다.** SLO·`AppInstanceDown` 모두 job 합산으로 평가한다. (정정: 초기엔 `AppInstanceDown`을 `by (instance)`로 짜 인스턴스별 이상을 잡으려 했으나, 유휴 색 타깃의 `up=0`이 영구 오탐을 냈다. 활성 색이 1대라 인스턴스 소실 == 서비스 소실이므로 `sum by (job)(up) == 0`으로 변경했고, 합산이 마스킹하는 이상은 없다. → `alerts-infra.yml`)
- **silent failure는 알림만으로 완전히 막지 못한다.** 본 작업의 룰도 "메트릭에 드러난 증상"을 잡을 뿐이다. 구성과 동작의 검증을 분리하려면 합성 프로브(blackbox-exporter로 `/ws/info` 외부 프로브 → `probe_success == 0`)가 필요하며 별도 exporter 추가라 후순위로 둔다.
- **prod-promtail이 unhealthy 상태다(발견 당시).** 로그 기반 대응 경로가 불안정하므로 메트릭 알림이 더 중요해지는 동시에 promtail 점검이 별도로 필요하다.

## 결과

- 파일 변경 정본은 `backend/docker/monitoring/`: `conf/prometheus.yml`(`alerting`+`rule_files` 추가), `conf/alertmanager.yml`(신규), `conf/rules/*.yml`(신규), `docker-compose.yml`(alertmanager 서비스 + rules 마운트 + `alertmanager-data` 볼륨), `secrets/.gitignore`(신규). **Phase A는 Grafana provisioning을 건드리지 않는다**(위 범위 수정 참조). Phase B에서 8룰을 Prometheus로 이관하며 `provisioning/alerting/`(현재 서버에서 `alerting_bak`으로 비활성, 레포 정본은 마운트·CD 어디에도 닿지 않는 orphan)을 정리한다.
- **Phase A = 이슈 #1399**, **Phase B(8룰 이관·Grafana Alerting 제거) = 이슈 #1400**으로 분리한다.
- 호스트에서 9090/9093은 미노출(`expose`만)이므로 검증·운영 접근은 `docker exec` 또는 monitoring-network 내부에서만 한다.
- 롤백은 앱·트래픽 영향이 없다: `docker compose stop alertmanager` + `prometheus.yml`의 `alerting`/`rule_files` 주석 처리 후 `/-/reload`로 즉시 원복된다.
- 브랜치는 `be/chore/1399-add-alertmanager`다.

## 배포 시 수동 적용 (Phase A 운영 체크리스트)

Git 정본(`backend/docker/monitoring/`)은 edge-cd 워크플로우가 서버 `~/monitor/`로 동기화하지만, **시크릿과 실측 검증은 코드로 자동화되지 않는다.** 아래는 사람이 서버에서 직접 해야 하는 작업이다. 순서를 지킨다 — 시크릿이 없으면 alertmanager 컨테이너가 기동에 실패한다.

### 1. 배포 전 — 시크릿·환경변수 준비 (필수)

Phase A는 **Alertmanager만** Slack에 연동한다(기존 Grafana 8룰 전달은 Phase B로 미룬다 — 위 "실측에 의한 Phase A 범위 수정" 참조).

- **Slack webhook 발급**: Incoming Webhook을 **`#problem` 채널**로 발급한다. `alertmanager.yml`이 receiver에서 채널을 명시하지만, webhook 자체도 같은 채널로 발급해 두면 일관된다.
- **Alertmanager용 — 파일 마운트**: 서버에서 webhook URL을 파일로 만든다. `secrets/` 디렉터리는 Git에 `.gitignore`만 올라가므로 rsync로 동기화되지 않는다. 직접 생성한다.

```bash
mkdir -p ~/monitor/secrets
printf '%s' 'https://hooks.slack.com/services/XXX/YYY/ZZZ' > ~/monitor/secrets/slack_api_url
# prom/alertmanager 이미지는 nobody(uid 65534)로 실행된다. 파일 소유자를 컨테이너 uid에
# 맞춰야 발송 시점에 읽을 수 있다. chmod 600만 하고 ubuntu 소유로 두면 컨테이너의 nobody가
# 읽지 못한다(아래 주의 참조).
sudo chown 65534:65534 ~/monitor/secrets/slack_api_url
sudo chmod 600 ~/monitor/secrets/slack_api_url
```

> **주의 — `permission denied`로 알림만 누락되는 함정.** `slack_api_url_file`은 컨테이너
> *기동 시점이 아니라 알림 발송 시점*에 읽힌다. 따라서 소유자를 컨테이너 uid(65534)에 맞추지
> 않으면 alertmanager는 정상 기동하고 `/-/healthy`도 통과하지만, 실제 발송에서만
> `open /etc/alertmanager/secrets/slack_api_url: permission denied`로 조용히 실패한다.
> 원인은 Alertmanager 로그(`docker logs alertmanager`)의 `Notify for alerts failed`에만 남는다.
> 단일 사용자 서버라 host 노출이 무방하면 `chmod 644`로도 풀 수 있으나, webhook은 민감정보이므로
> 소유자를 65534로 맞추고 600을 유지하는 편을 권장한다. 파일은 발송마다 새로 읽히므로 수정 후
> 컨테이너 재시작은 불필요하다.

### 2. 배포 — 적용

- `backend/docker/monitoring/**` 변경이 `be/dev`에 머지되면 edge-cd가 `~/monitor/`로 동기화 → `docker compose up -d`(alertmanager 신규 기동, prometheus 재생성) → `docker kill -s HUP prometheus`(룰 리로드)를 수행한다.
- monitoring은 edge-cd에서 **best-effort**라 실패해도 워크플로우는 성공으로 뜬다. 아래 검증으로 실제 적용 여부를 직접 확인한다.

### 3. 배포 후 — 검증 (성공 기준)

호스트에서 9090/9093은 미노출이므로 `docker exec`로 접근한다.

- **alertmanager healthy + prometheus 연결**:

```bash
docker exec prometheus wget -qO- http://alertmanager:9093/-/healthy
docker exec prometheus wget -qO- http://localhost:9090/api/v1/alertmanagers
```

- **룰 로드 확인**: `docker exec prometheus wget -qO- http://localhost:9090/api/v1/rules` 로 14개 룰이 보이는지 확인.
- **테스트 알림 → `#problem` 도착**:

```bash
docker exec alertmanager amtool alert add alertname=test severity=critical \
  --alertmanager.url=http://localhost:9093 \
  --annotation='summary="배포 검증 테스트"'
```

- amtool에는 `-a` 단축 플래그가 없다(롱폼 `--annotation`만). 첫 인자도 라벨이므로 `alertname=` 을 명시한다. 어노테이션 값에 공백/한글이 있으면 셸용 작은따옴표 + amtool UTF-8 파서용 큰따옴표로 이중 인용한다. 주입 후 `amtool alert query --alertmanager.url=http://localhost:9093`로 큐 적재를 확인한다.

- **critical 룰 실평가**: dev 앱 컨테이너 하나를 중지해 `AppInstanceDown`이 발화 → 재기동 시 해제 알림까지 양쪽 도착하는지 확인.

### 4. 배포 후 — 임계값 보정 (모든 신규 임계는 추정치)

배포 직후 Prometheus API로 정상 베이스라인을 측정해 `conf/rules/*.yml`의 임계를 조정한다. 특히:

- **WS `uri` 라벨 실측**: `http_server_requests_seconds_count`의 `uri` 라벨이 `/ws/info`인지 `/ws/**`인지 확인. `WsHandshakeFailure`의 `uri=~"/ws.*"`가 실제 라벨과 안 맞으면 룰이 발화하지 않는다.
- **`ip_block_*` 베이스라인**: 정상 차단량을 재고 `MassIpBlockingSpike`(5 req/s)·`IpBanRateSpike`(1/s) 임계를 보정. 초기엔 오탐을 줄이는 방향(높게)으로 둔다.
- **Redis Stream**: `redis_stream_e2e_latency`·`redis_stream_length`의 정상 분위수/길이 확인 후 0.5s·1000 임계 조정.

### 5. 롤백

앱·트래픽 영향 없음. `docker compose stop alertmanager` 후 `prometheus.yml`의 `alerting`/`rule_files`를 주석 처리하고 `docker kill -s HUP prometheus`(또는 `/-/reload`)로 즉시 원복한다.

## 배포 시 수동 적용 (Phase B 운영 체크리스트)

Phase B(이슈 #1400, PR #1412)는 기존 Grafana Alerting 8룰을 `conf/rules/alerts-migrated.yml`로 신규 편입하고 orphan `provisioning/alerting/`을 제거한다. 이 8룰은 전달된 적이 없으므로(orphan/비활성) 검증 기준은 발화 "동치"가 아니라 **정의 충실성 + 발화 가능성**이다. 9090/9093은 호스트 미노출이라 아래는 `docker exec`로 서버에서 직접 해야 한다.

### 1. 배포 — 적용

- `backend/docker/monitoring/conf/rules/**` 변경이 `be/dev`에 머지되면 edge-cd가 `~/monitor/`로 동기화 → `docker kill -s HUP prometheus`(룰 리로드). `alertmanager.yml`은 변경 없으므로 시크릿 추가 작업은 불필요(Phase A에서 완료).
- monitoring은 edge-cd에서 best-effort라 실패해도 워크플로우는 성공으로 뜬다. 아래 검증으로 실제 적용을 직접 확인한다.

### 2. 배포 후 — 룰 로드·발화/해제 검증

- **룰 로드 확인**: `docker exec prometheus wget -qO- http://localhost:9090/api/v1/rules` 로 본 Phase B 12룰(8 + absent 4)이 추가돼 **총 26룰**(Phase A 14 + Phase B 12)이 보이는지 확인.
- **발화/해제 실측**: dev 환경에서 룰 1건을 의도적으로 발화시켜(예: 앱 컨테이너 중지 → `CircuitBreaker`/`Jvm` 계열 또는 부하로 `WsInboundLatency`) `#problem` 도착 → 해제까지 양쪽 확인.

### 3. 배포 후 — 정의 충실성·발화 가능성 검증 (추측 금지, 서버 실측 필수)

로컬 `promtool check rules`는 문법·PromQL 파싱만 검증한다. "룰이 실제로 발화 가능한가"는 메트릭 실재에 달려 있어 서버에서만 확인된다.

- **P0 — JVM 힙 발화 가능성**: `docker exec prometheus wget -qO- 'http://localhost:9090/api/v1/query?query=jvm_memory_max_bytes{area="heap"}'` 로 시리즈별 값 확인. **G1GC는 Eden/Survivor의 max를 `-1`(미정의)로 노출**하는 경우가 흔하다. 이 경우 합산 분모가 오염돼 `JvmHeapUsageHigh`(critical)가 **조용히 안 터지고**(absent 동반룰도 메트릭이 present라 못 잡음) — 이 ADR이 싸우는 바로 그 silent failure다. 룰은 분모에 `> 0` 필터로 정의된 max만 합산하게 방어했으나, **전 풀이 -1이면 분모가 공집합 → 무발화**이므로 정의된 max가 최소 1개(보통 G1 Old Gen) 존재하는지 반드시 확인한다.
- **redis 메트릭 실재**: `redis_commands_duration_seconds_total`·`redis_commands_processed_total`이 redis_exporter에서 실제 노출되는지, `cmd` 라벨 유무 확인. 없으면 `RedisCommandLatencyHigh`가 발화하지 않는다.
- **WS 메트릭 idle 거동**: `websocket_message_inbound_time_seconds{quantile}`·`_count`가 저트래픽 dev에서 사라지는지 확인. `WsInboundLatencyMetricAbsent`가 idle에 오탐하면 `and on() up{job=~"dev-app|prod-app"} == 1` 게이팅을 추가한다.

### 4. 배포 후 — 임계값 보정 (모든 임계는 추정치)

8룰의 임계는 원본 부하 테스트 실측값을 보존했으나 베이스라인 재측정 없이 옮겼다. 배포 직후 Prometheus API로 정상 범위를 재고 `alerts-migrated.yml`의 임계(힙 85%·WS p99 0.5s·DB 8·Redis 50ms·디스크 85%·CPU 90%)를 조정한다. 초기엔 오탐을 줄이는 방향(높게)으로 둔다.

### 5. 소음 후속 — absent 동반룰 중첩

`JvmHeapMetricAbsent`·`CircuitBreakerMetricAbsent`는 Phase A `AppInstanceDown`(`up == 0`)과, `NodeCpuMetricAbsent`는 `MonitoringTargetDown{job="node"}`과 조건이 겹친다(메트릭 소스가 죽으면 양쪽 발화). §3이 absent 동반룰을 명시 요구하므로 제거하지 않고, 대신 한 사건이 critical을 증폭하지 않게 **`alertmanager.yml`에 `inhibit_rules`를 함께 추가했다** — `AppInstanceDown`이 같은 `job`의 absent 동반룰 3개를, `MonitoringTargetDown`(node)이 `NodeCpuMetricAbsent`를 억제한다(`equal: ['job']`로 dev↔prod 교차 억제 방지). 배포 후 실제 소음 패턴을 보고 매처를 조정한다.

### 6. 롤백

`conf/rules/alerts-migrated.yml`만 제거(또는 `prometheus.yml` rule_files에서 제외) 후 `docker kill -s HUP prometheus`로 즉시 원복. 앱·트래픽 영향 없음. orphan provisioning 삭제는 런타임에 닿지 않았으므로 롤백 불필요.
