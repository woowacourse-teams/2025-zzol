# 0028. Alloy 로그 설정 Git pull 기반 전환 — import.git + be/prod 추종 prod 핀

- 날짜: 2026-06-12
- 상태: 제안

## 컨텍스트

ADR-0027로 Promtail을 Grafana Alloy로 교체하면서, Alloy 설정(`backend/docker/{dev,prod}/conf/config.alloy`)은 **호스트에 정적 bind-mount되고 backend-cd 경로로 배포**되도록 결정했다. 즉 로그 파이프라인을 한 줄만 고쳐도 backend-cd(앱) 파이프라인을 거쳐야 하고, 적용 시 컨테이너 재시작이 따른다.

Alloy는 Promtail에 없던 기능을 하나 갖고 있다 — `import.git` 설정 블록으로 **자기 설정 모듈을 Git 저장소에서 직접 pull**하고, `pull_frequency` 주기로 변경을 감지해 **핫리로드**한다(pull 기반 GitOps). 이 프로젝트의 GitOps는 현재 edge-cd(ADR-0023)의 push+rsync 수렴이 담당하지만, Alloy 설정은 그 범위 밖이고 Alloy의 네이티브 Git 기능도 쓰지 않는 상태였다.

요구는 명확하다: **로그 파이프라인 설정을 CD 파이프라인에서 떼어내, Alloy가 Git에서 직접 pull해 앱 재배포 없이 핫리로드하게 한다.** 이때 prod에 검증 안 된 커밋이 자동으로 흘러드는 것(blast radius)과, pull 기반에 검증 게이트가 없다는 점을 통제해야 한다.

확인한 전제:

- `import.git`은 **모듈**(`declare` 커스텀 컴포넌트)을 가져오고, 로컬 root config가 그 컴포넌트를 인스턴스화한다. 따라서 Alloy 시작에 필요한 **로컬 bootstrap config는 그대로 남는다.**
- 저장소 `woowacourse-teams/2025-zzol`은 **public**이다(`private:false`). → import.git에 **인증 블록·deploy key·시크릿 마운트가 전부 불필요**하다.
- dev/prod는 타임스탬프 처리가 다르다(ADR-0027 보존 제약). River에서 `stage.timestamp`는 argument로 토글할 수 없어, 단계 구성이 다른 두 컴포넌트가 필요하다.
- Prometheus는 현재 Alloy를 스크레이프하지 않는다. pull 기반은 "깨진 config를 받아도 Alloy가 죽지 않고 last-good 유지 + 컴포넌트만 unhealthy"라 **조용한 정체**가 생기는데, 이를 감지할 알림이 없다.

## 결정

**Alloy 설정을 bootstrap(로컬) + 파이프라인 모듈(Git pull)로 분리하고, dev는 `be/dev`·prod는 기존 prod 브랜치 `be/prod`를 추종한다(전용 브랜치 신설 없이 기존 브랜치 쌍 재사용). 동시에 Alloy self-metrics 스크레이프와 health 알림을 추가한다. 이는 ADR-0027의 "config 정적 bind-mount + backend-cd 배포" 결정 중 _config 내용 전달 방식_을 supersede한다(컨테이너·compose 서비스는 여전히 backend-cd).**

1. **파이프라인 로직을 Git 모듈로 분리한다.** `backend/docker/alloy/modules/app-logs.alloy`에 `declare` 2개를 둔다 — `app_logs_ingest_time`(타임스탬프 미파싱, dev)과 `app_logs_log_time`(KST 파싱, prod). 각 모듈은 `environment`·`job`을 argument로 받아 라벨에 주입한다. ADR-0027의 dev/prod 타임스탬프 차이를 그대로 보존한다. **이 파일은 호스트에 배포되지 않는다** — Alloy가 GitHub에서 직접 pull하므로 레포 해당 브랜치에 존재하기만 하면 된다.

2. **bootstrap config.alloy는 import + 인스턴스화만 남긴다.** `config.alloy`(dev/prod)는 ~15줄짜리 진입점으로 축소된다. `import.git`으로 모듈을 가져오고 커스텀 컴포넌트를 1개 인스턴스화한다. public 저장소라 인증 블록 없음.
   - **dev**: `revision = "be/dev"` — be/dev HEAD 직접 추종(모듈 변경 즉시 반영, 검증용).
   - **prod**: `revision = "be/prod"` — 기존 prod 브랜치만 추종. `pull_frequency = "60s"`.

3. **prod는 기존 `be/prod` 브랜치로 승격을 게이트한다(전용 브랜치 신설 안 함).** be/dev의 매 커밋이 60초 내 prod로 게이트 없이 자동 배포되는 것을 막는다(edge-cd가 self-path 제외·수동 dispatch로 막은 것과 같은 blast-radius 통제). `be/prod`는 이미 backend-cd가 prod 배포에 쓰는 보호 브랜치(PR·CI·브랜치 보호 적용)이므로, 그 관문이 그대로 Alloy 모듈 승격의 게이트가 된다 — **새 브랜치를 만들지 않고 기존 흐름을 재사용**한다. 전용 `release/alloy` 신설안은 "로그 설정을 앱 릴리스와 무관하게 독립 승격"하는 이점이 있으나, 이 파이프라인은 변경 빈도가 매우 낮아 그 이점이 영구적 브랜치 관리·보호 비용을 정당화하지 못해 채택하지 않는다. 불변 태그 핀 방식도 매 변경마다 bootstrap의 `revision`을 고쳐 호스트에 재배포해야 해 pull 분리 이점을 깎으므로 채택하지 않는다.

4. **Alloy self-metrics 스크레이프 + health 알림을 추가한다(검증 게이트 부재 보완).** `prometheus.yml`에 `alloy` job 추가(`dev-alloy:12345`, `prod-alloy:12345`, `env` 라벨). `alerts-infra.yml`에 룰 2개:
   - `AlloyDown`: `up{job="alloy"} == 0` 5분 → warning(로그 인입 단절).
   - `AlloyComponentUnhealthy`: `alloy_component_controller_running_components{health_type="unhealthy"} > 0` 10분 → warning. **잘못된 모듈을 pull했을 때의 조용한 정체를 잡는 핵심 룰**이다. 이 알림은 ADR-0026 Alertmanager로 전달된다.

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| pull 기반 import.git + 기존 be/prod 추종(채택) | 앱 재배포 없이 핫리로드, 설정/CD 분리, public이라 인증 불필요, **새 브랜치 0개**(기존 be/dev·be/prod 보호·게이트 재사용) | 검증 게이트 부재(→ health 알림 보완), github egress 의존, 모듈/bootstrap 2층 구조, prod 로그 설정 승격이 be/prod 릴리스 관문에 편승(독립 케이던스 없음) |
| pull 기반 import.git + 전용 `release/alloy` 신설 | 로그 설정을 앱 릴리스와 무관하게 독립 승격 | **영구 브랜치 1개 추가**(생성·보호·문서화·인지 비용), 배포 전 선생성 전제, 변경 빈도가 낮아 독립 케이던스 이점이 작음 |
| ADR-0027 유지(정적 bind-mount + backend-cd) | 단순, 추가 의존 없음 | 설정 한 줄 변경에도 앱 파이프라인·재시작, GitOps 자동화 없음 |
| Alloy 설정을 edge-cd로 이관(push 기반) | edge-cd의 검증·원복 게이트 재사용 | Alloy는 환경별·앱 로그볼륨 결합 → 단일 공유 monitoring 스택과 모델 불일치, pull 자동 reload 아님 |
| prod도 be/dev 직접 추종 | 구성 1개로 단순 | 검증 안 된 커밋이 60초 내 prod 자동 배포 — blast radius 통제 불가 |
| 불변 태그 핀(릴리스마다 새 태그) | "지금 prod에 뭐가 도나" 100% 결정적 | 매 변경마다 bootstrap 수정+호스트 재배포 → pull 분리 이점 상실 |

## 트레이드오프

- **blast radius는 revision 핀으로 통제한다.** pull 자동 reload는 곧 "커밋이 자동 배포된다"는 위험이다. prod를 `be/prod`에 핀해 PR 게이트를 강제하고, dev만 be/dev를 자유 추종한다. `be/prod`는 이미 보호·PR 게이트가 걸린 prod 배포 브랜치라 별도 보호 설정이 불필요하다. 이 분리가 깨지면(prod가 be/dev를 보면) edge-cd가 막았던 위험이 그대로 재발한다.
- **prod 로그 설정 승격이 be/prod 릴리스 관문에 편승한다.** 전용 `release/alloy`와 달리 로그 파이프라인만 따로 prod에 밀 수 없고 be/prod 머지를 타야 한다(앱 배포와 동일 관문). 단 ① import.git은 **파일 내용이 바뀔 때만** reload하므로 be/prod가 앱 코드로 전진해도 모듈 미변경이면 Alloy는 reload하지 않고 ② 이 파이프라인 변경 빈도가 매우 낮아, 독립 케이던스 상실의 실무 비용은 작다. 굳이 로그만 독립 승격이 필요해지면 그때 `release/alloy`를 도입해도 늦지 않다(역전 가능한 결정).
- **검증 게이트는 없지만 장애가 아니라 '정체'다.** edge-cd의 `nginx -t`+probe+원복 같은 사전 게이트가 pull 경로엔 없다. 다만 Alloy는 깨진 config를 pull해도 죽지 않고 last-good을 유지하므로 결과는 다운타임이 아닌 silent staleness다. `AlloyComponentUnhealthy` 알림이 이 구멍을 사후 감지로 닫는다(완전한 사전 게이트는 아님).
- **github.com egress에 의존한다.** Alloy 컨테이너가 GitHub(HTTPS 443)에 도달해야 모듈을 pull한다. dev/prod bridge 네트워크의 기본 NAT egress로 충족되나, GitHub 장애·방화벽 변경 시 pull이 멈춘다(이때도 last-good 유지). 배포 후 검증 항목.
- **모듈 내 중복.** dev/prod 타임스탬프 차이 때문에 regex·labels·source·write가 두 `declare`에 중복된다(~25줄). River에서 처리 단계를 argument로 토글할 수 없어 감수한 비용이며, 차이를 명시적으로 보존하는 편이 단일 모듈로 dev 동작을 바꾸는 것보다 안전하다.
- **두 배포 경로로 갈린다.** bootstrap config.alloy는 backend-cd, prometheus.yml·룰은 edge-cd(monitoring)로 흘러간다. 모듈은 어느 경로도 아닌 Alloy의 직접 pull. 한 변경이 세 경로로 나뉘는 인지 비용이 있으나, 각각의 케이던스·blast radius가 달라 의도된 분리다.

## 결과

- **신규 파일**: `backend/docker/alloy/modules/app-logs.alloy`(Git pull 모듈), `docs/adr/0028-alloy-pull-based-config-via-git.md`.
- **수정 파일**: `backend/docker/dev/conf/config.alloy`·`backend/docker/prod/conf/config.alloy`(파이프라인 → import.git bootstrap으로 축소), `backend/docker/monitoring/conf/prometheus.yml`(`alloy` 스크레이프 job), `backend/docker/monitoring/conf/rules/alerts-infra.yml`(`AlloyDown`·`AlloyComponentUnhealthy`), `docs/adr/index.md`(본 ADR 등록).
- **compose·시크릿 변경 없음.** public 저장소라 인증 불필요, bootstrap은 기존 bind-mount 그대로, egress는 bridge NAT 기본값. dev/prod docker-compose.yml 무수정.
- **배포 전제: be/prod에 모듈 파일 존재.** prod Alloy의 bootstrap이 `be/prod`를 pull하므로, **prod 배포 시점에 `be/prod`에 모듈 파일(`backend/docker/alloy/modules/app-logs.alloy`)이 포함돼 있어야 한다.** 없으면 import.git이 실패하고 prod 로그 파이프라인이 뜨지 않는다. 순서: 본 PR be/dev 머지 → be/dev를 be/prod로 승격(앱 prod 배포와 동일) → prod 배포. dev는 be/dev에 모듈이 있으므로 추가 조치 불필요. **전용 브랜치 선생성 단계가 사라져 원안의 배포 전제 리스크가 제거된다.**
- **검증 한계.** `alloy validate`는 import.git이 가리키는 원격 모듈까지는 오프라인 검증하지 못한다(bootstrap 문법만 검증). 모듈 자체는 dev에서 be/dev 추종으로 먼저 검증되는 셈이고, prod 승격 전 dev에서 정상 인입을 확인하는 것이 사실상의 게이트다.
- **배포 후 확인**: ① dev/prod Loki에 로그 정상 인입(prod의 KST→UTC 타임스탬프 포함) ② Prometheus `up{job="alloy"}==1` ③ 모듈 한 줄 수정 후 60초 내 dev 반영되는지(핫리로드 동작) ④ github egress 가용성.
- **브랜치는 `be/chore/alloy-pull-config`(`be/chore/promtail-to-alloy` 위 스택)다.** ADR-0027이 be/dev 미머지 상태라 그 위에 쌓이며, PR은 promtail-to-alloy를 타깃(또는 그게 be/dev 머지되면 rebase)한다.
- **ADR-0027 관계**: 0027의 "config 정적 bind-mount + backend-cd 배포" 결정 중 _config 내용 전달_ 부분을 supersede한다. 컨테이너 정의·이미지 핀(v1.16.3)·healthcheck 미추가·listen-addr 0.0.0.0·dev/prod 타임스탬프 차이는 0027 결정을 그대로 계승한다.
