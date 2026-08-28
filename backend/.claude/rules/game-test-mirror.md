---
description: 게임 전용 스케줄러 빈·Redis Stream 키·타이밍 프로퍼티를 프로덕션에 추가할 때 같은 diff에 넣어야 하는 테스트 미러. 판정은 :app 테스트가 하고, 이 문서는 어디를 고칠지만 알려준다.
paths:
  - "**/src/main/java/**/config/*SchedulerConfig.java"
  - "**/src/main/java/**/config/*TaskSchedulerConfig.java"
  - "**/src/main/java/**/config/*TimingProperties.java"
  - "**/src/main/java/**/*StreamKey.java"
  - "**/src/main/resources/config/redis.yml"
  - "**/src/main/resources/config/game.yml"
  # 미러 파일 자체
  - "**/src/testFixtures/java/**/config/GameSchedulerTestConfig.java"
  - "**/src/testFixtures/java/**/config/IntegrationSchedulerTestConfig.java"
  - "**/src/testFixtures/resources/application-test-game.yml"
  - "**/src/main/resources/application-test-base.yml"
---

## 전용 빈·스트림을 추가하면 테스트 미러를 같은 커밋에 넣는다

동적 타이머 게임(SpeedTouch·BlindTimer·Nunchi·WormGame)은 OCP 한 줄 등록(`MiniGameType` + Factory) 외에 **전용 스케줄러 빈·Redis Stream 키·타이밍 프로퍼티**를 추가한다. 프로덕션에만 등록하면 **도메인·서비스 단위 테스트는 통과하고 통합테스트만 깨진다.**

| 프로덕션에 추가한 것 | 같은 diff에 넣을 미러 |
| --- | --- |
| `@Bean("xGameScheduler") @Profile("!test")` 스케줄러 | **같은 이름** 빈을 두 곳에: ① `game/src/testFixtures/.../config/GameSchedulerTestConfig`(서비스 테스트용 — mock/`TestTaskScheduler`) ② 같은 디렉터리의 `IntegrationSchedulerTestConfig`(IT용 — `ShutDownTestScheduler`, game IT와 app IT가 공유) |
| `redis.yml`의 `redis.stream.keys."[xgame]"` | `test-support/src/main/resources/application-test-base.yml`의 같은 키 |
| `game.yml`의 `x-game.timing.*` | `game/src/testFixtures/resources/application-test-game.yml`의 같은 키(IT 가속값) |

**빠뜨리면 `:app`의 `SchedulerMirrorTest`·`ConfigMirrorTest`가 실패한다** — Docker도 컨텍스트 로딩도 없이 몇 초 안에. 어떤 이름이 빠졌는지 단언 메시지가 그대로 알려주므로, 이 표를 눈으로 대조할 일은 없다.

배경(왜 세 곳이 아니라 두 곳인가, 왜 IT 55건이 한꺼번에 깨졌었나): `docs/architecture.md` → "전용 스케줄러·스트림을 쓰는 게임 — 테스트 미러링", `docs/postmortem/0004-test-mirror-checklist-incomplete-recurrence.md`.
