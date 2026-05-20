# ADR-0011 Phase 1 실행 기록

← 의사결정 문서: [0011-multi-module-migration.md](0011-multi-module-migration.md)

## Phase 1 실행 기록 (2026-05-19)

실제 컴파일 오류를 통해 드러난 의존 위반으로 인해 사전 계획과 다른 위치에 배치된 파일 목록.
Phase 1에서는 파일 이동 시 패키지 이름을 변경하지 않았으나, 이후 후속 작업에서 물리 위치와 패키지명 불일치를 별도로 정리했다 (각 항목의 이유 열 참조).

### 계획 → 실제 이동 차이

**`:global` → `:websocket`** (websocket 타입 참조로 인한 이동)

| 원래 파일 경로 | 계획 모듈 | 실제 모듈 | 이유 |
|---|---|---|---|
| `global/exception/WebSocketExceptionHandler.java` | `:global` | `:websocket` | `coffeeshout.websocket.LoggingSimpMessagingTemplate` 참조. 패키지도 `coffeeshout.websocket.exception`으로 변경 |

**`:global` 유지 — 포트 추출 또는 의존성 이동으로 역전** (Phase 1 이후 구조 개선)

| 원래 파일 경로 | Phase 1 위치 | 최종 위치 | 해소 방법 |
|---|---|---|---|
| `global/config/SwaggerConfig.java` | `:websocket` | `:global` | SpringDoc 의존성을 `:global`로 이동. `api` 선언으로 전이되어 하위 모듈 별도 선언 불필요 |
| `global/health/GracefulShutdownHealthIndicator.java` | `:websocket` | `:global` | `ShutdownStateReader` 포트를 `:global.health`에 신규 선언. `WebSocketGracefulShutdownHandler`가 구현. Health Indicator는 포트만 의존 |

**`:global` → `:user`** (user 타입 참조로 인한 이동)

| 원래 파일 경로 | 계획 모듈 | 실제 모듈 | 이유 |
|---|---|---|---|
| `global/config/WebMvcConfig.java` | `:global` | `:user` | `AuthenticatedUserArgumentResolver` 참조. 패키지도 `coffeeshout.user.config`으로 변경. CORS 설정은 `CorsConfig`로 분리해 `:global`에 신규 생성 — `cors.allowed-origins` 프로퍼티로 환경별 허용 오리진 설정 |

**`:websocket` → `:user`** (user 타입 참조로 인한 이동)

| 원래 파일 경로 | 계획 모듈 | 실제 모듈 | 이유 |
|---|---|---|---|
| `websocket/interceptor/StompPrincipalInterceptor.java` | `:websocket` | `:user` | `coffeeshout.user.application.service.AuthTokenService`, `coffeeshout.user.domain.AuthenticatedUser` 참조 |
| `websocket/config/WebSocketMessageBrokerConfig.java` | `:websocket` | `:user` | `StompPrincipalInterceptor`가 `:user`로 이동했으므로 함께 이동 |

**`:user` → `:room`** (room 타입 참조로 인한 이동)

| 원래 파일 경로 | 계획 모듈 | 실제 모듈 | 이유 |
|---|---|---|---|
| `user/application/service/UserNicknameCleanupService.java` | `:user` | `:room` | `ProfanityWordBlockedEvent` 참조. 패키지도 `coffeeshout.room.application.user`로 변경 — room 이벤트에 반응해 user 데이터를 정리하는 room 오케스트레이션 서비스 |
| `friend/application/service/RoomInvitationService.java` | `:user` | `:room` | `coffeeshout.room.domain.*`, `coffeeshout.room.domain.repository.*` 참조 |
| `friend/ui/RoomInvitationRestController.java` | `:user` | `:room` | `RoomInvitationService`가 `:room`으로 이동했으므로 함께 이동 |

**`:user` → `:admin`** (report/admin 타입 참조로 인한 이동)

| 원래 파일 경로 | 계획 모듈 | 실제 모듈 | 이유 |
|---|---|---|---|
| `user/application/service/UserWithdrawalService.java` | `:user` | `:admin` | `coffeeshout.report.domain.ReportAnonymizationRepository` 참조 |
| `user/ui/UserRestController.java` | `:user` | `:admin` | `UserWithdrawalService`가 `:admin`으로 이동했으므로 함께 이동 |

**`:room(minigame)` → `:game`** (game 타입 참조로 인한 이동)

| 원래 파일 경로 | 계획 모듈 | 실제 모듈 | 이유 |
|---|---|---|---|
| `minigame/ui/command/handler/SelectCardCommandHandler.java` | `:room` | `:game` | `coffeeshout.cardgame.domain.event.*`, `coffeeshout.cardgame.infra.*` 참조 |
| `minigame/ui/response/MiniGameStateMessage.java` | `:room` | `:game` | `coffeeshout.cardgame.domain.*` 참조 |

**`:room` 유지 — 포트 추출로 `:admin` 이동 없음** (dashboard 타입 역참조 → 포트 역전)

초기에는 `PlayerNameRankingCleanupService`가 `coffeeshout.dashboard.domain.*`을 직접 참조해 `:admin` 이동이 불가피해 보였다.
그러나 `:admin`으로 이동하면 서비스·스케줄러가 room 도메인 파일임에도 admin 모듈에 묶이는 문제가 발생한다.
대신 ADR §7의 포트 패턴(인터페이스를 하위 모듈에 선언, 구현체를 상위 모듈에 배치)을 적용해 두 파일을 `:room`에 유지했다.

| 원래 파일 경로 | 계획 모듈 | 실제 모듈 | 해소 방법 |
|---|---|---|---|
| `room/application/service/player/name/PlayerNameRankingCleanupService.java` | `:room` | `:room` | `RankedNicknameReader` 포트를 `:room.domain`에 신규 선언, `DashboardRankedNicknameReader` 구현체를 `:admin`에 배치 |
| `room/infra/PlayerNameRankingCleanupScheduler.java` | `:room` | `:room` | 위 서비스가 `:room`에 유지되었으므로 스케줄러도 함께 유지 |

**`:game` → `:room`** (모듈 의존 방향 교정)

| 원래 파일 경로 | 계획 모듈 | 실제 모듈 | 이유 |
|---|---|---|---|
| `cardgame/domain/event/dto/MiniGameStartedEvent.java` (`:game`) | `:game` | `:room` | `:room`의 `MiniGameEventService`가 발행하고 `:game`의 `CardGameNotifier`가 수신하는 공통 이벤트. 발행 측이 있는 `:room`으로 이동하고 패키지를 `coffeeshout.minigame.event`로 변경 |

**`:room` → `:zzolbot` (Phase 1) → `:room` 복귀 (구조 개선)** (Gemini AI 참조로 인한 이동 후 복귀)

`GeminiPlayerNameAuditor`와 `GeminiAuditConfig`는 Phase 1 당시 Google GenAI SDK 의존성이 `:zzolbot`에만 선언되어 있어 `:zzolbot`으로 임시 이동했다.
그러나 두 파일은 `coffeeshout.room.domain.*` 타입을 참조하는 room 도메인 인프라 파일이며, `:zzolbot`에 위치하는 것은 의존 방향 위반이다.
`room/build.gradle.kts`에 `google-genai` 의존성을 추가한 뒤 `:room.infra`로 복귀했다.

| 파일 | Phase 1 위치 | 최종 위치 | 이유 |
|---|---|---|---|
| `room/infra/GeminiPlayerNameAuditor.java` | `:zzolbot` | `:room` | room 도메인 타입 참조 — `:zzolbot` 배치는 의존 방향 위반 |
| `room/config/GeminiAuditConfig.java` | `:zzolbot` | `:room` | `GeminiPlayerNameAuditor`와 같은 이유로 함께 복귀 |

### 파일 이동 외 코드 변경

`room/config/RoomConfig.java` — Gemini `Client` bean을 제거하고 `:zzolbot` 모듈의 새 파일로 분리.

`minigame/domain/MiniGameType.java` — `createMiniGame()` 팩토리 메서드 제거. 6개 게임 도메인 import 전체 제거. 순수 enum 상수만 유지.

`minigame/domain/MiniGameResult.java` — `calculateRank()` 내 sentinel 초기값을 `CardGameScore.INF`(`:game` 타입)에서 `null`로 교체. `MiniGameScore.equals(null)`이 `false`를 반환하므로 동작 동일.

`room/domain/service/RoomCommandService.java` — `PlayableFactory` 필드 추가, `miniGameType.createMiniGame()` 직접 호출을 `playableFactory.create()`로 교체.

신규 파일:

- `room/src/main/java/coffeeshout/room/config/GeminiAuditConfig.java` — `RoomConfig`에서 분리한 Gemini `Client` bean (`@Profile("!local & !test")`). 초기에 `:zzolbot`에 잘못 배치되었다가 `:room`으로 복귀
- `websocket/src/main/java/coffeeshout/websocket/RecoveryMessageStore.java` — `GameRecoveryService`와 `LoggingSimpMessagingTemplate` 순환 의존 해소용 포트 인터페이스 (`save()` 메서드만 선언)
- `room/src/main/java/coffeeshout/minigame/domain/PlayableFactory.java` — `MiniGameType`에서 분리한 게임 인스턴스 생성 인터페이스. `create(MiniGameType, String joinCode) : Playable` 메서드 선언. `:room`이 `:game` 구현체를 직접 참조하지 않도록 포트 역할
- `game/src/main/java/coffeeshout/minigame/PlayableFactoryImpl.java` — `PlayableFactory` 구현체. 기존 `MiniGameType.createMiniGame()` 로직을 그대로 이전. `@Component`로 등록
- `room/src/main/java/coffeeshout/room/domain/player/RankedNicknameReader.java` — `PlayerNameRankingCleanupService`의 `:admin` 역참조 해소용 포트 인터페이스. `findRankedNicknames(LocalDateTime, LocalDateTime, int) : Set<String>` 선언
- `admin/src/main/java/coffeeshout/dashboard/infra/DashboardRankedNicknameReader.java` — `RankedNicknameReader` 구현체. `DashboardStatisticsRepository`에서 상위 닉네임을 조회해 반환
- `global/src/main/java/coffeeshout/global/health/ShutdownStateReader.java` — `GracefulShutdownHealthIndicator`의 `:websocket` 역참조 해소용 포트 인터페이스. `isShuttingDown() : boolean` 선언. `WebSocketGracefulShutdownHandler`가 구현
- `global/src/main/java/coffeeshout/global/config/CorsConfig.java` — `WebMvcConfig`에서 분리한 CORS 설정. `cors.allowed-origins` 프로퍼티로 환경별 허용 오리진 설정 (local: `localhost:3000`, dev: `dev.zzol.site`, prod: `www.zzol.site`)

`GameRecoveryService.java` — `implements RecoveryMessageStore` 추가.

`LoggingSimpMessagingTemplate.java` — `GameRecoveryService` 필드 타입을 `RecoveryMessageStore`로 교체.

### build.gradle.kts 변경 (계획에 없던 의존성)

| 모듈 | 추가 의존성 | 이유 |
|---|---|---|
| `:global` | `resilience4j-spring-boot3` (추가), `jspecify` (추가), ZXing·resilience4j `api` 승격, SpringDoc `api` 추가 | `IpBlockStore`(resilience4j), `IpBlockFilter`(jspecify), 하위 모듈 컴파일 전파. `SwaggerConfig` `:global` 복귀로 SpringDoc도 `:global`에서 선언 |
| `:websocket` | `spring-boot-starter-security`, `jjwt-api/impl/jackson` — SpringDoc 선언 제거 | SpringDoc은 `:global` `api`로 전이됨. `WsCatalogSecurityConfig`, `websocket/auth/JjwtRoomSessionTokenIssuer` |
| `:room` | OCI SDK, `badwordfiltering`, SpringDoc | `OracleObjectStorageService`, `VaneProfanityChecker`, `RoomApi/GameRecoveryApi` |
| `:admin` | SpringDoc (`springdoc-openapi-starter-webmvc-ui`) | `DashboardApi`, `PatchNoteApi`, `ReportApi` 등 Swagger 어노테이션 사용 |
| `:app` | OCI SDK, Google GenAI, TestContainers, `micrometer-tracing-test`, WireMock, Spring Security, `badwordfiltering` (모두 `testImplementation`) | 모든 테스트 소스가 `:app`으로 이동됨. 각 원 모듈의 `implementation` 의존성은 컴파일 시점에 전이되지 않으므로 `:app` 테스트 컴파일에 필요한 의존성을 명시적으로 선언 |
| root `build.gradle.kts` | QueryDSL APT(`querydsl-apt`, `jakarta.annotation-api`, `jakarta.persistence-api`)를 `subprojects {}` 공통 선언으로 이동 | 기존에 `:global`에만 선언되어 있어 `:admin`(`QReport`) 등 다른 모듈에서 Q 클래스 생성 불가 |

### Phase 1 컴파일 위반 해소 (2026-05-19)

이전 작업 중단 시점에 남겨진 4개 파일의 `:room → :game` 역참조를 다음과 같이 해소했다.

| 파일 | 문제 | 해소 방법 |
|---|---|---|
| `minigame/domain/MiniGameType.java` | `createMiniGame()`이 6개 게임 도메인 직접 인스턴스화 | `createMiniGame()` 제거, 순수 enum 상수만 유지. `PlayableFactory` 인터페이스를 `:room`에 신규 선언, 구현체 `PlayableFactoryImpl`을 `:game`에 `@Component`로 신규 생성. `RoomCommandService`가 `PlayableFactory`를 주입받아 사용하도록 변경 |
| `minigame/domain/MiniGameResult.java` | `CardGameScore.INF`(`:game`)을 sentinel 초기값으로 사용 | `prevScore = null`로 교체. `MiniGameScore.equals(null)`이 `false`를 반환하므로 동작 동일 |
| `minigame/application/MiniGameEventService.java` | `coffeeshout.cardgame.domain.event.dto.MiniGameStartedEvent` import | `MiniGameStartedEvent`를 cardgame 패키지에서 `coffeeshout.minigame.event` 패키지(`:room`)로 이동. 수신 측 `CardGameNotifier`(`:game`)의 import도 함께 수정 |
| `room/infra/PlayerNameRankingCleanupScheduler.java` | `PlayerNameRankingCleanupService`가 `dashboard.domain.*`을 직접 참조 | `RankedNicknameReader` 포트를 `:room.domain`에 신규 선언, `DashboardRankedNicknameReader` 구현체를 `:admin.infra`에 신규 생성. 서비스·스케줄러 모두 `:room`에 유지 |

### Phase 1 최종 상태 (2026-05-19)

`./gradlew assemble` 전체 통과. 8개 모듈 모두 컴파일 성공.

```text
:global ✅  :websocket ✅  :user ✅  :room ✅
:game ✅    :admin ✅      :zzolbot ✅  :app ✅
```

`./gradlew build` (테스트 포함) — Docker(TestContainers) 환경에서 별도 검증 필요.

## Phase 1 후속 — WebSocketMessageBrokerConfig :user → :app 이동 (2026-05-20)

`WebSocketMessageBrokerConfig`는 `:websocket`이 예정 모듈이었으나, `StompPrincipalInterceptor`가 `:user`로 이동하면서 함께 `:user`로 임시 배치되었다.
설정 클래스 자체는 user 도메인과 무관한 순수 WebSocket 인프라 와이어링이므로 `:user`에 두는 것은 의미상 오류다.

`:app`은 모든 모듈에 의존하므로 `:websocket`(나머지 인터셉터)과 `:user`(`StompPrincipalInterceptor`) 양쪽 빈을 모두 주입받을 수 있다.
모듈 간 와이어링 책임을 `:app`이 담당하는 자연스러운 위치로 이동했다.

| 파일 | 변경 전 모듈 | 변경 후 모듈 | 비고 |
|---|---|---|---|
| `websocket/config/WebSocketMessageBrokerConfig.java` | `:user` | `:app` | 패키지 `coffeeshout.websocket.config` 유지 |
| `websocket/interceptor/StompPrincipalInterceptor.java` | `:user` (`coffeeshout.websocket.interceptor`) | `:user` (모듈 동일) | 패키지 → `coffeeshout.user.websocket.interceptor`로 변경 |

`StompPrincipalInterceptor`는 `AuthTokenService` · `AuthenticatedUser` 참조로 인해 `:user`에 유지하되, 물리 위치와 패키지명 일치를 위해 패키지를 `coffeeshout.user.websocket.interceptor`로 변경했다.

## Phase 1 최종 상태 (2026-05-20)

후속 작업 두 건 적용 후 `./gradlew assemble` + `./gradlew test` 전체 통과. 8개 모듈 모두 컴파일·테스트 성공.

```text
:global ✅  :websocket ✅  :user ✅  :room ✅
:game ✅    :admin ✅      :zzolbot ✅  :app ✅
```

## Phase 1 후속 — room invitation 패키지 정합성 (2026-05-20)

Phase 1에서 `:user → :room`으로 이동한 `RoomInvitationService` · `RoomInvitationRestController`의 패키지명이 `coffeeshout.friend.*`로 남아있어 물리 위치와 불일치했다. 이를 `:room` 모듈 패키지 체계에 맞게 정리했다.

| 파일 | 변경 전 패키지 | 변경 후 패키지 | 비고 |
|---|---|---|---|
| `RoomInvitationService.java` | `coffeeshout.friend.application.service` (`:room`) | `coffeeshout.room.application.service` (`:room`) | 패키지 이동 |
| `RoomInvitationRestController.java` | `coffeeshout.friend.ui` (`:room`) | `coffeeshout.room.ui` (`:room`) | 패키지 이동 |
| `SendRoomInvitationRequest.java` | `coffeeshout.friend.ui.request` (`:user`) | `coffeeshout.room.ui.request` (`:room`) | 모듈 + 패키지 이동 |

`SendRoomInvitationRequest`는 `:room` 컨트롤러가 단독으로 사용하는 요청 DTO이므로 `:user`에 남아있을 이유가 없어 함께 이동했다.

## Phase 1 후속 — 게임 엔티티·서비스 소유권 `:game`으로 이전 (2026-05-20)

Phase 1에서 `MiniGameEntity` · `MiniGameResultEntity`와 관련 레포지토리, 오케스트레이션 서비스가 `:room`에 남아있었다.
`:admin` 대시보드가 이들 Q클래스를 `:room` 경유로 접근하는 구조였는데, 게임 결과 데이터는 게임 도메인이 소유하는 것이 타당하므로 `:game`으로 이전했다.

**이전 파일 목록 (`:room` → `:game`, 패키지명 `coffeeshout.minigame.*` 유지, import 변경 없음)**

| 파일 | 분류 |
|---|---|
| `minigame/infra/persistence/MiniGameEntity.java` | JPA 엔티티 |
| `minigame/infra/persistence/MiniGameResultEntity.java` | JPA 엔티티 |
| `minigame/infra/persistence/MiniGameJpaRepository.java` | 레포지토리 |
| `minigame/infra/persistence/MiniGameResultJpaRepository.java` | 레포지토리 |
| `minigame/infra/persistence/MiniGameResultBulkRepository.java` | 레포지토리 |
| `minigame/infra/persistence/MiniGameResultBulkRepositoryImpl.java` | 레포지토리 구현 |
| `minigame/application/MiniGameEventService.java` | 게임 시작 오케스트레이션 |
| `minigame/application/MiniGamePersistenceService.java` | 게임 엔티티 저장 |
| `minigame/event/MiniGameResultSaveEventListener.java` | 결과 저장 이벤트 리스너 |
| `minigame/infra/messaging/consumer/StartMiniGameCommandEventConsumer.java` | Redis 스트림 컨슈머 |

**`:room` 유지 파일 (이벤트 계약·스트림 키)**

`MinigameStreamKey`, `MiniGameStartedEvent`, `MiniGameFinishedEvent`, `StartMiniGameCommandEvent` 등 이벤트 레코드는 `:room`의 계약으로 유지한다.
`:game`이 `:room`에 의존하므로 이들 타입은 `:game`에서도 접근 가능하다.

**build.gradle.kts 변경**

| 모듈 | 변경 내용 | 이유 |
|---|---|---|
| `:admin` | `api(project(":game"))` 추가 | `QMiniGameEntity` · `QMiniGameResultEntity` Q클래스를 `:game` 경유로 접근 |

**결과 의존 계층**

```text
:admin → :game → :room → :global, :websocket, :user
```
