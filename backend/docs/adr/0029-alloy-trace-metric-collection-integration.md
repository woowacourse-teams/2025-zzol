# 0029. 트레이스·메트릭 수집의 Alloy 통합 검토 — 콜렉터 일원화와 백엔드 불변

- 날짜: 2026-06-14
- 상태: 제안 (초안 — 결정 보류)

## 컨텍스트

ADR-0027로 로그 콜렉터를 Promtail → Grafana Alloy로 교체하면서, Alloy가 OpenTelemetry Collector 기반이라 로그뿐 아니라 **트레이스·메트릭도 수집할 수 있다**는 점이 드러났다. 자연히 "트레이스·메트릭도 Alloy로 모아 텔레메트리 수집을 일원화하고, 나아가 **Tempo 없이 일관화**할 수 있지 않나"라는 질문이 제기됐다.

이 질문에는 **먼저 교정해야 할 전제 오류**가 있고, 그 교정이 이 ADR이 실제로 결정할 수 있는 범위를 결정한다.

### Alloy는 콜렉터(agent)이지 저장·질의 백엔드가 아니다

- Alloy는 텔레메트리를 **수신·가공·전달**한다. 트레이스를 **저장하거나 질의(TraceQL)하지 못한다.**
- **Tempo는 트레이스 데이터베이스**다(저장 + TraceQL 질의). 로그의 Loki, 메트릭의 Prometheus에 대응하는 **트레이스 평면의 저장 백엔드**다.
- 따라서 "트레이스를 Alloy로 통합"이란 **Alloy를 Tempo 앞단의 OTLP 게이트웨이로 끼워넣는 것**이지 Tempo를 대체하는 게 아니다. Tempo는 그대로 남는다.

```text
[현행]  App(OTel SDK) ───OTLP───────────────▶ Tempo (저장/질의)
[통합]  App(OTel SDK) ─OTLP▶ Alloy(otelcol) ─OTLP▶ Tempo (저장/질의)
                              └ 가공: tail-sampling, 배치, 재시도 큐, 속성 주입
```

- 로그 + 트레이스 + 메트릭을 한 곳에 저장하는 **단일 Grafana 백엔드는 없다.** 세 질의 언어(LogQL / TraceQL / PromQL), 세 저장소가 그대로다. Alloy가 일원화하는 것은 **수집 계층**(에이전트 1개·River 설정 1벌·OTLP 파이프라인이 세 백엔드로 fan-out)이며, 이것이 곧 표준 LGTM 토폴로지다. **백엔드 제거가 아니다.**

### "Tempo 제거"를 단독으로 불가능하게 만드는 결정적 제약 (1차 근거)

`app/src/main/resources/config/zzolbot.yml`은 `tempo-url: ${ZZOL_BOT_TEMPO_URL:http://tempo:3200}`로 **Tempo 질의 API(3200)를 직접 조회**한다. zzol-bot이 트레이스를 읽어 쓰는 **출시된 기능**이 Tempo에 의존한다. 수집 계층을 어떻게 바꾸든 Tempo를 없애면 이 기능이 깨진다. `docker/monitoring/docker-compose.yml`의 Grafana도 `depends_on: tempo`로 Tempo를 트레이스 데이터소스로 둔다.

→ **결론: "Tempo 없이 일관화"는 달성 불가능한 목표다.** 이 ADR이 실제로 다룰 수 있는 결정은 훨씬 좁다 — *트레이스·메트릭을 Alloy 경유(agent 통합)로 보낼 것인가, 직결을 유지할 것인가.*

### 현재 텔레메트리 평면

| 신호 | 수집 경로 | 저장/질의 백엔드 | Alloy 관여 |
|------|-----------|------------------|------------|
| 로그 | App → 파일 → Alloy tail | Loki | ✅ (ADR-0027) |
| 트레이스 | App(OTel SDK, Micrometer Tracing) → 직결 OTLP `tempo:4318` (샘플링 `TRACE_SAMPLING_PROBABILITY`=0.1) | **Tempo** | ❌ 미경유 |
| 메트릭 | Prometheus **pull** ← app `/actuator/prometheus`, cadvisor, mysql/redis/node-exporter | Prometheus | ❌ 미경유 |

트레이스 설정은 `app/src/main/resources/config/monitoring.yml`(`management.otlp.tracing.endpoint=${TEMPO_URL}`), Redis Stream 트레이스 전파는 ADR-0021에 정의돼 있다.

## 결정

**현 시점에서는 트레이스·메트릭의 Alloy 경유 통합을 보류한다(defer).** 통합을 진행한다면 **신호별로 분리 판단**하며, 어느 경우에도 **Tempo/Loki/Prometheus 세 백엔드는 유지**한다. "Tempo 제거"는 zzol-bot·Grafana 의존으로 채택 불가다.

세부 판단:

1. **두 결정은 독립이며 묶지 않는다.** 트레이스-Alloy경유와 메트릭-Alloy경유는 실익이 전혀 다르다.

2. **트레이스(OTLP 게이트웨이)는 통합의 *방어 가능한* 절반이다.** Alloy를 끼우면 tail-based 샘플링, 버퍼링/재시도 큐, 속성 enrichment, 앱↔Tempo 엔드포인트 디커플링이 가능하다. 그러나 현 트래픽과 head 샘플링 0.1에서 직결로 충분하며, 이 이점들을 실제로 요구하는 구체적 드라이버(예: tail-sampling으로 에러 트레이스만 보존, Tempo 장애 시 버퍼링)가 생기기 전에는 추가 hop·운영 비용을 정당화하지 못한다.

3. **메트릭은 대부분 churn이라 권장하지 않는다.** Alloy `prometheus.scrape` + `remote_write`로 바꿔도 **Prometheus는 저장소로 그대로 남고**, ADR-0026 알림 룰이 의존하는 `up{job="..."}` per-target 의미가 깨져 룰을 재작성해야 한다. 이중 스크레이프(Alloy + Prometheus 동시) 위험만 새로 생기고 얻는 게 없다. **Prometheus pull 유지.**

4. **blast radius 확대를 인지한다(ADR-0028 상호작용).** 오늘은 잘못된 Alloy 설정이 **로그만** 끊는다. 트레이스를 Alloy 경유로 두고 ADR-0028의 `import.git` pull까지 결합하면, 잘못 pull된 config가 이제 **트레이스까지** 끊는다. 실패 도메인이 로그→로그+트레이스로 넓어진다.

5. **중복 수집 금지(통합 시 필수 제거 항목).** 통합을 진행할 경우 같은 신호를 두 경로로 동시에 수집하지 않도록 직결 경로를 **반드시 제거**한다.
   - 트레이스: 앱의 `TEMPO_URL`을 Alloy OTLP receiver로 변경(엔드포인트가 하나라 사실상 배타되지만 명시적으로 전환).
   - 메트릭: 만약 옮긴다면 `prometheus.yml`의 해당 scrape job을 제거(안 그러면 이중 스크레이프).

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| 현행 유지(직결 OTLP + Prometheus pull) — *보류 시 사실상 채택* | 단순, hop·실패점 추가 없음, ADR-0026 알림 룰·ADR-0021 전파 무수정, 검증된 동작 | 수집 계층이 신호별로 분산(에이전트 일원화 이점 없음) |
| 트레이스만 Alloy OTLP 게이트웨이(메트릭은 pull 유지) | tail-sampling·버퍼링·enrichment 여지, 앱↔Tempo 디커플링 | hop 1개·실패점 추가, blast radius 확대(ADR-0028), 현 규모에선 이점 미발현 |
| 트레이스+메트릭 전면 Alloy 경유 | 수집 계층 완전 일원화(에이전트·설정 1벌) | 메트릭은 순수 churn(룰 재작성·`up` 의미 손상), 이중 수집 위험, 운영 표면↑ |
| Tempo 제거(질문 원안) | (가정상) 백엔드 단순화 | **불가** — zzol-bot이 `tempo:3200` 질의 의존, Grafana 트레이스 데이터소스 의존; Alloy는 저장·질의 불가라 대체 불가 |

## 트레이드오프

- **통합이 제거하는 백엔드는 0개다.** 일원화 효과는 *수집 에이전트*에 한정되고, 비용은 신규 hop·실패점이다. 현 스택은 직결이라 hop이 없다.
- **자기 일관성.** 직전 검토(로그 전용 like-for-like)에서 "현 규모엔 직결이 낫다"고 본 결론을 이 ADR이 뒤집지 않는다. Tempo 교정으로 통합의 주된 명분(백엔드 일원화)이 사라졌으므로 오히려 보류 쪽으로 강화된다.
- **재검토 트리거.** 다음 중 하나가 생기면 트레이스 게이트웨이(대안 2)를 재평가한다 — (a) tail-based 샘플링 요구(에러/지연 트레이스만 보존), (b) Tempo 장애 시 트레이스 유실을 버퍼링으로 막아야 하는 SLO, (c) 다중 송신원(여러 앱/사이드카)을 한 OTLP 수집점으로 모을 필요.

## 결과

- **이 ADR은 코드/설정 변경을 수반하지 않는다(보류 결정).** 통합을 채택할 경우의 변경 지점만 사전 식별한다.
  - 트레이스 게이트웨이 채택 시: `docker/{dev,prod}/conf/config.alloy`에 `otelcol.receiver.otlp` + `otelcol.exporter.otlp(→tempo:4318)` 추가, 앱 `TEMPO_URL`을 Alloy로 재지정, `docker-compose`에서 alloy가 Tempo 네트워크 도달 확인.
  - 메트릭 이관(비권장) 시: `config.alloy`에 `prometheus.scrape` + `prometheus.remote_write(→prometheus)`, `prometheus.yml` 해당 job 제거, ADR-0026 룰의 `up{job}` 의존 재작성.
- **Tempo·Loki·Prometheus 세 백엔드는 어느 시나리오에서도 유지된다.**
- 본 ADR은 ADR-0027(로그 Alloy 교체)·ADR-0028(Alloy 설정 git pull)과 같은 Alloy 주제군에 속하나, 그 둘과 달리 **트레이스·메트릭 평면**을 다루며 현재로선 **변경 없음**으로 종결한다.
- 브랜치는 `be/chore/promtail-to-alloy`(또는 후속 전용 브랜치)다.
