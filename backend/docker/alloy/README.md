# Alloy 로그 수집기 운영 런북

Grafana Alloy로 앱 로그를 Loki에 적재한다(Promtail 후속, ADR-0027). 설정은
**bootstrap(호스트 bind-mount) + 파이프라인 모듈(Alloy가 GitHub에서 직접 pull)**
2층 구조다(ADR-0028).

```text
config.alloy (호스트 1회 배치, ~15줄 진입점)
  └─ import.git → backend/docker/alloy/modules/app-logs.alloy (Alloy가 직접 pull)
       └─ loki.process → loki.write → Loki
```

- dev bootstrap: `be/dev` 추종(모듈 변경 즉시 반영)
- prod bootstrap: `be/prod` 추종(PR 게이트로 blast radius 통제)

## bootstrap 호스트 1회 배치 (★ 필수)

backend-cd는 `conf/`를 서버로 전송하지 않는다(SCP는 `docker-compose.yml`·
`deploy-*.sh`·`.env`만). 따라서 bootstrap `config.alloy`는 **호스트에 1회 수동
배치**해야 한다. 안 하면 Docker가 마운트 소스를 빈 디렉터리로 자동 생성해
`not a directory` 오류로 alloy가 뜨지 않는다.

```bash
# 로컬 레포에서 (dev 예시; prod는 dev→prod 치환)
scp backend/docker/dev/conf/config.alloy <user>@<host>:~/dev/conf/config.alloy
```

`conf/`는 CD가 덮어쓰지 않으므로 **한 번 배치하면 영속**한다. 이후
backend-cd부터 `deploy-infrastructure.sh`가 alloy를 자동 기동한다.

## 기동 방법

```bash
# CD가 자동 수행 (deploy-infrastructure.sh 내부)
# 수동 재기동이 필요하면 deploy 디렉터리에서:
cd ~/dev   # 또는 ~/prod
docker compose --env-file .env up -d dev-alloy   # prod는 prod-alloy
```

> ⚠️ bootstrap이 없는 상태에서 `docker compose up`을 직접 실행하면 Docker가
> `conf/config.alloy`를 빈 디렉터리로 만들어 마운트 오류가 난다. **항상 bootstrap을
> 먼저 배치**하고, 가급적 CD/`deploy-infrastructure.sh` 경로로만 기동한다.

## 트러블슈팅

### `not a directory: ... mount config.alloy`

bootstrap 파일이 없어 Docker가 디렉터리를 자동 생성한 경우다.

```bash
cd ~/dev
docker compose --env-file .env rm -sf dev-alloy
rm -rf conf/config.alloy        # 잘못 생성된 빈 디렉터리 제거
mkdir -p conf
# 로컬에서 scp로 실제 config.alloy 배치 (위 "1회 배치" 참조)
docker compose --env-file .env up -d dev-alloy
```

### 로그가 Loki에 안 들어옴 / 모듈 pull 실패

`docker logs dev-alloy` 에서 pull 오류를 찾는다(정확한 문구는 Alloy 버전에 따라 다를 수 있어 키워드로 grep — 아래는 예시):

- `import.git` / `unhealthy` — 모듈 pull·평가 실패
- `dial tcp github.com:443` / `connection refused` / `i/o timeout` — github egress 차단(방화벽·NAT). 예: `dial tcp github.com:443: connect: connection refused`
- `404`(경로·브랜치 오타) / `403`(rate limit·접근) — import.git `path`/`revision` 점검

알림(ADR-0026 Alertmanager 전달):

- **`AlloyDown`** — `up{job="alloy"} == 0` 이 **5분** 지속(컨테이너 다운 또는 스크레이프 단절).
- **`AlloyComponentUnhealthy`** — unhealthy 컴포넌트가 **10분** 지속(깨진 모듈 pull 등). Alloy는 last-good config로 계속 동작 — 다운이 아닌 '정체'다.

모듈 자체는 GitHub의 해당 브랜치(`be/dev`/`be/prod`)에 존재해야 한다.

## 관련 문서

- [ADR-0027](../../docs/adr/0027-promtail-to-alloy-migration.md) — Promtail → Alloy 마이그레이션
- [ADR-0028](../../docs/adr/0028-alloy-pull-based-config-via-git.md) — Git pull 기반 설정 전환 + 운영 절차
