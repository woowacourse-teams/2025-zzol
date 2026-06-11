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
   - **Phase A (이슈 #1399):** Alertmanager 기동 + Slack 연동 + **두 엔진 어디에도 없던 신규 인시던트 룰·SLO**(대량 IP 차단, WS 핸드셰이크 실패, 에러버짓 burn-rate)를 Prometheus 룰로 작성한다. 동시에 **기존 Grafana Alerting에 Slack contact point를 추가**해, 이미 있는 8룰이 *즉시* 전달되게 한다. 이 구간은 일시적으로 엔진이 둘이지만 **둘 다 전달은 되므로** 인시던트 루프가 곧바로 닫힌다.
   - **Phase B (별도 이슈):** 8룰을 Prometheus 룰로 이관하고, 룰별 발화 동치를 검증한 뒤 Grafana Alerting provisioning을 제거한다. 신중한 검증을 동반한 통합 단계다.

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
- **`job="prod-app"`는 blue/green 두 인스턴스 합산 기준이다.** 인스턴스별 이상은 `by (instance)` 룰이 없으면 합산에 묻힐 수 있다.
- **silent failure는 알림만으로 완전히 막지 못한다.** 본 작업의 룰도 "메트릭에 드러난 증상"을 잡을 뿐이다. 구성과 동작의 검증을 분리하려면 합성 프로브(blackbox-exporter로 `/ws/info` 외부 프로브 → `probe_success == 0`)가 필요하며 별도 exporter 추가라 후순위로 둔다.
- **prod-promtail이 unhealthy 상태다(발견 당시).** 로그 기반 대응 경로가 불안정하므로 메트릭 알림이 더 중요해지는 동시에 promtail 점검이 별도로 필요하다.

## 결과

- 파일 변경 정본은 `backend/docker/monitoring/`: `conf/prometheus.yml`(`alerting`+`rule_files` 추가), `conf/alertmanager.yml`(신규), `conf/rules/*.yml`(신규), `docker-compose.yml`(alertmanager 서비스 + rules 마운트 + `alertmanager-data` 볼륨), `secrets/.gitignore`(신규). Phase A에서 기존 `provisioning/alerting/contact-points.yml`에 Slack contact point 추가, Phase B에서 `provisioning/alerting/` 정리.
- **Phase A = 이슈 #1399**, **Phase B(8룰 이관·Grafana Alerting 제거) = 이슈 #1400**으로 분리한다.
- 호스트에서 9090/9093은 미노출(`expose`만)이므로 검증·운영 접근은 `docker exec` 또는 monitoring-network 내부에서만 한다.
- 롤백은 앱·트래픽 영향이 없다: `docker compose stop alertmanager` + `prometheus.yml`의 `alerting`/`rule_files` 주석 처리 후 `/-/reload`로 즉시 원복된다.
- 브랜치는 `be/chore/1399-add-alertmanager`다.
