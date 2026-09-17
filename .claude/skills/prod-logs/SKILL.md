---
name: prod-logs
description: Grafana MCP로 운영·개발 서버의 Loki 로그와 Prometheus 지표를 직접 조회한다. "운영 로그 봐줘", "prod 에러 로그", "지표 확인해줘", 장애 분석에 실제 로그가 필요할 때 사용한다.
---

# prod-logs

운영 로그는 Alloy가 Loki로 보내고 Grafana(`https://status.zzol.site`)가 Loki·Prometheus·Tempo를 본다. `.mcp.json`의 `grafana` 서버가 Grafana 공식 MCP(`grafana/mcp-grafana`)를 docker stdio로 띄워 그 Grafana에 붙는다. 사람이 로그를 복사해 붙여넣지 않고 `mcp__grafana__*` 도구로 직접 조회한다(#1789).

같은 항목이 루트·`backend/`·`frontend/`의 `.mcp.json`에 있다. Claude Code는 실행한 디렉터리의 `.mcp.json`만 읽기 때문이다(`tools/api-mcp/README.md`). 바꿀 때는 셋을 같이 고친다.

## 준비 (한 번만)

1. Grafana 관리자가 **Viewer** 권한 서비스 계정을 만들고 토큰을 발급한다. Administration → Service accounts → Add service account → Role: Viewer → Add token.
2. 토큰을 셸 환경변수로 둔다. 저장소에는 넣지 않는다.

   ```bash
   # ~/.zshrc 또는 ~/.zshenv
   export GRAFANA_SA_TOKEN='glsa_…'
   ```

3. Claude Code를 다시 시작한다. `.mcp.json`은 시작 시점의 환경변수를 읽는다. docker 데몬(OrbStack)이 떠 있어야 한다.
4. 첫 실행에서 프로젝트 MCP 서버 승인 프롬프트가 뜨면 승인한다. 승인 전에는 `mcp__grafana__*` 도구가 목록에 없다.

읽기만 된다. 토큰이 Viewer이고 서버도 `--disable-write`와 `--enabled-tools datasource,prometheus,loki`로 띄우므로 남는 도구 15개가 전부 조회다.

## 붙었는지 확인

`mcp__grafana__list_datasources`를 부른다. Loki·Prometheus·Tempo 데이터소스가 나오면 된다. 401이면 토큰이 비었거나 틀린 것이다. 도구 자체가 없으면 승인 전이거나, docker가 안 떠 있거나, 재시작 전이다.

## Loki 라벨

라벨은 `backend/docker/alloy/modules/app-logs.alloy`가 붙인다. 여기가 SSOT이고 아래는 요약이다.

| 라벨 | 값 | 비고 |
| --- | --- | --- |
| `job` | `prod-app`, `dev-app` | 환경별 config.alloy가 넘긴다 |
| `environment` | `prod`, `dev` | |
| `level` | **지금은 안 붙는다** | Alloy 정규식이 로그의 `[traceId,spanId]` 필드를 빠뜨려 매칭이 실패한다. 레벨은 라벨이 아니라 라인 필터로 거른다 |

로그 한 줄 형식(`backend/app/src/main/resources/logback-spring.xml`의 `FILE_LOG_PATTERN`):

```text
[yyyy-MM-dd HH:mm:ss.SSS] [LEVEL] [traceId,spanId] --- [thread] logger : message
```

같은 이유로 `timestamp`도 못 뽑아 로그 시각이 아니라 Loki 인입 시각으로 적재된다. 시간 범위를 좁힐 때 인입 지연만큼 어긋난다. 운영 알럿 규칙(`backend/docker/monitoring/conf/loki-rules/fake/zzolbot-log-signals.yml`)도 그래서 `|= "ERROR"` 라인 필터를 쓴다.

## 자주 쓰는 LogQL

```logql
{job="prod-app"} |= "ERROR"                                   # 운영 에러만
{job="prod-app"} |= "roomId=abc123"                           # 특정 방 추적
{job="prod-app"} |~ "WebSocket|1006"                          # 정규식
sum(count_over_time({job="prod-app"} |= "ERROR" [5m]))        # 에러 건수 추이
```

`query_loki_logs`는 한 번에 최대 100줄이다. 넓게 보려면 `query_loki_stats`나 `count_over_time`으로 분포부터 잡고 시간 범위를 좁힌다.

**Loki 조회는 하나씩 보낸다.** 여러 도구 호출을 한 번에 던지면 Loki가 `429 too many outstanding requests`로 전부 거부한다. 날짜별로 나눠 볼 때도 앞 호출의 결과를 받은 뒤 다음을 보낸다.

`|= "ERROR"` 라인 필터는 메시지에 "ERROR"가 든 INFO 로그도 잡는다. ZzolBot의 "ERROR 로그 없음" 같은 줄이 섞이니 집계할 때 `[ERROR]`로 한 번 더 거른다. 스택트레이스는 줄마다 별도 항목으로 적재되므로 예외 클래스를 보려면 `|~ "Exception"`으로 같은 시각 범위를 따로 조회한다.

## Prometheus

알럿 규칙은 `backend/docker/monitoring/conf/rules/`에 있다. 알럿이 왜 울렸는지 볼 때는 그 규칙의 `expr`를 `query_prometheus`에 그대로 넣는다. 지표 이름을 모르면 `list_prometheus_metric_names`로 먼저 찾는다.

## 하지 말 것

- 운영 로그에는 IP·닉네임·playerName이 INFO·WARN으로 찍힌다. 조회 결과는 그대로 AI 컨텍스트로 올라가므로 `{job="prod-app"}`처럼 통째로 긁지 말고 필터로 좁힌다. 결과를 이슈·PR·코멘트에 그대로 붙이지 않는다.
- 운영 서버에 ssh로 들어가 `docker logs`를 치지 않는다. 같은 로그가 Loki에 있고, ssh는 쓰기 권한까지 딸려간다.
- 토큰을 `.mcp.json`이나 커밋에 적지 않는다.
