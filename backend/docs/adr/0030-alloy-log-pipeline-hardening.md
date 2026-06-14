# 0030. Alloy 로그 파이프라인 강화 — 멀티라인·regex 버그 수정과 라이브 디버깅 UI

- 날짜: 2026-06-14
- 상태: 제안

## 컨텍스트

ADR-0027로 Promtail을 Alloy로 교체하고 ADR-0028로 로그 파이프라인을 Git pull 모듈(`backend/docker/alloy/modules/app-logs.alloy`)로 분리한 뒤, Alloy가 Promtail보다 제공하는 기능들을 활용할 수 있는지 검토했다. 후보는 다섯이었다.

1. Java 스택트레이스 멀티라인 묶기
2. 로그 기반 메트릭(`stage.metrics`)
3. PII/시크릿 마스킹(`stage.replace`/`luhn`)
4. 로그 볼륨 제어(`stage.drop`/`sampling`)
5. self-observability(라이브 디버깅 웹 UI + `alloy_component_*` 메트릭)

**각 후보가 이 코드베이스에서 실제로 가치가 있는지 근거를 확인**한 결과, 대부분은 죽은 설정이었고 그 과정에서 더 중요한 **latent 버그**를 발견했다.

### (발견) 로그 파싱 regex가 현재 로그 포맷과 매치되지 않는다

`app/src/main/resources/logback-spring.xml`의 `FILE_LOG_PATTERN`은 다음이다.

```text
[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%level] [%X{traceId:-},%X{spanId:-}] --- [%thread] %logger{36} : %msg%n
```

그런데 모듈의 `stage.regex`는 `[ts] [level] --- [thread] ...`로 **`[traceId,spanId]` 구간이 빠져 있어** 매치에 실패한다. git 히스토리로 원인을 확정했다.

- `d51f096a`(#1064): `[ts] [level] --- [thread]` 포맷과 promtail regex를 **같은 커밋에** 추가 — 당시엔 정확히 매치됐다.
- `5d72b73c`: `FILE_LOG_PATTERN`에 `[%X{traceId:-},%X{spanId:-}]`를 추가(트레이스 가독성). **regex는 함께 고치지 않았다.**

즉 `5d72b73c` 이후 파일 로그 모든 줄에 regex가 모르는 `[traceId,spanId]` 구간이 생겨 매치가 깨졌다. 결과(매치 실패 시 추출 맵이 비므로):

- `stage.labels`의 `level`이 추출되지 않아 **Loki에서 레벨 필터 불가**.
- prod의 `stage.timestamp`가 `source = "timestamp"`를 못 찾아 **KST→UTC 변환이 no-op**(인입 시각 폴백) — ADR-0027이 보존하려던 바로 그 처리가 사실상 동작하지 않고 있었다.

> regex가 포맷과 다르다는 것은 정적으로 확정된 사실이다. 런타임 영향(레벨 라벨·KST 변환 무력화)은 위 메커니즘상의 귀결로, 배포 후 Loki에서 1회 확인을 권한다(아래 결과).

### 후보별 근거 확인

- **#1 멀티라인 — 필요(채택).** 현재 모듈에 `stage.multiline`이 없어 Java 예외 스택트레이스가 줄 단위로 쪼개져 Loki에 인입된다. continuation 줄은 위 regex의 `^[ts]` 앵커에도 안 걸려 라벨/시각도 못 받는다.
- **#2 로그→메트릭 — 중복(기각).** `infra/build.gradle.kts`가 `micrometer-registry-prometheus` + actuator를 포함해 Spring Boot가 `logback_events_total{level=...}`를 `/actuator/prometheus`로 이미 노출하고 Prometheus가 스크레이프 중이다. 레벨 카운트를 Alloy에서 또 만들면 동일 신호 중복이다.
- **#3 PII 마스킹 — 대상 없음(기각).** 토큰 관련 로그는 `StompPrincipalInterceptor`(`log.warn("...검증 실패: {}", e.getMessage())`)와 `JwtAuthenticationFilter`(`log.debug(...e.getMessage())`) 둘뿐이고, 둘 다 **예외 메시지**만 찍지 토큰 값을 찍지 않는다. 마스킹 룰이 매치할 대상이 없어 죽은 설정이 된다.
- **#4 볼륨 제어 — 노이즈 없음(기각).** INFO 로그는 모두 스케줄러 시작/완료·복구·기동 1회성으로 per-request·per-tick 고빈도 로거가 없다. root=INFO라 DEBUG/TRACE drop도 매치 대상이 없다.
- **#5 self-observability — 일부 신규(부분 채택).** `alloy_component_*` 스크레이프와 health 알림(`AlloyDown`/`AlloyComponentUnhealthy`)은 **ADR-0028이 이미 추가**했다. 남은 신규 항목은 **라이브 디버깅 웹 UI 접근**뿐이다. ADR-0027이 `listen-addr=0.0.0.0:12345`로 열어둔 것이 이 활용의 포석이었다.

## 결정

**멀티라인 묶기 + regex 버그 수정을 적용하고, 라이브 디버깅 UI를 호스트 loopback + SSH 터널로 접근 가능하게 한다. #2·#3·#4는 위 근거로 채택하지 않는다.**

1. **`stage.multiline`을 두 declare(`app_logs_ingest_time`/`app_logs_log_time`)에 추가한다.** `firstline = "^\[\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}\]"`로 타임스탬프 브래킷으로 시작하지 않는 줄을 직전 엔트리에 합친다. `max_lines`는 기본 128(초과분은 다음 엔트리로 분할 — 유실 아님), `max_wait_time = "3s"`.

2. **regex를 실제 포맷에 맞게 고치고 멀티라인 대응을 넣는다.**
   - `(?:\[(?P<trace>[^\]]*)\] )?` — `[traceId,spanId]` 구간을 **옵션**으로 매치(트레이스 있는/없는 줄, 구포맷 모두 수용). `traceId`는 고카디널리티라 추출만 하고 라벨로 쓰지 않는다.
   - `(?s)` 플래그 — `.`이 개행을 포함하게 해 병합된 멀티라인 엔트리 전체를 매치한다. **이게 없으면** 스택트레이스 엔트리에서 regex가 매치 실패해 `level`/`timestamp`가 추출되지 않는다(즉 멀티라인을 켠 의미가 사라진다).

3. **라이브 디버깅 UI를 활성화하고 loopback으로만 노출한다.** bootstrap config(dev/prod)에 `livedebugging { enabled = true }`를 추가하고, compose에서 `127.0.0.1:12345:12345`(dev) / `127.0.0.1:12346:12345`(prod, 동일 호스트 공존 시 충돌 회피)로 publish한다. **공개 노출은 없으며** 접근은 SSH 터널로만 한다(아래 런북). 활성 스트리밍 중에만 오버헤드가 발생한다.

4. **#2·#3·#4는 추가하지 않는다.** 근거는 컨텍스트 참조. 죽은/중복 설정을 prod 로그 파이프라인에 넣지 않는다(요청된 항목이라도 근거가 없으면 보류). 향후 토큰을 실제로 로깅하게 되면 #3을, 고빈도 로거가 생기면 #4를 재검토한다.

## 라이브 디버깅 UI 접근 런북 (SSH 터널)

Alloy는 원격 서버 컨테이너에서 돌고, UI 포트는 호스트 `127.0.0.1`에만 열려 공개되지 않는다. 로컬에서 보려면 SSH 로컬 포워딩으로 터널을 연다.

`~/.ssh/config`에 한 번만 등록한다.

```text
Host alloy
    HostName <서버주소>
    User <배포계정>
    IdentityFile ~/.ssh/<your-key>.pem
    LocalForward 12345 localhost:12345
    LocalForward 12346 localhost:12346
```

이후 접근은 한 줄이다.

```bash
ssh alloy
```

세션을 연 채 로컬 브라우저에서 접속한다.

- dev UI: `http://localhost:12345`
- prod UI: `http://localhost:12346`

세션을 닫으면 터널도 사라진다. 노출은 SSH 접근 권한(키 보유자)에 종속되므로 별도 인증이 필요 없다.

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| 멀티라인+regex 수정(채택) | 스택트레이스 1엔트리화, 레벨 필터·KST 변환 복구, 하위호환 | River regex 복잡도 소폭↑ |
| regex를 트레이스 필수로 수정 | 식 단순 | 트레이스 없는 startup 로그·구포맷 줄 매치 실패(옵션 처리가 더 안전) |
| #2 로그→메트릭 추가 | 앱 무관 메트릭 가능 | `logback_events_total`과 **중복** — 혼란만 |
| #3 PII 마스킹 추가 | 방어적 | 현재 토큰을 로깅하지 않아 **매치 대상 없음**(죽은 설정) |
| #4 볼륨 drop 추가 | Loki 비용↓ | 고빈도 로거 부재로 **drop 대상 없음**(죽은 설정) |
| UI를 nginx로 상시 공개 | 터미널 불필요 | 디버깅 엔드포인트 공개 노출↑, 인증 설정 필요 |
| UI를 loopback+SSH 터널(채택) | 공개 노출 0, 추가 인증 불필요, 설정 최소 | 접근 시 SSH 세션 필요(별칭으로 `ssh alloy` 한 줄) |

## 트레이드오프

- **`alloy validate`는 이 regex 버그를 못 잡는다.** validate는 문법·컴포넌트 결선만 본다. "컴파일은 되나 아무것도 매치 안 하는" 식은 validate를 통과한다. 그래서 **실제 로그 라인(스택트레이스 포함)으로 regex를 검증**했다(아래 결과). validate는 문법 게이트로 별도 수행한다.
- **멀티라인 `max_lines` 한계.** 기본 128줄. 매우 깊은 cause 체인은 분할될 수 있으나 유실은 아니며, 한도를 키우면 버퍼 메모리가 증가한다. 기본값을 유지하고 실측 후 조정한다.
- **regex 수정의 배포 경로.** 본 변경은 ADR-0028 모듈(Git pull) 위에 얹힌다. 모듈은 dev=`be/dev`, prod=`be/prod` 추종이라 **0028이 배포된 환경에서만** 반영된다. 만약 0027만 배포되고 0028이 지연되면, 같은 깨진 regex가 0027 정적 config(`docker/{dev,prod}/conf/config.alloy`)에 남으므로 그 경우 동일 수정을 0027 config에도 백포트해야 한다.
- **라이브 디버깅 오버헤드.** UI를 실제로 보고 있을 때만 데이터가 스트리밍된다. 평상시 비용은 무시 가능하며, 접근이 SSH 터널로 제한돼 상시 부하 유발 경로가 없다.

## 결과

- **수정 파일**:
  - `backend/docker/alloy/modules/app-logs.alloy` — 두 declare에 `stage.multiline` 추가 + `stage.regex` 수정(`(?s)` + 트레이스 옵션 구간).
  - `backend/docker/dev/conf/config.alloy`, `backend/docker/prod/conf/config.alloy` — `livedebugging { enabled = true }`.
  - `backend/docker/dev/docker-compose.yml`(`127.0.0.1:12345:12345`), `backend/docker/prod/docker-compose.yml`(`127.0.0.1:12346:12345`).
  - `docs/adr/index.md`(본 ADR 등록).
- **regex 검증(node)**: OLD식은 트레이스 포함 라인에 매치 실패(버그 재현), 구포맷엔 매치(과거 동작 확인). NEW식은 트레이스/빈트레이스(`[,]`)/구포맷/스택트레이스 병합본 전부 매치하고 `timestamp`·`level`을 정상 추출. `(?s)` 제거 시 스택트레이스 매치 실패(load-bearing 확인). `firstline`은 첫 줄만 매치(continuation 미매치).
- **validate 미수행(Docker down) — 머지 전 필수 게이트.** dev는 `be/dev` HEAD를 60초 내 핫풀하므로 validate는 **pre-merge 게이트**다(dev-alloy 캐년리는 백스톱). 단 bootstrap config는 `import.git`으로 **원격 be/dev(=구 모듈)**를 당기므로 그대로 validate하면 *내 모듈 변경을 검증하지 못한다.* 로컬 모듈을 `import.file`로 인스턴스화하는 임시 root로 검증한다.

  ```text
  # /work/alloy/validate-root.alloy (임시)
  import.file "shipping" { filename = "/work/alloy/modules/app-logs.alloy" }
  shipping.app_logs_ingest_time "dev"  { environment = "dev"  job = "dev-app" }
  shipping.app_logs_log_time   "prod" { environment = "prod" job = "prod-app" }
  livedebugging { enabled = true }
  ```

  ```bash
  # 모듈(멀티라인/regex) + livedebugging 동시 검증 — 네트워크 불필요
  docker run --rm -v "$PWD/docker:/work" grafana/alloy:v1.16.3 \
    validate /work/alloy/validate-root.alloy
  ```

- **livedebugging 수용성 확인.** `livedebugging` 블록은 v1.16.3에서 GA로 stability 플래그가 불필요하다(1.3 experimental 도입 후 GA 승격). validate가 이 블록에서 오탐하던 버그(#3769)는 v1.9.0 문제로 PR #3785에서 수정돼 v1.16.3엔 포함된다. 따라서 위 validate가 정상 게이트로 동작한다.

- **배포 후 확인 1회**: Loki에서 (a) `level` 라벨이 다시 채워지는지, (b) prod 로그 시각이 인입 시각이 아닌 로그의 KST 시각(UTC 변환)으로 들어오는지, (c) 예외가 1엔트리로 묶이는지 확인한다.
- **#5 self-metrics는 본 ADR 범위 밖**(ADR-0028이 이미 추가). 본 ADR은 라이브 UI 접근만 더한다.
- 브랜치는 `be/chore/alloy-log-pipeline-hardening`(ADR-0028 위 스택)다.
