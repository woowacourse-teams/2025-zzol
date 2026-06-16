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

- `docker logs dev-alloy` 에서 `import.git` 오류·github egress 오류 확인.
- Prometheus `up{job="alloy"}` 가 0이면 `AlloyDown`,
  컴포넌트 unhealthy면 `AlloyComponentUnhealthy` 알림이 ADR-0026 Alertmanager로 전달된다.
- 모듈 자체는 GitHub의 해당 브랜치(`be/dev`/`be/prod`)에 존재해야 한다.

## 관련 문서

- [ADR-0027](../../docs/adr/0027-promtail-to-alloy-migration.md) — Promtail → Alloy 마이그레이션
- [ADR-0028](../../docs/adr/0028-alloy-pull-based-config-via-git.md) — Git pull 기반 설정 전환 + 운영 절차
