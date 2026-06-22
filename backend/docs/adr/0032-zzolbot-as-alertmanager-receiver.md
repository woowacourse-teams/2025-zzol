# 0032. zzol-bot을 Alertmanager 웹훅 수신기로 재배치 — 탐지 일원화 + LLM 보강 분리

- 날짜: 2026-06-22
- 상태: 승인 (팀 합의 — 구현을 본 PR에 포함)

## 컨텍스트

`be/feat/1474-zzolbot-upgrade` 브랜치의 코드 리뷰에서, zzol-bot 능동 모니터링이 이상 발생 시 `ZzolBotSlackNotifier`로 **Slack에 직접 알림을 전송**한다는 점이 [ADR-0026](0026-alertmanager-and-alerting-rules.md)과의 잠재 충돌로 지적됐다. ADR-0026은 "Alertmanager를 단일 알림 엔진으로 채택"하고 "같은 datasource를 읽는 두 엔진이 되어 알림 설정이 이중화"되는 것을 명시적 약점으로 경계한다.

표면적으로는 "Slack에 보내는 경로가 둘"로 보이지만, 코드를 보면 충돌의 본질은 더 깊다 — **탐지(detection)가 중복**이다.

- `coffeeshout.zzolbot.monitor.application.AnomalyGate`는 Loki ERROR/WARN 카운트·Prometheus 5xx에 대한 **순수 임계값 게이팅**이다. 이는 Prometheus alert rule이나 Loki ruler rule이 하는 일과 정확히 같다. 즉 zzol-bot의 결정적 탐지 레이어는 단일 룰셋과 **진짜로 중복**이며, 이것이 ADR-0026 충돌의 실체다.
- 반면 `MonitorAnalysis`(LLM이 로그 샘플을 읽어 생성하는 근본원인 가설 + 제안 조치)는 Alertmanager·PromQL이 **표현할 수 없는 유일한 비중복 가치**다.

정리하면 zzol-bot 모니터에서 ADR-0026과 부딪히는 것은 "탐지 레이어"이고, 살릴 가치가 있는 것은 "LLM 보강 레이어"다. 이 구분이 결정 범위를 좁힌다.

### 현재 구조

```text
@Scheduled(공유 풀) → MonitorScheduler → MonitorCollector
    → Loki/Prometheus 직접 폴링(수집) → AnomalyGate(임계 게이팅)
    → (이상·비쿨다운·예산) AnomalyAnalyzer(LLM) → ZzolBotSlackNotifier → Slack
```

이 구조는 두 가지 부수 문제를 동반한다.

1. **탐지가 Alertmanager 룰셋과 중복**(ADR-0026 약점 재현).
2. `MonitorCollector`가 공유 `@Scheduled` 스레드(pool size 1)에서 Loki/Prometheus를 **타임아웃 없이 동기 폴링**한다. 외부 응답이 지연되면 아웃박스 릴레이·Redis 스트림 복구 등 다른 스케줄 작업까지 굶는다(같은 리뷰의 BLOCKER 지적).

## 결정

**zzol-bot을 "병렬 알림기"에서 "Alertmanager 웹훅 수신기"로 재배치한다.** 결정적 탐지는 단일 엔진(Prometheus 룰 + 신규 Loki ruler)으로 일원화하고, zzol-bot은 Alertmanager가 발화시킨 알림을 받아 **LLM으로 보강**(근본원인 가설 + 제안 조치 + Loki 로그 샘플)한 뒤 Slack에 게시한다.

```text
Prometheus 룰 + Loki ruler  →  Alertmanager (그룹·억제·silence)
                            →  webhook  →  zzol-bot 보강(LLM + Loki 샘플)  →  Slack
```

세부 결정:

1. **탐지를 단일 엔진으로 되돌린다 — 옛 self-poll 5개 신호를 빠짐없이 이관한다.** 자체 폴링이 보던 신호와 단일 엔진 대응은 다음과 같다(누락 방지):
   - **HTTP 5xx**(`http5xx-threshold`) → 기존 Prometheus 룰 `Http5xxRatioHigh`(alerts-app.yml). 이미 커버.
   - **Stream backlog**(`stream-backlog-threshold`) → 기존 Prometheus 룰 `RedisStreamBacklogHigh`(`max(redis_stream_length) > 1000`). 이미 커버 — self-poll은 이걸 중복으로 본 것이라 제거가 정확.
   - **ERROR/WARN 로그**(`error-log-threshold`/`warn-log-threshold`) → 신규 **Loki ruler 룰**(`count_over_time(... |= "ERROR" [w]) > N`). 율 신호.
   - **DLQ 적체**(`dead-letter-threshold`) → 동등 메트릭·룰이 **없어 누락 위험이 있었다(코드 리뷰에서 발견).** 깊이 신호라 로그 율 룰로는 느린 누적을 못 잡으므로, `outbox_dead_letter_count` Micrometer 게이지(`OutboxDeadLetterMetricService`, `redis_stream_length` 패턴 미러)를 신설하고 Prometheus 룰 `OutboxDeadLetterHigh`(`> 10`, 옛 임계 보존)로 복구했다.

   Alertmanager가 평가 커버리지·그룹화·silence·inhibition의 단일 주체로 남는다.

2. **zzol-bot은 웹훅 수신기로만 동작한다.** `POST /internal/zzolbot/alerts/webhook` 하나를 노출하고, Alertmanager의 `webhook_config` receiver가 firing 알림을 여기로 보낸다. zzol-bot은 `status=firing` 알림만 보강해 Slack에 enriched 메시지로 게시한다. `AnomalyGate`·`MonitorCollector`의 자체 수집·게이팅은 단계적으로 제거한다.

3. **부수 효과로 폴링 기아(starvation) 위험이 해소된다.** zzol-bot이 더 이상 공유 `@Scheduled` 스레드에서 Loki/Prometheus를 폴링하지 않으므로, 타임아웃 부재로 스케줄러 풀이 굶는 경로 자체가 사라진다. 이 ADR을 채택하면 코드 리뷰의 BLOCKER가 별도 핫픽스 없이 구조적으로 닫힌다.

4. **앱측 쿨다운(`MonitorService.inCooldown`)은 제거한다.** 재알림 억제는 이제 Alertmanager가 `group_interval`/`repeat_interval`(현재 4h ≈ 기존 cooldown 240m)로 소유한다. 앱측 쿨다운을 남기면 이중 억제이므로 떼어내고, 비용 백스톱은 `LlmCallBudget`(일일 호출 상한)이 담당한다. 단순화이지 기능 손실이 아니다.

5. **본 PR이 전체 구현을 담는다(골격 → 전체).** 팀 합의로 ADR을 `승인` 전환하고, 결정 기록과 함께 ① Java 보강 경로(`AlertEnrichmentService`가 `AnomalyAnalyzer`·`ZzolBotSlackNotifier`·`LokiLogClient.tailErrors`·`LlmCallBudget` 재사용, 자체 폴링 `MonitorCollector`/`Scheduler`/`AnomalyGate` 제거), ② 인프라(`alertmanager.yml` nginx-경유 receiver + 베어러 인증, nginx 내부 리스너·공개 차단, `docker-compose` 네트워크 조인, `loki.yml` ruler), ③ Spring Security `/internal/**` 베어러 토큰을 함께 반영한다. 인프라는 로컬 검증 불가라 배포 시점 검증 대상이다(아래 결과).

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| 현행 유지(`ZzolBotSlackNotifier` 직접 Slack) | 추가 작업 없음 | 탐지가 Alertmanager 룰셋과 중복(ADR-0026 약점), 폴링 기아 위험 잔존, silence/inhibition 미적용 |
| **A. 웹훅 수신기(채택)** | 단일 엔진 유지, LLM 가치 보존, 폴링 기아 해소, silence/그룹화/억제가 enriched 알림에도 적용 | Alertmanager 룰 커버리지에 의존(자체 탐지 상실), 네트워크 도달성·내부 노출 차단 등 배선 필요 |
| B. zzol-bot 자체 탐지 유지 + `/api/v2/alerts` push | 전달만 일원화하면서 자체 탐지 유지 | 중복 탐지 레이어를 그대로 남김(ADR-0026 약점 미해결) — 현 게이트가 단순 임계값이라 **현 시점엔 순수 중복**. LLM 기반 *탐지*(PromQL로 표현 못 하는 의미론적 패턴) 로드맵이 생길 때만 정당화됨 |

> 대안 B는 "보강이 아니라 탐지"로 진화할 때를 위한 장래 선택지로 남긴다. 현재 `AnomalyGate`는 임계값이라 B는 중복을 제거하지 못한다.

## 트레이드오프

- **Alloy는 알림 엔진이 아니다.** "Alloy로 푼다"의 정확한 메커니즘은 **Loki ruler**다. `loki.yml`의 `common.storage.rules_directory: /loki/rules`는 있으나 `ruler:` 블록(`alertmanager_url`)이 미배선이라, ruler를 켜는 것이 곧 로그 탐지를 단일 엔진에 합류시키는 작업이다. Alloy의 역할은 그 스택에 로그를 먹이는 파이프라인이라는 점뿐이며([ADR-0027](0027-promtail-to-alloy-migration.md)·[ADR-0028](0028-alloy-pull-based-config-via-git.md)), [ADR-0030](0030-alloy-trace-metric-collection-integration.md)이 검토하는 수집 일원화와도 결이 같다.
- **웹훅 타깃은 raw 색 컨테이너가 아니라 nginx 경유다(블루-그린 + 네트워크 + 내부 차단 일괄 해결).** 앱은 `dev-app-blue`/`dev-app-green` 두 컨테이너로 뜨고 **활성 색을 가리키는 정적 별칭은 없다.** 활성 색의 단일 진실 원천은 nginx의 `docker/nginx/conf/*-service.inc`(`set $upstream http://dev-app-blue:8080;`)이며, 블루-그린 전환 시 이 `.inc`만 재작성된다(ADR-0023에서 edge-cd 동기화 제외 대상인 라이브 B/G 파일). 따라서 Alertmanager 웹훅을 `dev-app-blue`로 못박으면 green 활성 시 죽은 색으로 가 **보강이 조용히 실패한다.** 해법은 웹훅을 **nginx로 보내고 nginx가 `$upstream`(활성 색)으로 프록시**하게 두는 것이다. 이 한 수가 세 문제를 동시에 푼다 — ① 블루-그린 활성 색 자동 추종(`$upstream` 재사용, 별도 추적 로직 불필요), ② Alertmanager(`monitoring-network`)↔앱(`dev/prod-network`) 네트워크 도달, ③ 내부 노출 차단.
- **내부 노출 차단 필수.** 웹훅 엔드포인트(`/internal/zzolbot/alerts/webhook`)는 공개로 노출하면 임의 Slack 알림 주입이 가능하다. 따라서 nginx에는 **내부 전용 location**(공개 server 블록과 분리, 모니터링 네트워크에서만 도달)으로 추가하고 Spring Security로 `/internal/**`를 추가 제한한다. 공개 `proxy_pass $upstream` 경로와 같은 `$upstream` 변수를 재사용해 블루-그린 추종을 공유한다.
- **웹훅 페이로드 형태.** 수신기는 firing과 resolved를 모두 받고, 각 페이로드는 (상류에서 그룹화된) `alerts[]` 배열을 싣는다. LLM 보강은 firing에만 의미가 있으므로 배열을 순회하며 `status` 필터링한다.
- **Slack 포맷팅 소유권 이동.** enriched 알림은 zzol-bot Java가 포맷해 Alertmanager의 Slack receiver를 우회한다. 어느 알림을 어느 경로로 보낼지(Alertmanager 직접 Slack vs zzol-bot enriched)를 라우팅(`continue`)으로 명시 설계해야 중복·누락이 없다.
- **자체 탐지 상실.** zzol-bot은 Alertmanager 룰이 발화한 것만 본다. PromQL/LogQL로 표현 못 하는 이상은 (대안 B의 장래 선택지 전까지) 탐지 범위 밖이다.

## 결과

- **상태는 `승인`이다.** ADR-0026(단일 알림 엔진)·ADR-0030(수집 일원화)을 건드리는 팀 레벨 결정으로 합의됐고, 구현을 본 PR에 포함한다.
- **이 PR의 변경 지점 — Java(로컬 검증됨):**
  - 신규: `monitor/ui/AlertmanagerWebhookController`·`AlertmanagerWebhookRequest`(DTO), `monitor/application/FiringAlertEnricher`(포트)·`AlertEnrichmentService`(구현), `monitor/domain/FiringAlert`.
  - 리팩터: `AnomalyAnalyzer`/`GeminiAnomalyAnalyzer`/`ZzolBotSlackNotifier`/`MonitorRunEntity.of`를 `MonitorSnapshot` 대신 **알림 컨텍스트** 입력으로, `MonitorService`는 조회 전용으로 슬림화, `ZzolBotMonitorController`에서 수동 트리거(`POST /run`) 제거, `LokiLogClient`는 `tailErrors`만 유지, `MonitorProperties` 축소(`enabled`·`errorLogWindowMinutes`), `zzolbot.html` 대시보드 읽기 전용.
  - 삭제: `MonitorCollector`·`MonitorScheduler`·`AnomalyGate`·`PrometheusMetricClient`·`MonitorSnapshot`/`MonitorSignal`/`AnomalyVerdict`·`LoggingFiringAlertEnricher`(+관련 테스트).
  - **DLQ 신호 복구**: `:infra` `OutboxDeadLetterMetricService`(신규, `outbox_dead_letter_count` 게이지) + `OutboxEventRepository.countByStatus`. self-poll 제거로 빠진 DLQ "적체 깊이" 신호를 단일 엔진 메트릭으로 되살린다(코드 리뷰 발견).
- **이 PR의 변경 지점 — 인프라(배포 시점 검증):**
  - `docker/monitoring/conf/alertmanager.yml`: `zzolbot-enrich` receiver를 `http://nginx:8889/internal/...` + 베어러 인증(`credentials_file`)으로 지정, firing 미러링 route.
  - `docker/monitoring/conf/rules/alerts-redis-stream.yml`: `OutboxDeadLetterHigh` 룰(`max(outbox_dead_letter_count) > 10`) 신규. `promtool check rules` 통과.
  - `docker/nginx/conf/internal.conf`(신규): 8889 내부 전용 리스너가 `$upstream`(prod-service.inc=활성 색)으로 프록시. `dev.conf`·`prod.conf` 공개 블록은 `/internal/` 404로 차단.
  - `docker/nginx/docker-compose.yml`: nginx를 `monitoring-network`에 조인 + 8889 `expose`(호스트 미노출).
  - `docker/monitoring/conf/loki.yml`: ruler 블록(미배선이던 `ruler:` 활성화).
  - Spring Security: `/internal/**` 전용 체인(Order 0)이 베어러 토큰 검증.
- **배포 시점 검증/결정(로컬 불가):**
  - **시크릿**: 서버에서 `~/monitor/secrets/zzolbot_webhook_token` 생성(`slack_api_url`과 동일: `chown 65534:65534`·`chmod 600`)하고, 앱 env `ZZOL_BOT_ALERT_WEBHOOK_TOKEN`에 동일 값 주입.
  - **도달성**: alertmanager → `nginx:8889` 도달(`monitoring-network` 조인)과 nginx → 활성 색 프록시를 `docker exec`로 확인.
  - **환경 라우팅 가정**: 내부 리스너는 현재 **prod 활성 색**(`prod-service.inc`)으로 보낸다(zzol-bot 모니터는 prod 인시던트 중심). dev 보강이 필요하면 `dev-service.inc`를 include한 8890 리스너를 동일 패턴으로 추가하고, `alertmanager.yml`에서 `job` 라벨로 분기한다.
  - **Loki ruler 룰 마운트**: `docker-compose`에 `./conf/loki-rules:/etc/loki/rules:ro` 마운트와 테넌트 `fake/` 하위 배치가 필요(별도 후속 — 본 PR은 ruler 설정·룰 파일까지).
  - 배포는 edge-cd(ADR-0023) 위에서 진행하며 서버 직접 수정 금지(ADR-0026 계승). `*-service.inc`는 edge-cd 동기화 제외(라이브 B/G) 대상임에 유의.
- **롤백.** 인프라는 alertmanager `zzolbot-enrich` route/receiver 제거 + nginx `internal.conf` 제거 + 리로드로 원복(앱·트래픽 영향 없음). Java는 자체 폴링 제거를 포함하므로, 되돌리려면 본 커밋 revert.
- 브랜치는 `be/chore/zzolbot-alertmanager-receiver-adr`이며 PR 타깃은 `be/feat/1474-zzolbot-upgrade`(#1492)다.
