# 0027. 로그 콜렉터 Promtail → Grafana Alloy 마이그레이션

- 날짜: 2026-06-11
- 상태: 승인 (2026-06-14)

## 컨텍스트

dev/prod 두 환경은 애플리케이션 로그(`/app/logs/application*.log`)를 **Promtail**로 수집해 Loki(`http://loki:3100`)로 push한다. 설정은 각각 `backend/docker/dev/conf/promtail.yml`, `backend/docker/prod/conf/promtail.yml`이고, 컨테이너는 dev/prod `docker-compose.yml`의 `dev-promtail`/`prod-promtail` 서비스로 떠 있다.

이 구성에는 두 문제가 있다.

- **Promtail이 EOL이다.** Grafana는 Promtail의 LTS를 2025-02-13에 시작하고 **2026-03-02에 EOL**로 종료했다. 오늘(2026-06-11) 기준 이미 EOL을 넘겨 보안 패치·버그 픽스가 끊겼다. Grafana는 후속 콜렉터로 **Grafana Alloy**(OpenTelemetry Collector 기반의 차세대 텔레메트리 콜렉터)를 공식 권장하며, Promtail의 모든 기능을 흡수했다.
- **prod 이미지가 `grafana/promtail:latest`로 핀이 풀려 있다.** 재현 불가능한 안티패턴이다. 서버가 언제 pull 했느냐에 따라 버전이 달라지고, EOL 이미지가 조용히 바뀔 수도 있다. (dev는 `2.9.6`으로 고정되어 있어 그나마 낫다.) 병행 진행 중인 알림 정비 작업(#1399)도 "prod-promtail이 unhealthy 상태였다"는 발견을 기록한 바 있어, 콜렉터 계층의 정비가 필요한 상황이었다.

두 promtail 설정은 거의 대칭이나 **타임스탬프 처리에서 의도적으로 다르다.** dev는 timestamp stage가 주석 처리되어 있어(콜렉터의 ingestion 시각을 로그 시각으로 사용), prod는 timestamp stage가 활성으로 스프링 로그의 KST 타임스탬프를 파싱해 UTC로 변환(`location: Asia/Seoul`)한다. 마이그레이션은 이 차이를 **반드시 보존**해야 한다.

## 결정

**Promtail을 Grafana Alloy로 교체하고, 변환은 Grafana 공식 도구 `alloy convert`로 수행한다. 이미지는 특정 버전으로 고정한다.**

1. **`alloy convert --source-format=promtail`로 기계 변환 후 사람이 검수한다.** promtail.yml(YAML)을 Alloy의 River 설정(`config.alloy`)으로 변환했다. 산출물은 `backend/docker/{dev,prod}/conf/config.alloy`다. 변환 결과를 그대로 두지 않고 컴포넌트별로 의미를 확인했다.
   - `loki.source.file`(파일 tail + glob discovery + `legacy_positions_file`) → `loki.process`(regex·labels·timestamp stage) → `loki.write`(Loki endpoint) 3단 파이프라인으로 떨어진다. 원본의 job/environment/level 라벨, `__path__` glob, regex가 모두 보존된다.
   - **dev**: timestamp stage 없음(원본 주석 처리와 동치 — ingestion 시각 사용).
   - **prod**: `stage.timestamp { source = "timestamp"; format = "2006-01-02 15:04:05.000"; location = "Asia/Seoul" }`로 KST→UTC 변환을 보존.

2. **변환 산출물을 `alloy validate`로 검증하고 `alloy fmt`로 포맷한다.** 검증은 promtool/amtool에 해당하는 Alloy의 사전 검증 게이트다. **이 게이트가 실제 버그를 잡았다**: convert가 promtail의 `labels: { level: }`(빈 값 = "추출된 동명 필드를 라벨로")를 River의 `level = null`로 옮겼는데, Alloy `stage.labels`는 `expected string, got null`로 거부한다. 동치인 `level = ""`(빈 문자열 = "키와 같은 이름의 필드 사용")로 고쳐 dev/prod 모두 validate exit 0을 확인했다. 기계 변환을 맹신하지 않고 검증·검수를 거치는 이유다.

3. **compose 서비스를 alloy로 교체하고 이미지를 버전 고정한다.** `dev-promtail`/`prod-promtail` → `dev-alloy`/`prod-alloy`. 이미지는 조사 시점 최신 안정 릴리스 **`grafana/alloy:v1.16.3`**(2026-06-08 릴리스, RC/Windows 변형 아님)로 dev/prod 모두 고정한다. **prod의 `latest` 안티패턴 해소가 본 작업의 부수 목표**다. command는 이미지 기본 CMD를 따르되(`run /etc/alloy/config.alloy --storage.path=/var/lib/alloy/data`) `--server.http.listen-addr=0.0.0.0:12345`를 추가했다. 설정 마운트는 `./conf/config.alloy:/etc/alloy/config.alloy:ro`. 볼륨(app-logs `:ro`, localtime), networks, `TZ=Asia/Seoul`, `deploy.resources`는 기존 값을 그대로 유지한다.

4. **healthcheck는 추가하지 않는다.** Alloy는 `/-/ready` 엔드포인트를 제공하지만, `grafana/alloy:v1.16.3` 이미지에는 `wget`도 `curl`도 없다(`command -v`로 확인). 없는 바이너리를 호출하는 healthcheck는 컨테이너를 영구 unhealthy로 만들어 healthcheck가 없는 것보다 나쁘다. 원본 promtail도 healthcheck가 없었으므로 동작 변경 없이 현 상태를 유지한다. (필요 시 후속에서 Alloy의 컴포넌트 헬스 API를 외부에서 프로브하는 방식을 검토한다.)

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| Promtail 유지(버전만 prod 고정) | 변경 최소, River 학습 불필요 | **EOL 미해소** — 보안/버그 픽스 없음, Grafana 권장 경로 역행 |
| Alloy 전환 + `alloy convert`(채택) | 공식 변환으로 안전·정확, EOL 해소, latest 안티패턴 동시 해소 | River 설정 언어 학습 비용, convert 산출물 검수 필요 |
| Alloy 전환 + River 수기 작성 | 산출물을 처음부터 통제 | 휴먼 에러 위험↑, 라벨/타임스탬프 누락 가능, 작업량↑ |
| 다른 콜렉터(Vector, Fluent Bit) | 일부 기능 우위 | Grafana/Loki 생태계 이탈, 운영 학습·통합 비용, 본 목적(Promtail 후속) 초과 |

## 트레이드오프

- **River 설정 언어 전환 비용.** 팀이 YAML promtail에 익숙한 만큼 River 문법은 초기 진입 비용이 있다. 단 파이프라인 구조가 단순(file→process→write)하고 convert가 1:1로 떨어져 비용은 낮다. validate/fmt로 사전 검증이 가능해 오히려 안전성은 올라간다.
- **기계 변환은 무오류가 아니다.** `level = null` 사례처럼 convert 산출물이 곧바로 유효하지는 않다. validate를 필수 게이트로 두는 이유이며, 이 ADR의 검증 절차가 그 보증이다.
- **Alloy는 promtail보다 약간 무겁다.** OTel Collector 기반이라 메모리/기능 표면이 더 크다. 그러나 본 용도(단일 로그 파일 tail→Loki push)에서는 기존 `deploy.resources`(mem 128M/64M) 한도로 충분하다고 보고 한도를 유지했다. 배포 후 실측으로 조정한다.
- **listen-addr을 0.0.0.0으로 열었다.** Alloy 기본은 loopback(`127.0.0.1:12345`)이라 컨테이너 밖에서 `/-/ready`·메트릭에 접근할 수 없다. 향후 헬스 프로브/메트릭 스크레이프 여지를 남기려 `0.0.0.0:12345`로 열되, 포트를 호스트로 publish하지 않으므로 노출은 Docker 네트워크 내부로 한정된다.

## 결과

- **신규 파일**: `backend/docker/dev/conf/config.alloy`, `backend/docker/prod/conf/config.alloy`, `docs/adr/0027-promtail-to-alloy-migration.md`.
- **수정 파일**: `backend/docker/dev/docker-compose.yml`(`dev-promtail`→`dev-alloy`), `backend/docker/prod/docker-compose.yml`(`prod-promtail`→`prod-alloy`), `docs/adr/index.md`(본 ADR 등록).
- **삭제 파일**: `backend/docker/dev/conf/promtail.yml`, `backend/docker/prod/conf/promtail.yml`(교체).
- **검증 통과**: `alloy validate /work/config.alloy` dev/prod 각각 exit 0. `docker compose config`도 dev/prod 모두 통과(미설정 env 경고만, 서비스 정의 파싱 오류 없음).
- **배포 경로 정합성**: dev/prod compose는 `edge-cd.yml`(ADR-0023) 대상이 **아니다.** edge-cd는 `backend/docker/nginx/**`·`backend/docker/monitoring/**`만 트리거·동기화한다. dev/prod compose는 **`backend-cd.yml`**(`backend/**` push)이 SCP로 `~/${ENV}/docker-compose.yml`에 복사하고 `deploy-infrastructure.sh`/`deploy-application.sh`로 적용한다(이 두 스크립트는 레포에 없고 서버에 존재 — 미확인). 따라서 본 변경은 backend-cd 경로로 흘러간다.
- **서비스 리네임에 따른 고아 컨테이너 정리 필요(배포 시 확인).** `dev-promtail`→`dev-alloy`로 서비스명·컨테이너명이 바뀌므로 `docker compose up -d`만으로는 기존 promtail 컨테이너가 고아로 남는다. `deploy-infrastructure.sh`가 `--remove-orphans`를 쓰는지 확인하거나, 1회 수동으로 `docker rm -f dev-promtail prod-promtail`(+ 사용처 없는 promtail 이미지/포지션 볼륨 정리)이 필요하다. promtail은 별도 볼륨을 쓰지 않고 positions 파일을 컨테이너 내부 `/tmp`에 두므로 상태 마이그레이션은 불필요하다.
- **prod 콜렉터 헬스 재점검.** #1399가 기록한 "prod-promtail unhealthy"는 콜렉터 교체로 자연 해소될 수 있으나, 배포 후 Loki에 dev/prod 로그가 정상 인입되는지(특히 prod의 KST→UTC 타임스탬프) 확인이 필요하다.
- **monitoring 무관.** `backend/docker/monitoring/`는 promtail/9080을 참조하지 않으므로(Alloy→Loki push 구조, cross-network scrape 없음) monitoring 변경은 불필요하며 본 작업 범위 밖이다.
- 브랜치는 `be/chore/promtail-to-alloy`다.

## 머지·배포 시 처리 작업 (체크리스트)

서비스 리네임(`*-promtail` → `*-alloy`)을 동반하는 변경이라 `docker compose up -d`만으로는 정합이 깨진다. 배포는 **backend-cd** 경로(`backend/**` push → SCP → `deploy-infrastructure.sh`)로 흐른다. 배포 담당이 순서대로 확인한다.

- [ ] **고아 컨테이너 정리.** `deploy-infrastructure.sh`가 `docker compose up -d --remove-orphans`를 쓰는지 확인. 아니면 1회 수동으로 `docker rm -f dev-promtail prod-promtail` 실행(서비스명이 바뀌어 기존 promtail 컨테이너가 고아로 잔존).
- [ ] **미사용 promtail 이미지/볼륨 정리.** promtail은 별도 볼륨 없이 positions를 컨테이너 내부 `/tmp`에 두므로 **상태 마이그레이션 불필요**. 사용처 없어진 `grafana/promtail` 이미지만 정리(`docker image prune` 또는 명시 삭제).
- [ ] **dev/prod 로그 인입 확인.** 배포 후 Loki에 dev·prod 애플리케이션 로그가 정상 인입되는지 확인. 특히 **prod의 KST→UTC(`Asia/Seoul`) 타임스탬프 변환**이 보존됐는지(로그 시각과 wall-clock 일치) 확인.
- [ ] **prod 콜렉터 헬스 재점검.** #1399가 기록한 "prod-promtail unhealthy"가 콜렉터 교체로 해소됐는지 확인(healthcheck는 미추가이므로 `docker ps` 상태가 아닌 Loki 인입 여부로 판정).
- [ ] **이미지 핀 적용 확인.** prod가 더 이상 `grafana/promtail:latest`가 아닌 `grafana/alloy:v1.16.3`로 고정 기동되는지 확인(`latest` 안티패턴 해소 검증).
- [ ] **후속 ADR-0028 전제.** ADR-0028(`import.git` pull 기반)을 이어서 머지한다면, **`be/prod` 브랜치에 `backend/docker/alloy/modules/app-logs.alloy`가 존재**해야 prod Alloy의 `import.git`이 실패하지 않는다(없으면 prod 로그 단절). 0028 머지 전 선행 확인.
