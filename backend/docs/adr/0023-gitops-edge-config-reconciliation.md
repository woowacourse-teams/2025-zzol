# 0023. GitOps 엣지 설정 동기화 — nginx/monitoring reconciliation과 edge-cd 분리

- 날짜: 2026-06-09
- 상태: 제안

## 컨텍스트

운영 중 nginx 설정이 조용히(silent) 깨지는 인시던트가 발생했다. 원인을 추적하면서 다음 사실이 드러났다.

서버의 `~/nginx/conf`, `~/monitor/conf`는 Git의 `backend/docker/nginx/conf`,
`backend/docker/monitoring/conf`와 **동기화를 강제하는 메커니즘이 없었다.**

- `backend-cd.yml`의 SCP 단계는 `docker/{env}/docker-compose.yml`,
  `deploy-*.sh`, `.env`만 서버로 옮긴다. nginx/monitoring 설정 디렉터리는 **전송 대상이 아니다.**
- 이름이 `deploy-infrastructure.sh`인 스크립트는 실제로는 **MySQL/Redis만** 기동한다.
  nginx·Prometheus·Grafana·Loki·Tempo 설정에는 관여하지 않는다.
- CD가 nginx에서 건드리는 파일은 오직 `~/nginx/conf/{env}-service.inc` 하나다.
  `switch_nginx_upstream()`(deploy-utils.sh)이 Blue/Green 업스트림 포인터를 런타임에 다시 쓴다.

즉 **구조 설정 파일 전체와 모든 monitoring 설정이 어떤 CD에도 걸리지 않는 미관리 영역**이었고,
서버에서 직접 수정한 변경이 Git으로 환류되지 않아 양쪽이 표류(drift)했다. 인시던트의 본질은
"설정이 사라졌다"가 아니라 **GitOps 동기화 규율의 부재**였다.

진단을 위해 서버와 Git을 대조하자 drift가 **양방향이고 품질이 혼재**했다.

- 서버에만 있는 운영 필수: Let's Encrypt ACME 챌린지 처리(`acme-challenge.inc` + 인증서 갱신),
  Prometheus prod exporter·라벨, Loki `creation_grace_period`.
- Git에만 있는 보안 하드닝: rate limiting zone(`00-common.conf`), 강한 SSL 암호 스위트,
  `ssl_session_tickets off`, `X-Forwarded-For $proxy_add_x_forwarded_for`(올바른 체인 누적).
- 서버에서 회귀한 항목: `X-Forwarded-For $remote_addr`(체인 덮어씀), 약한 암호 스위트(`HIGH:!aNULL:!MD5`).

어느 한쪽도 일률적인 "진실"이 아니었다. 따라서 "서버를 통째로 채택"하면 보안 하드닝을 잃고,
"Git을 그대로 배포"하면 인증서 갱신과 prod 메트릭을 깬다.

## 결정

세 가지를 함께 결정한다.

1. **엣지 설정 배포를 애플리케이션 배포에서 분리한다.** nginx(엣지)와 monitoring 스택의
   설정을 관리하는 워크플로우 `edge-cd.yml`을 신설한다. `backend-cd.yml`과 분리하는 이유는
   변경 케이던스(앱은 잦고 인프라는 드묾)와 영향 범위(앱 빌드 ~10분 vs 설정 reload 수초)가
   다르기 때문이다. 끼워넣으면 nginx 한 줄 수정에 앱 풀빌드를 강제하게 된다.

2. **서버 ↔ Git을 일회성 큐레이션 병합으로 수렴시킨다.** drift가 양방향·품질 혼재이므로
   파일 종류별로 병합 전략을 달리한다.
   - **conf 파일은 best로 병합한다.** nginx 설정 내용은 `nginx -s reload`로 적용되어
     싸고 가역적이며 프로브로 보호할 수 있으므로, Git의 보안 하드닝과 서버의 인증서/ACME를
     합친 교정본을 만든다. (예: `ssl-common.inc` = Git 암호 스위트 + 서버 `zzol.site` 인증서)
   - **compose 파일은 서버 현실을 채택하고 개선은 후속 PR로 미룬다.** `docker compose up -d`는
     컨테이너 재생성이라 무겁고 변경이 상호의존적이다(예: 서버가 `nginx:alpine`→`nginx:latest`로
     바꾸면서 Debian 이미지에 `wget`이 없어 healthcheck를 함께 제거함 — cherry-pick 불가).

3. **edge-cd의 적용 단계는 백업-원복 패턴을 따른다.** 레포가 이미 가진 `switch_nginx_upstream()`의
   규율(이전 설정 백업 → 쓰기 → `nginx -t` → 실패 시 원복+reload)을 구조 설정 동기화에도 미러링한다.
   적용 순서는 **백업 → sync → `docker compose up -d` → `nginx -t` → reload → 실제 경로 프로브 →
   실패 시 원복**이다. 프로브는 실제로 깨졌던 경로(`/ws/info`, `status.zzol.site`→Grafana)를 때린다.
   `nginx -t`는 문법만 검증하므로 "문법은 통과하지만 라우팅이 틀린" 인시던트를 막지 못한다.

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| `backend-cd.yml`에 설정 동기화 끼워넣기 | 워크플로우 1개로 단순 | 앱·인프라 변경 도메인이 섞임, 설정 한 줄에 앱 풀빌드 강제 |
| 파일만 옮기고 reload는 수동 | 구현 최소 | "옮겼는데 안 켰다" 휴먼에러 — 이번 인시던트와 동종 |
| 서버 → Git 통째 채택(첫 배포 no-op) | 첫 배포가 무사고 | 보안 하드닝(암호·XFF·session_tickets) 회귀 |
| Git → 서버 통째 배포 | Git이 단일 진실 | 인증서 갱신(ACME)·prod 메트릭 파괴 |
| conf=best 병합 + compose=서버 채택 + 백업-원복 (채택) | drift를 교정 상태로 치유, 첫 배포부터 보호 | 큐레이션이 수작업, compose 개선이 후속으로 분리됨 |

## 트레이드오프

- **첫 배포는 순수 no-op이 아니다.** reconciliation 후 Git은 서버보다 세 곳(암호 하드닝,
  XFF 교정, inert한 rate-limit zone) 앞선다. edge-cd 첫 실행은 이 차이를 적용하는
  "통제된 치유"이며, 그래서 백업-원복과 실제 경로 프로브가 첫 실행부터 필수다.
- **compose 개선이 뒤로 밀린다.** nginx 이미지 핀(`:latest` 제거), healthcheck 재도입,
  Loki `user` 고정, Grafana alerting 마운트는 별도 후속 PR로 다룬다. `:latest`가 남는 동안은
  이미지 드리프트 위험이 잔존한다.
- **인증서가 단일 `zzol.site` SAN 인증서로 통합된다.** dev/prod/status 서브도메인별 인증서
  지정을 제거하고 `ssl-common.inc`의 공통 인증서에 의존한다. 갱신 시 모든 SAN을 유지해야 한다.
- **큐레이션은 자동화할 수 없다.** 양방향·품질 혼재 drift는 사람이 헝크 단위로 판단해야 한다.
  이 일회성 비용을 지불해야 이후 GitOps가 성립한다.

## 결과

- `edge-cd.yml`은 `backend-cd.yml`과 동일하게 **main과 be/dev 양쪽**에 둔다. push 트리거는
  푸시된 브랜치의 워크플로우 파일로 실행되므로, 한쪽만 두면 드리프트가 다시 쌓인다.
- edge-cd는 `*-service.inc`(라이브 Blue/Green 포인터)를 **절대 동기화하지 않는다.** Git의 값은
  템플릿이고 서버 실물은 deploy-application.sh가 런타임에 다시 쓴다. 통째 복사 시 트래픽을
  죽은 컨테이너로 보낼 수 있다. SCP는 glob 제외가 안 되므로 `rsync --exclude='*-service.inc'`를 쓴다.
- nginx/monitoring 설정 파일은 Linux 컨테이너로 배포되므로 `.gitattributes`로 LF를 고정한다.
  Windows에서 편집·커밋해도 CRLF로 변환되지 않아 `nginx -t` 오류와 무의미한 diff를 방지한다.
- monitoring `.env` 시크릿(`GRAFANA_ADMIN_PASSWORD` 등)은 Git에 두지 않는다. 서버의 기존
  `.env`를 유지하거나 backend-cd처럼 GitHub 시크릿으로 주입한다.
- 향후 Alertmanager 도입(Prometheus 알림 룰의 GitOps 관리)은 이 워크플로우 위에서 진행한다.
