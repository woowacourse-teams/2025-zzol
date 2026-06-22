# 0032. zzol-bot을 Alertmanager 웹훅 수신기로 재배치 — 탐지 일원화 + LLM 보강 분리

- 날짜: 2026-06-22
- 상태: 승인 (팀 합의 — 구현을 본 PR에 포함)

## 컨텍스트

`be/feat/1474-zzolbot-upgrade` 코드 리뷰에서, zzol-bot이 이상 발생 시 `ZzolBotSlackNotifier`로 **Slack에 직접 알림을 보낸다**는 점이 [ADR-0026](0026-alertmanager-and-alerting-rules.md)과 충돌로 지적됐다. ADR-0026은 Alertmanager를 **단일 알림 엔진**으로 두기로 했는데, zzol-bot이 두 번째 알림 경로가 되기 때문이다.

겉으로는 "Slack 경로가 둘"로 보이지만, 진짜 문제는 **탐지가 겹치는 것**이다.

- `AnomalyGate`는 Loki ERROR/WARN 수·Prometheus 5xx를 **임계값으로 판정**한다. Prometheus·Loki 룰이 하는 일과 똑같아 룰과 진짜로 중복이다.
- 반면 `MonitorAnalysis`(LLM이 로그를 읽어 만든 원인 가설 + 조치 제안)는 Alertmanager·PromQL로는 못 만드는 **여기서만 나오는 가치**다.

즉 ADR-0026과 부딪히는 건 "탐지"이고, 살릴 건 "LLM 보강"이다.

### 현재 구조

```text
@Scheduled(공유 풀) → MonitorScheduler → MonitorCollector
    → Loki/Prometheus 직접 폴링 → AnomalyGate(임계 판정)
    → (이상·비쿨다운·예산) AnomalyAnalyzer(LLM) → ZzolBotSlackNotifier → Slack
```

문제 둘:

1. **탐지가 Alertmanager 룰과 중복**(ADR-0026 약점 재현).
2. `MonitorCollector`가 공유 `@Scheduled` 스레드(pool size 1)에서 Loki/Prometheus를 **타임아웃 없이 폴링**한다. 외부 응답이 느리면 아웃박스 릴레이·Redis 스트림 복구 등 다른 스케줄 작업까지 멈춘다(리뷰의 BLOCKER).

## 결정

**zzol-bot을 "별도 알림기"에서 "Alertmanager 웹훅 수신기"로 바꾼다.** 탐지는 단일 엔진(Prometheus 룰 + 새 Loki ruler)으로 모으고, zzol-bot은 Alertmanager가 보낸 알림을 받아 **LLM으로 보강**(원인 가설 + 조치 제안 + Loki 로그 샘플)한 뒤 Slack에 올린다.

```text
Prometheus 룰 + Loki ruler  →  Alertmanager (그룹·억제·silence)
                            →  webhook  →  zzol-bot 보강(LLM + Loki 샘플)  →  Slack
```

세부 결정:

1. **탐지를 단일 엔진으로 되돌리되, 옛 폴링이 보던 신호 5개를 빠짐없이 옮긴다.**
   - **HTTP 5xx** → 기존 Prometheus 룰 `Http5xxRatioHigh`. 이미 커버.
   - **Stream backlog** → 기존 룰 `RedisStreamBacklogHigh`(`max(redis_stream_length) > 1000`). 이미 커버 — 폴링이 이걸 중복으로 보던 것이라 제거가 맞다.
   - **ERROR/WARN 로그** → 새 **Loki ruler 룰**(`count_over_time(... |= "ERROR" [w]) > N`).
   - **DLQ 적체** → 대응 메트릭·룰이 **없어 누락 위험**이었다(리뷰 발견). 로그 율 룰로는 느린 누적을 못 잡으므로 `outbox_dead_letter_count` 게이지(`OutboxDeadLetterMetricService`)를 새로 만들고 룰 `OutboxDeadLetterHigh`(`> 10`, 옛 임계 유지)로 복구했다.

   이제 Alertmanager가 탐지·그룹화·silence·억제를 모두 맡는다.

2. **zzol-bot은 웹훅 수신기로만 동작한다.** `POST /internal/zzolbot/alerts/webhook` 하나만 열고, Alertmanager가 firing 알림을 여기로 보낸다. `status=firing`만 보강해 Slack에 올린다. `AnomalyGate`·`MonitorCollector`는 제거한다.

3. **덤으로 폴링 굶음(starvation) 위험이 사라진다.** zzol-bot이 공유 `@Scheduled` 스레드에서 더는 폴링하지 않으므로, 스케줄러 풀이 굶는 경로 자체가 없어진다. 리뷰의 BLOCKER가 핫픽스 없이 닫힌다.

4. **앱측 쿨다운(`MonitorService.inCooldown`)은 제거한다.** 재알림 억제는 이제 Alertmanager의 `repeat_interval`(zzolbot-enrich = 1h)이 맡는다(옛 앱 cooldown 240m을 대체). 앱에도 쿨다운을 두면 이중이라 떼고, 비용 안전장치는 `LlmCallBudget`(일일 호출 상한)이 맡는다. 기능 손실이 아니라 단순화다.

5. **이 PR이 구현 전체를 담는다.** ADR을 `승인`으로 올리고 ① Java 보강 경로, ② 인프라(`alertmanager.yml` nginx 경유 receiver + 베어러 인증, nginx 내부 리스너·공개 차단, `docker-compose` 네트워크 조인, `loki.yml` ruler), ③ Spring Security `/internal/**` 베어러 토큰을 함께 반영한다. 인프라는 로컬 검증이 안 돼 배포 시점에 확인한다(아래 결과).

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| 현행 유지(직접 Slack) | 추가 작업 없음 | 탐지가 룰과 중복(ADR-0026 약점), 폴링 굶음 잔존, silence/억제 미적용 |
| **A. 웹훅 수신기(채택)** | 단일 엔진 유지, LLM 가치 보존, 폴링 굶음 해소, silence·그룹화·억제가 보강 알림에도 적용 | Alertmanager 룰 커버리지에 의존(자체 탐지 없음), 네트워크·노출 차단 배선 필요 |
| B. 자체 탐지 유지 + `/api/v2/alerts` push | 전달만 일원화하며 자체 탐지 유지 | 중복 탐지를 그대로 둠(약점 미해결) — 지금 게이트는 단순 임계값이라 순수 중복. LLM *탐지*(PromQL로 못 푸는 패턴)가 생길 때만 정당 |

> 대안 B는 "보강이 아니라 탐지"로 발전할 때를 위한 장래 선택지다.

## 트레이드오프

- **Alloy는 알림 엔진이 아니다.** 로그 탐지를 단일 엔진에 합치는 실제 장치는 **Loki ruler**다. `loki.yml`에 `rules_directory`는 있으나 `ruler:` 블록이 비어 있어, 이걸 켜는 게 곧 합치는 작업이다. Alloy는 로그를 그 스택에 넣는 파이프라인일 뿐이다([ADR-0027](0027-promtail-to-alloy-migration.md)·[ADR-0028](0028-alloy-pull-based-config-via-git.md)·[ADR-0030](0030-alloy-trace-metric-collection-integration.md)).
- **웹훅은 앱 컨테이너가 아니라 nginx로 보낸다(블루-그린·네트워크·노출차단 한 번에 해결).** 앱은 `blue`/`green` 두 컨테이너로 뜨고 활성 색을 가리키는 고정 별칭이 없다. 활성 색의 유일한 기준은 nginx의 `*-service.inc`(`set $upstream http://dev-app-blue:8080;`)이고 전환 때 이 파일만 바뀐다(ADR-0023에서 edge-cd 동기화 제외인 라이브 파일). 웹훅을 `blue`로 못박으면 green이 활성일 때 죽은 색으로 가 **조용히 실패**한다. 웹훅을 nginx로 보내 nginx가 `$upstream`(활성 색)으로 넘기면 ① 활성 색 자동 추종 ② Alertmanager(`monitoring-network`)↔앱(`dev/prod-network`) 네트워크 도달 ③ 노출 차단이 한 번에 풀린다.
- **내부 노출은 막아야 한다.** `/internal/zzolbot/alerts/webhook`이 공개되면 가짜 Slack 알림을 주입할 수 있다. nginx에 **내부 전용 location**(공개 블록과 분리, 모니터링 네트워크에서만 도달)으로 넣고 Spring Security로 `/internal/**`를 한 번 더 막는다. 공개 경로와 같은 `$upstream`을 재사용해 블루-그린 추종을 공유한다.
- **페이로드 형태.** 수신기는 firing·resolved를 모두 받고 각 페이로드에 (상류에서 그룹화된) `alerts[]` 배열이 들어온다. 보강은 firing만 의미 있어 `status`로 거른다.
- **웹훅은 한 번만 오지 않는다 — 수신기는 멱등이어야 한다(결정 #4 부작용).** Alertmanager는 타임아웃·5xx, `repeat_interval`(1h) 주기, 재시작 때 같은 firing을 다시 보낸다. 보강이 동기(Gemini 수 초)라 막는 장치가 없으면 Slack 중복·예산 중복 소진·중복 행이 생긴다. **완화:** `AlertEnrichmentService`가 시작 지점에서 `duplicate-suppression-seconds`(기본 300s, `repeat_interval`보다 짧게) 안에 같은 fingerprint 알림 이력이 있으면 건너뛴다. 쿨다운 부활이 아니라 *중복 배달 차단*이다(쿨다운=재알림 억제, 이 가드=재시도 중복 흡수). 조회-후-저장이라 동시 배달은 미세하게 샐 수 있어, 완전 차단이 필요하면 `(fingerprint, 시간버킷)` unique 제약을 후속 도입한다.
- **Slack 포맷은 zzol-bot이 맡는다.** 보강 알림은 zzol-bot Java가 포맷해 Alertmanager의 Slack receiver를 거치지 않는다. 어느 알림을 어느 경로로 보낼지(Alertmanager 직접 vs zzol-bot 보강)를 라우팅(`continue`)으로 정해야 중복·누락이 없다.
- **자체 탐지는 포기한다.** zzol-bot은 Alertmanager 룰이 켜진 것만 본다. PromQL/LogQL로 못 쓰는 이상은 (대안 B 전까지) 탐지 밖이다.

## 결과

- **상태는 `승인`이다.** ADR-0026(단일 알림 엔진)·ADR-0030(수집 일원화)을 건드리는 팀 레벨 결정으로 합의했고, 구현을 본 PR에 포함한다.
- **Java 변경(로컬 검증됨):**
  - 신규: `monitor/ui/AlertmanagerWebhookController`·`AlertmanagerWebhookRequest`(DTO), `monitor/application/FiringAlertEnricher`(포트)·`AlertEnrichmentService`(구현), `monitor/domain/FiringAlert`.
  - 리팩터: `AnomalyAnalyzer`/`GeminiAnomalyAnalyzer`/`ZzolBotSlackNotifier`/`MonitorRunEntity.of`를 `MonitorSnapshot` 대신 **알림 컨텍스트** 입력으로, `MonitorService`는 조회 전용으로, `ZzolBotMonitorController`에서 수동 트리거(`POST /run`) 제거, `LokiLogClient`는 `tailErrors`만 유지, `MonitorProperties`는 `enabled`·`errorLogWindowMinutes`·`duplicateSuppressionSeconds`로 정리, `zzolbot.html` 대시보드 읽기 전용.
  - 삭제: `MonitorCollector`·`MonitorScheduler`·`AnomalyGate`·`PrometheusMetricClient`·`MonitorSnapshot`/`MonitorSignal`/`AnomalyVerdict`·`LoggingFiringAlertEnricher`(+테스트).
  - **DLQ 신호 복구**: `:infra` `OutboxDeadLetterMetricService`(신규, `outbox_dead_letter_count` 게이지) + `OutboxEventRepository.countByStatus`. 폴링 제거로 빠진 DLQ "적체 깊이" 신호를 단일 엔진 메트릭으로 되살린다(리뷰 발견).
- **인프라 변경(배포 시점 검증):**
  - `docker/monitoring/conf/alertmanager.yml`: `zzolbot-enrich` receiver를 `http://nginx:8889/internal/...` + 베어러 인증(`credentials_file`)으로 지정, firing 미러링 route.
  - `docker/monitoring/conf/rules/alerts-redis-stream.yml`: `OutboxDeadLetterHigh` 룰(`max(outbox_dead_letter_count) > 10`) 신규. `promtool check rules` 통과.
  - `docker/nginx/conf/internal.conf`(신규): 8889 내부 전용 리스너가 `$upstream`(prod-service.inc=활성 색)으로 프록시. `dev.conf`·`prod.conf` 공개 블록은 `/internal/`을 404로 막는다.
  - `docker/nginx/docker-compose.yml`: nginx를 `monitoring-network`에 조인 + 8889 `expose`(호스트 미노출).
  - `docker/monitoring/conf/loki.yml`: ruler 블록(비어 있던 `ruler:` 활성화).
  - Spring Security: `/internal/**` 전용 체인(Order 0)이 베어러 토큰을 검증한다.
- **배포 시점에 확인할 것(로컬 불가):**
  - **시크릿**: 서버에서 `~/monitor/secrets/zzolbot_webhook_token` 생성(`slack_api_url`과 동일: `chown 65534:65534`·`chmod 600`)하고 앱 env `ZZOL_BOT_ALERT_WEBHOOK_TOKEN`에 같은 값 주입.
  - **도달성**: alertmanager → `nginx:8889`(`monitoring-network` 조인)와 nginx → 활성 색 프록시를 `docker exec`로 확인.
  - **환경 라우팅**: 내부 리스너는 현재 **prod 활성 색**(`prod-service.inc`)으로 보낸다(zzol-bot 모니터는 prod 인시던트 중심). dev 보강이 필요하면 `dev-service.inc`를 include한 8890 리스너를 같은 패턴으로 추가하고 `alertmanager.yml`에서 `job` 라벨로 분기한다.
  - **Loki ruler 룰 마운트**: `docker-compose`에 `./conf/loki-rules:/etc/loki/rules:ro` 마운트 + 테넌트 `fake/` 하위 배치(본 PR 포함). Loki는 ruler를 기동 시 읽으므로 **컨테이너 재시작** 필요.
  - 배포는 edge-cd(ADR-0023) 위에서 하며 서버 직접 수정 금지(ADR-0026 계승). `*-service.inc`는 edge-cd 동기화 제외(라이브 B/G) 대상임에 유의.
- **롤백.** 인프라는 alertmanager `zzolbot-enrich` route/receiver 제거 + nginx `internal.conf` 제거 + 리로드로 원복(앱·트래픽 영향 없음). Java는 폴링 제거를 포함하므로 되돌리려면 본 커밋 revert.
- 브랜치는 `be/chore/zzolbot-alertmanager-receiver-adr`, PR 타깃은 `be/feat/1474-zzolbot-upgrade`(#1492)다.

## 배포 시 운영 체크리스트

Git 정본(`backend/docker/**`)은 edge-cd(ADR-0023)가 서버로 동기화하지만, **시크릿·env·컨테이너 재생성·실측 검증은 자동화되지 않는다.** 아래는 사람이 서버에서 직접 한다. 순서를 지킨다 — 시크릿이 없으면 인증이 막히고, 네트워크 조인이 없으면 alertmanager가 nginx에 못 닿는다.

### 1. 배포 전 — 시크릿·환경변수 준비 (필수)

- **베어러 토큰 생성**: JWT가 아니라 단순 공유 시크릿(랜덤 문자열)이다. 앱은 상수시간 비교만 하므로 **한 번 만들어 같은 값을 양쪽(파일·env)에** 넣는 게 핵심이다. 앱 env에 개행이 섞이면 401이 나므로 파일은 `printf`(개행 없음)로 쓴다.

```bash
# 한 번 만들어 변수에 담는다 — 256-bit, 헤더에 안전한 hex(특수문자 없음)
TOKEN=$(openssl rand -hex 32)

# (a) Alertmanager용 파일 — 개행 없이 기록, 컨테이너 uid(65534) 소유(slack_api_url과 동일)
printf '%s' "$TOKEN" | sudo tee ~/monitor/secrets/zzolbot_webhook_token >/dev/null
sudo chown 65534:65534 ~/monitor/secrets/zzolbot_webhook_token
sudo chmod 600 ~/monitor/secrets/zzolbot_webhook_token

# (b) 앱용 — 같은 $TOKEN 값을 ZZOL_BOT_ALERT_WEBHOOK_TOKEN 으로 주입(개행 없이)
echo "$TOKEN"
```

- **앱 env 선언**: `ZZOL_BOT_ALERT_WEBHOOK_TOKEN`을 dev/prod 앱 컨테이너 env에 주입한다(`docker/{dev,prod}/docker-compose.yml`의 app env 또는 서버 `.env`). 미설정이면 `InternalWebhookTokenFilter`가 모든 `/internal` 요청을 401로 막는다(기본 차단). **값은 커밋하지 않는다.**
- 시크릿 파일은 발송마다 새로 읽히므로 alertmanager 재시작은 불필요하지만, **앱 env 주입은 앱 컨테이너 재배포(backend-cd)로 반영**된다.

### 2. 배포 — 컨테이너 재생성/재시작 (reload로 안 되는 곳 주의)

| 대상 | 변경 | reload로 충분? | 필요 조치 |
|------|------|----------------|-----------|
| **nginx** | monitoring-network 조인 + 8889 expose | ❌ (네트워크·expose는 컨테이너 속성) | **`docker compose up -d nginx` 재생성** — 공개 nginx 순단 발생, 저트래픽 시간대 권장. (무중단 임시방편: `docker network connect monitoring-network nginx` 후 conf만 reload, 다음 재생성 때 일치) |
| nginx conf (internal.conf·/internal 404) | conf만 | ✅ | 재생성에 포함. 단독 변경이면 `nginx -s reload` |
| **loki** | `ruler:` 블록 + 룰 마운트 | ❌ (ruler는 기동 시 시작) | **loki 재시작**(`docker compose up -d loki`) |
| **alertmanager** | `zzolbot-enrich` receiver/route | ✅ | `docker kill -s HUP alertmanager`(또는 `/-/reload`) |
| **prometheus** | `OutboxDeadLetterHigh` 룰 | ✅ | `docker kill -s HUP prometheus` |
| **app** | webhook 수신기·enricher·DLQ 게이지·env | — | backend-cd 재배포 |

### 3. 배포 후 — 검증 (9090/9093/3100 호스트 미노출 → `docker exec`)

- **설정 문법**: `docker exec nginx nginx -t`, `docker exec prometheus promtool check rules /etc/prometheus/rules/alerts-redis-stream.yml`.
- **DLQ 메트릭 노출**: `docker exec prometheus wget -qO- 'http://localhost:9090/api/v1/query?query=outbox_dead_letter_count'`로 시리즈가 보이는지.
- **룰 로드**: `.../api/v1/rules`에 `OutboxDeadLetterHigh`와 Loki ruler 발화분이 보이는지.
- **도달성**: `docker exec alertmanager wget -qO- http://nginx:8889/internal/zzolbot/alerts/webhook` → 토큰 없이는 **401**(차단 확인). Alertmanager가 실제 보낼 때만 200.
- **엔드투엔드**: dev에서 임계 초과를 유발(또는 `amtool` 테스트 알림)해 firing → zzol-bot 보강 → Slack 메시지 도착까지 확인.

### 4. 롤백

앱·트래픽 영향 최소: alertmanager `zzolbot-enrich` 제거 + `docker kill -s HUP alertmanager`, nginx `internal.conf` 제거 + 재생성(또는 `network disconnect`), loki ruler 블록 주석 + 재시작. Java(폴링 제거 포함)는 커밋 revert.
