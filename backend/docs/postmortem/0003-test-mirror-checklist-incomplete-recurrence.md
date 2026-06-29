# 0003. 테스트 미러링 체크리스트 불완전 — 문서화 후 재발

- 날짜: 2026-06-22
- 심각도: P2 (CI 차단·개발 생산성, 사용자 영향 없음)
- 상태: 해결 (수정 머지 대기)
- 관련 ADR: [0031 눈치게임(Nunchi)](../adr/0031-nunchi-game-design.md)
- 관련 포스트모템: [0001 게임 통합테스트 플레이키](0001-game-integration-test-flaky.md)

## 요약

새 게임(Nunchi)이 전용 `TaskScheduler` 빈(`nunchiGameScheduler`)을 프로덕션에 등록하면서, 테스트측 스케줄러 미러를 빠뜨렸다. 단 하나의 빈 누락이 같은 Spring 컨텍스트를 공유하는 **`app` 모듈 통합테스트 약 55건을 무더기로 실패**시켰다(PR #1484 CI). 표면적으로는 `SecurityConfigTest`·`RoomRestController`·`OutboxE2ETest`·`RedisStreamContextPropagationTest` 등 Nunchi와 무관한 클래스가 깨져 원인이 분산돼 보였으나, 실제 원인은 컨텍스트 로딩 실패 하나였다.

이 회고의 핵심은 "빈을 빠뜨렸다"가 아니다. **이 사고는 ADR-0031 1차 작업에서 이미 겪고 `architecture.md`에 미러링 체크리스트 섹션까지 작성한 뒤에 재발했다.** 그 체크리스트가 스케줄러 미러 위치를 3곳 중 1곳만 열거했기 때문에, 문서를 따랐는데도 빠진 곳이 남았다. 즉 **불완전한 체크리스트가 "다 했다"는 착각을 만들어 재발을 막지 못한** 프로세스 회고다.

## 타임라인

| 시점 | 사건 |
|------|------|
| ADR-0031 1차 | Nunchi 전용 스케줄러/스트림 추가. 테스트 미러 누락으로 IT 일부 실패 → `architecture.md`에 "전용 스케줄러·스트림 테스트 미러링" 섹션·표 작성. 단, 스케줄러 미러를 `game/.../IntegrationTestConfig` **1곳만** 열거 |
| 1차 수정 | `game/src/testFixtures/.../GameSchedulerTestConfig`, `game/src/test/.../IntegrationTestConfig`에 `nunchiGameScheduler` 추가. `app/src/test/.../support/app/config/IntegrationTestConfig`는 누락(체크리스트에 없었음) |
| PR #1484 CI (2026-06-22 03:5x) | `:app:test` 컨텍스트 로딩 실패. `SecurityConfigTest`·`RoomRestController`·`OutboxE2ETest` 등 약 55건 FAILED. annotation은 테스트명만 노출(원인 분산처럼 보임) |
| 진단 | 초기엔 무관해 보이는 광범위 실패 → 단일 컨텍스트 로딩 실패로 가설 좁힘. `--log-failed`는 예외 타입만, 메시지는 `build/test-results/**/*.xml`에서 추출: `nunchiFlowOrchestrator` → `No qualifying bean of type 'TaskScheduler' @Qualifier("nunchiGameScheduler")` |
| 수정 (`cd0f7836`) | `app/.../support/app/config/IntegrationTestConfig`에 `blindTimerGameScheduler` 패턴 그대로 `nunchiGameScheduler` 빈 추가(5줄). `:app:test` 전체 통과 확인 |

## 임팩트

- PR #1484의 `Backend Test Results` 체크 FAILURE로 머지 차단. `:app:test` 약 55건 무더기 실패.
- 도메인·서비스 단위 테스트(`game` 모듈)는 통과 — `game` 테스트는 `nunchiGameScheduler`를 가진 `GameSchedulerTestConfig`를 import하므로. 그래서 로컬·게임 모듈 단계에선 누락이 드러나지 않았다.
- 사용자 영향 없음.

## 근본 원인

- **불완전한 체크리스트(직접 원인).** 스케줄러 빈 미러는 모듈별로 3곳에 존재한다 — ① `game` testFixtures `GameSchedulerTestConfig`, ② `game` test `IntegrationTestConfig`, ③ `app` test `IntegrationTestConfig`. 서로 다른 테스트 컨텍스트가 각기 다른 조합을 import한다. `architecture.md` 표는 ②만 열거했고, 1차 작업은 ①②를 채우고 ③을 빠뜨렸다. **문서가 SSOT로 신뢰됐으나 그 SSOT가 불완전했다.**
- **단일 빈 누락의 폭발 반경.** `@SpringBootTest` 전체 컨텍스트는 누락 빈 하나로 로딩 자체가 실패하고, 그 컨텍스트를 공유하는 모든 IT가 연쇄로 죽는다. 그래서 무관한 클래스 55건이 동시에 빨갛게 보였다(원인 분산 착시).
- **진단 신호의 빈약함.** GitHub CI annotation과 Gradle 콘솔은 예외 **타입**만 보여주고(`NoSuchBeanDefinitionException`) 빈 이름을 자르므로, 어느 빈인지는 `build/test-results/**/*.xml`까지 내려가야 나왔다.

## 대응

- **원인 격리.** 광범위·무관해 보이는 실패 패턴을 "여러 버그"가 아니라 "단일 컨텍스트 로딩 실패"로 가설을 세우고, XML에서 정확한 빈 이름·Qualifier를 추출해 확정했다.
- **수정.** `app/.../support/app/config/IntegrationTestConfig`에 기존 `blindTimerGameScheduler` 패턴과 동일하게 `nunchiGameScheduler`(`new ShutDownTestScheduler()`) 추가.
- **검증.** `*SecurityConfigTest` 단독 → 컨텍스트 정상 로드 확인 후 `:app:test` 전체 회귀 통과.
- **문서 교정.** `architecture.md` 미러링 표를 스케줄러 미러 **3곳 전부**를 열거하도록 수정하고 본 회고로 역링크.

## 재발 방지 액션

| 액션 | 상태 |
|------|------|
| `architecture.md` 미러링 표에 스케줄러 미러 3곳(①②③)을 모두 명시 | ☑ 완료 |
| 새 게임 PR 리뷰 시 `@Profile("!test")` 스케줄러 빈이 추가되면 테스트 미러 3곳이 같은 diff에 있는지 확인 | ☐ 리뷰 체크포인트 |
| 한 모듈 IT만 무더기로 깨지면 "여러 버그"보다 "단일 컨텍스트 로딩 실패"를 먼저 의심하고 XML에서 빈 이름 확인 | ☐ 진단 관행 |
| (검토) 체크리스트가 위치를 열거하는 대신, 새 스케줄러 빈에 대한 테스트 미러 누락을 ArchUnit/컨텍스트 스모크 테스트로 강제 | ☐ 아이디어 |

## 교훈

- **체크리스트는 그 자체가 SSOT가 되며, 불완전한 체크리스트는 "다 했다"는 착각을 만들어 재발을 막지 못한다.** 사고를 겪고 문서를 쓰는 것만으로 끝나지 않는다 — 문서가 모든 경우를 열거하는지 검증해야 한다. 1차 때 3곳 중 1곳만 적은 표가 정확히 빠진 그 한 곳을 다시 빠뜨리게 했다.
- **무관해 보이는 광범위 실패는 종종 단일 공유 자원의 단일 실패다.** 컨텍스트 로딩 실패는 폭발 반경이 크다 — 실패 개수가 아니라 공유 컨텍스트를 먼저 본다.
- **CI annotation/콘솔은 1차 신호일 뿐, 빈 이름 같은 상세는 `build/test-results/**/*.xml`에 있다.** 타입만 보고 추측하지 말고 XML까지 내려간다.
- **열거형 체크리스트보다 자동 강제가 낫다.** 위치를 나열하는 문서는 위치가 늘면 다시 불완전해진다 — 가능하면 누락을 테스트로 깨지게 만든다.
