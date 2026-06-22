# 0032. zzol-bot을 Alertmanager 웹훅 수신기로 재배치 — 탐지 일원화 + LLM 보강 분리

- 날짜: 2026-06-22
- 상태: 제안 (초안 — 결정 보류, 팀 합의 필요)

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

1. **탐지를 단일 엔진으로 되돌린다.** 5xx 급증은 이미 있는 Prometheus 룰로 충분하고, 로그 ERROR/WARN 카운트 신호는 **Loki ruler 룰**(`count_over_time(... |= "ERROR" [w]) > N`)로 이관해 Alertmanager로 발화시킨다. Alertmanager가 평가 커버리지·그룹화·silence·inhibition의 단일 주체로 남는다.

2. **zzol-bot은 웹훅 수신기로만 동작한다.** `POST /internal/zzolbot/alerts/webhook` 하나를 노출하고, Alertmanager의 `webhook_config` receiver가 firing 알림을 여기로 보낸다. zzol-bot은 `status=firing` 알림만 보강해 Slack에 enriched 메시지로 게시한다. `AnomalyGate`·`MonitorCollector`의 자체 수집·게이팅은 단계적으로 제거한다.

3. **부수 효과로 폴링 기아(starvation) 위험이 해소된다.** zzol-bot이 더 이상 공유 `@Scheduled` 스레드에서 Loki/Prometheus를 폴링하지 않으므로, 타임아웃 부재로 스케줄러 풀이 굶는 경로 자체가 사라진다. 이 ADR을 채택하면 코드 리뷰의 BLOCKER가 별도 핫픽스 없이 구조적으로 닫힌다.

4. **이 PR은 결정 골격까지만 담는다.** 본 ADR(결정 기록)과 함께, 웹훅 수신 컨트롤러 스텁·payload DTO·보강 포트(`FiringAlertEnricher`)와 인프라 설정 골격(`alertmanager.yml` 웹훅 receiver·`loki.yml` ruler 블록·Loki ruler 룰 파일)을 동봉한다. 실제 LLM 보강 배선(기존 `AnomalyAnalyzer`·`ZzolBotSlackNotifier` 재사용)과 자체 폴링 제거는 결정 승인 후 후속 PR로 진행한다.

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| 현행 유지(`ZzolBotSlackNotifier` 직접 Slack) | 추가 작업 없음 | 탐지가 Alertmanager 룰셋과 중복(ADR-0026 약점), 폴링 기아 위험 잔존, silence/inhibition 미적용 |
| **A. 웹훅 수신기(채택)** | 단일 엔진 유지, LLM 가치 보존, 폴링 기아 해소, silence/그룹화/억제가 enriched 알림에도 적용 | Alertmanager 룰 커버리지에 의존(자체 탐지 상실), 네트워크 도달성·내부 노출 차단 등 배선 필요 |
| B. zzol-bot 자체 탐지 유지 + `/api/v2/alerts` push | 전달만 일원화하면서 자체 탐지 유지 | 중복 탐지 레이어를 그대로 남김(ADR-0026 약점 미해결) — 현 게이트가 단순 임계값이라 **현 시점엔 순수 중복**. LLM 기반 *탐지*(PromQL로 표현 못 하는 의미론적 패턴) 로드맵이 생길 때만 정당화됨 |

> 대안 B는 "보강이 아니라 탐지"로 진화할 때를 위한 장래 선택지로 남긴다. 현재 `AnomalyGate`는 임계값이라 B는 중복을 제거하지 못한다.

## 트레이드오프

- **Alloy는 알림 엔진이 아니다.** "Alloy로 푼다"의 정확한 메커니즘은 **Loki ruler**다. `loki.yml`의 `common.storage.rules_directory: /loki/rules`는 있으나 `ruler:` 블록(`alertmanager_url`)이 미배선이라, ruler를 켜는 것이 곧 로그 탐지를 단일 엔진에 합류시키는 작업이다. Alloy의 역할은 그 스택에 로그를 먹이는 파이프라인이라는 점뿐이며([ADR-0027](0027-promtail-to-alloy-migration.md)·[ADR-0028](0028-alloy-pull-based-config-via-git.md)), [ADR-0030](0030-alloy-trace-metric-collection-integration.md)이 검토하는 수집 일원화와도 결이 같다.
- **네트워크 도달성.** Alertmanager는 `monitoring-network`에만 있고 앱은 `dev/prod-network`에 있다. Alertmanager → zzol-bot 웹훅이 도달하려면 Grafana처럼 양쪽 네트워크 조인(또는 역방향 라우팅)이 필요하다. 후속 PR의 `docker-compose` 변경 지점.
- **내부 노출 차단 필수.** 웹훅 엔드포인트는 nginx로 외부 노출하면 안 된다(`/internal/**` 차단 + Spring Security 제한). 외부에서 호출되면 임의 Slack 알림 주입이 가능하다.
- **웹훅 페이로드 형태.** 수신기는 firing과 resolved를 모두 받고, 각 페이로드는 (상류에서 그룹화된) `alerts[]` 배열을 싣는다. LLM 보강은 firing에만 의미가 있으므로 배열을 순회하며 `status` 필터링한다.
- **Slack 포맷팅 소유권 이동.** enriched 알림은 zzol-bot Java가 포맷해 Alertmanager의 Slack receiver를 우회한다. 어느 알림을 어느 경로로 보낼지(Alertmanager 직접 Slack vs zzol-bot enriched)를 라우팅(`continue`)으로 명시 설계해야 중복·누락이 없다.
- **자체 탐지 상실.** zzol-bot은 Alertmanager 룰이 발화한 것만 본다. PromQL/LogQL로 표현 못 하는 이상은 (대안 B의 장래 선택지 전까지) 탐지 범위 밖이다.

## 결과

- **결정 상태는 `제안`이다.** ADR-0026(단일 알림 엔진)과 ADR-0030(수집 일원화)을 건드리는 팀 레벨 결정이므로, 승인 후 본 ADR을 `승인`으로 갱신하고 후속 구현 PR을 연다.
- **이 PR의 변경 지점(골격):**
  - 코드: `zzolbot/.../monitor/ui/AlertmanagerWebhookController.java`(스텁), `.../monitor/ui/AlertmanagerWebhookRequest.java`(payload DTO), `.../monitor/application/FiringAlertEnricher.java`(포트), `.../monitor/infra/LoggingFiringAlertEnricher.java`(스텁 구현).
  - 인프라: `docker/monitoring/conf/alertmanager.yml`(webhook receiver + route), `conf/loki.yml`(ruler 블록), `conf/loki-rules/zzolbot-log-signals.yml`(신규 LogQL 룰).
- **후속 구현 PR의 변경 지점:**
  - `LoggingFiringAlertEnricher`를 실제 LLM 보강으로 교체(기존 `AnomalyAnalyzer`·`ZzolBotSlackNotifier` 재사용), `MonitorCollector`·`MonitorScheduler`·`AnomalyGate` 자체 폴링 제거.
  - `docker-compose.yml`: Loki ruler 룰 마운트, alertmanager ↔ 앱 네트워크 도달 배선.
  - Spring Security·nginx에서 `/internal/**` 외부 차단.
  - 배포는 edge-cd(ADR-0023) 위에서 진행하며 서버 직접 수정 금지(ADR-0026 계승).
- **롤백.** 골격은 자체 폴링·직접 Slack 경로를 제거하지 않으므로 기존 동작과 공존한다. 후속 PR에서 자체 폴링을 제거하기 전까지 두 경로가 잠시 병존할 수 있다(전환기 중복 알림 가능 — Phase 전환으로 해소).
- 브랜치는 `be/chore/zzolbot-alertmanager-receiver-adr`이며 PR 타깃은 `be/feat/1474-zzolbot-upgrade`다.
