---
description: 게임 전용 스케줄러 빈·Redis Stream 키·타이밍 프로퍼티를 프로덕션에 추가할 때 같은 diff에 넣어야 하는 테스트 미러 체크리스트. 누락 시 통합테스트 전체가 컨텍스트 로딩 실패·메시지 미수신으로 깨진다.
paths:
  - "**/src/main/java/**/config/*SchedulerConfig.java"
  - "**/src/main/java/**/config/*TaskSchedulerConfig.java"
  - "**/src/main/java/**/config/*TimingProperties.java"
  - "**/src/main/java/**/*StreamKey.java"
  - "**/src/main/resources/config/redis.yml"
  - "**/src/main/resources/config/game.yml"
  # 검증 지점 — 게임 IT를 만들 때 미러가 갖춰졌는지 확인한다
  - "**/src/test/java/**/*IntegrationTest.java"
  # 미러 파일 자체 — 한 곳을 고치면 나머지 두 곳도 같이 고쳐야 한다
  - "**/src/testFixtures/java/**/config/GameSchedulerTestConfig.java"
  - "**/src/test/java/**/config/IntegrationTestConfig.java"
  - "**/src/testFixtures/resources/application-test-game.yml"
  - "**/src/main/resources/application-test-base.yml"
---

## 전용 빈·스트림을 추가하면 테스트 미러를 같은 커밋에 넣는다

동적 타이머 게임(SpeedTouch·BlindTimer·Nunchi·WormGame)은 OCP 한 줄 등록(`MiniGameType` + Factory) 외에 **전용 스케줄러 빈·Redis Stream 키·타이밍 프로퍼티**를 추가한다. 이것들은 프로덕션 파일에만 등록하면 **도메인·서비스 단위 테스트는 통과하고 통합테스트만 깨진다** — 그것도 그 게임만이 아니라 같은 컨텍스트를 올리는 모듈의 IT 전체가.

지금 편집 중인 파일이 아래 왼쪽에 해당하면, 오른쪽을 **같은 diff**에 추가했는지 확인한다. SSOT는 `docs/architecture.md` → "전용 스케줄러·스트림을 쓰는 게임 — 테스트 미러링"이고, 사고 기록은 `docs/postmortem/0004-test-mirror-checklist-incomplete-recurrence.md`다.

| 프로덕션에 추가한 것 | 같은 diff에 넣을 테스트 미러 | 빠지면 |
| --- | --- | --- |
| `@Bean("xGameScheduler") @Profile("!test")` 전용 `TaskScheduler` | **같은 이름** 빈을 3곳 전부: ① `game/src/testFixtures/.../config/GameSchedulerTestConfig` → `new TestTaskScheduler()` ② `game/src/test/.../config/IntegrationTestConfig` → `new ShutDownTestScheduler()` ③ `app/src/test/.../support/app/config/IntegrationTestConfig` → `new ShutDownTestScheduler()` | `NoSuchBeanDefinitionException: TaskScheduler` — 미러가 빠진 모듈의 IT 무더기 실패 (ADR-0031 1차 PR #1484에서 ③ 누락으로 55건) |
| `redis.yml`의 `redis.stream.keys."[xgame]"` | `test-support/src/main/resources/application-test-base.yml`의 같은 키(`thread-pool-name`·`max-length` 동일). 공유 풀(`concurrent`)을 쓰면 `core-size ≥ 그 풀을 쓰는 스트림 수`인지도 확인 (ADR-0022) | "메시지 미수신" 타임아웃 — 컨슈머가 구독할 스트림이 테스트 설정에 없다 |
| `game.yml`의 `x-game.timing.*` (`@ConfigurationProperties` + `@NotNull`) | `game/src/testFixtures/resources/application-test-game.yml`의 같은 키(IT 가속값) | `@Validated` 바인딩 실패로 게임 컨텍스트를 올리는 모든 테스트 기동 실패 |

**3곳 중 하나만 고치면 안 되는 이유**: 세 설정은 서로 다른 테스트 컨텍스트가 import한다. ①은 game·service 테스트, ②는 game IT, ③은 app 전체 컨텍스트 IT — 한 곳을 빠뜨리면 그 곳을 쓰는 모듈만 깨져서 로컬에서 "내 모듈 테스트는 통과"로 보인다.

검증: 그 게임의 `*IntegrationTest`(플로우 해피패스 1개, `testing-integration.md`)를 **Docker 켜고** 실제로 돌린다. IT가 초록이면 세 미러가 전부 있다는 뜻이다.
