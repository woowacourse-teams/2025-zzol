---
name: prod-logs
description: Grafana MCP로 운영·개발 서버의 Loki 로그와 Prometheus 지표를 직접 조회한다. "운영 로그 봐줘", "prod 에러 로그", "지표 확인해줘", 장애 분석에 실제 로그가 필요할 때 사용한다.
---

# prod-logs

운영 로그는 Alloy가 Loki로 보내고 Grafana(`https://status.zzol.site`)가 Loki·Prometheus·Tempo를 본다. 루트 `.mcp.json`의 `grafana` 서버가 Grafana 공식 MCP(`mcp/grafana`)를 stdio로 띄워 그 Grafana에 붙는다. 사람이 로그를 복사해 붙여넣지 않고 `mcp__grafana__*` 도구로 직접 조회한다(#1789).

## 준비 (한 번만)

1. Grafana 관리자가 **Viewer** 권한 서비스 계정을 만들고 토큰을 발급한다. Administration → Service accounts → Add service account → Role: Viewer → Add token.
2. 토큰을 셸 환경변수로 둔다. 저장소에는 넣지 않는다.

   ```bash
   # ~/.zshrc 또는 ~/.zshenv
   export GRAFANA_SA_TOKEN='glsa_…'
   ```

3. Claude Code를 다시 시작한다. `.mcp.json`은 시작 시점의 환경변수를 읽는다. docker 데몬(OrbStack)이 떠 있어야 한다.

읽기만 된다. 토큰이 Viewer이고 서버도 `--disable-write`로 띄우므로 대시보드 수정·알럿 변경은 두 겹으로 막힌다.

## 붙었는지 확인

`mcp__grafana__list_datasources`를 부른다. Loki·Prometheus·Tempo 데이터소스가 나오면 된다. 401이면 토큰이 비었거나 틀린 것이고, 도구 자체가 없으면 docker가 안 떠 있거나 재시작 전이다.

## Loki 라벨

라벨은 `backend/docker/alloy/modules/app-logs.alloy`가 붙인다. 여기가 SSOT이고 아래는 요약이다.

| 라벨 | 값 | 비고 |
| --- | --- | --- |
| `job` | `prod-app`, `dev-app` | 환경별 config.alloy가 넘긴다 |
| `environment` | `prod`, `dev` | |
| `level` | `INFO`, `WARN`, `ERROR` 등 | 로그 줄의 `[LEVEL]`을 정규식으로 뽑는다. 형식이 안 맞는 줄은 라벨이 없다 |

로그 한 줄 형식: `[yyyy-MM-dd HH:mm:ss.SSS] [LEVEL] --- [thread] logger : message`

## 자주 쓰는 LogQL

```logql
{job="prod-app", level="ERROR"}                       # 운영 에러만
{job="prod-app"} |= "roomId=abc123"                   # 특정 방 추적
{job="prod-app"} |~ "WebSocket|1006"                  # 정규식
sum by (level) (count_over_time({job="prod-app"}[5m]))                # 레벨별 건수 추이
```

`query_loki_logs`는 한 번에 최대 100줄이다. 넓게 보려면 `query_loki_stats`나 `count_over_time`으로 분포부터 잡고 시간 범위를 좁힌다.

## Prometheus

알럿 규칙은 `backend/docker/monitoring/conf/rules/`에 있다. 알럿이 왜 울렸는지 볼 때는 그 규칙의 `expr`를 `query_prometheus`에 그대로 넣는다. 지표 이름을 모르면 `list_prometheus_metric_names`로 먼저 찾는다.

## 하지 말 것

- 운영 서버에 ssh로 들어가 `docker logs`를 치지 않는다. 같은 로그가 Loki에 있고, ssh는 쓰기 권한까지 딸려간다.
- 토큰을 `.mcp.json`이나 커밋에 적지 않는다.
