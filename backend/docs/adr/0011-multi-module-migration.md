# 0011. Gradle 멀티 모듈 전환

- 날짜: 2026-05-11
- 상태: 초안 (미결 사항 해소 2026-05-11)

## 컨텍스트

현재 프로젝트는 단일 Gradle 모듈(`:backend`)에 20개의 최상위 패키지가 공존한다.

```text
coffeeshout/
├── global/       # 횡단 관심사
├── websocket/    # STOMP 인프라
├── common/       # 닉네임 유틸 (3개 클래스)
├── user/ auth/ friend/
├── room/
├── gamecommon/ minigame/
├── blockstacking/ cardgame/ laddergame/ racinggame/ speedtouch/ blindtimer/
├── dashboard/ patchnote/ report/
└── zzolbot/
```

패키지 경계는 잘 설계되어 있지만 Gradle 수준에서는 전체가 단일 컴파일 단위다. 이로 인해 세 가지 문제가 있다.

1. **증분 빌드 불가**: 게임 하나를 수정해도 전체가 재컴파일된다.
2. **의존 위반 묵인**: 패키지 간 무단 참조가 컴파일 오류가 아닌 코드 리뷰 단계에서만 발견된다.
3. **독립 배포 불가**: zzolbot이나 admin 기능을 별도 인스턴스로 운영하려면 구조 변경이 필요하다.

`be/refactor/1250-global` 브랜치에서 진행 중인 패키지 정리(global 분리, zzolbot·websocket 최상위 격상)는 이 전환을 위한 사전 작업이다.

## 결정

### 1. 분리 기준 — 도메인 수직 분리

두 가지 방향을 검토했다.

**수평 분리 (계층별: api / domain / infra)**

각 계층을 독립 모듈로 두는 구조다. Clean Architecture·Hexagonal Architecture와 자연스럽게 매핑되며, 인프라 교체 시 `domain` 모듈을 건드리지 않아도 된다는 장점이 있다.

그러나 이 프로젝트에는 적합하지 않다.

- `domain` 모듈이 user·room·game 모든 도메인을 포함하므로 게임 하나를 수정해도 `domain` 전체가 재빌드된다. 증분 빌드 효과가 없다.
- zzolbot·admin 독립 배포를 목표로 한다면 `domain` 모듈이 공유되는 수평 구조에서는 분리가 불가능하다.
- 인프라 교체 빈도가 낮고, 여러 애플리케이션이 같은 도메인 모듈을 공유하는 모노레포 구조도 아니다.

수평 분리가 유리한 시나리오는 "인프라 교체가 잦고 여러 앱(api-server, batch-worker 등)이 동일 도메인 로직을 공유"할 때다. 이 프로젝트는 해당하지 않는다.

**수직 분리 (도메인별: :user / :room / :game …)**

도메인 경계를 모듈 경계와 일치시키는 구조다. 이 프로젝트에 적합한 이유는 세 가지다.

1. **빌드 격리**: 게임 6종은 가장 자주 바뀌는 코드다. `:game` 분리 후 게임 수정 시 `:game` + `:app`만 재빌드된다.
2. **독립 배포**: `./gradlew :zzolbot:bootJar` 한 줄로 zzolbot 단독 패키징이 가능해진다.
3. **의존 위반 조기 발견**: 모듈 간 무단 참조를 컴파일 시점에 차단한다.

수직 분리는 모듈 내부 계층 위반(예: `room.infra`가 `room.domain`을 역참조)을 Gradle이 잡아주지 못한다. 이 빈틈은 §10의 ArchUnit으로 보완한다.

도메인 단위 수직 분리를 채택한다.

### 2. 모듈 맵

```text
root
├── :global     # exception, config, health, filter, redis, outbox
│               # lock, trace, ratelimit, metric
│               # + common/ (닉네임 유틸) 흡수
├── :websocket  # STOMP 인터셉터, 세션 추적, 메트릭
│               # (room 관련 이벤트 핸들러는 :room으로 이동 — §8 참조)
├── :user       # user/, auth/, friend/
├── :room       # room/ + minigame/
│               # + websocket room 핸들러 (SessionConnectEventListener 등)
├── :game       # blockstacking/, cardgame/, laddergame/,
│               # racinggame/, speedtouch/, blindtimer/
│               # + gamecommon/ (flow, metric — §5 참조)
├── :admin      # dashboard/, patchnote/, report/
├── :zzolbot    # zzolbot/
└── :app        # CoffeeShoutApplication — 진입점, 모든 모듈 조합
```

의존 방향:

```text
:global
  ↑
:websocket ← :user
  ↑              ↑
:room ──────────┘
  ↑
:game
:admin  → :global, :websocket, :room, :user
:zzolbot→ :global, :room
:app    → all
```

### 3. `:social`을 `:user`에 통합하는 이유

`:social` 모듈을 별도로 두면 향후 소셜 피드·활동 알림이 방(room) 이벤트를 참조하게 될 때 `:social` → `:room` 의존이 추가된다. 현재 의존 그래프에 이 경로가 없으므로 새 화살표가 생기고 그래프가 복잡해진다.

`friend/` 하나를 위해 모듈을 두는 비용(빌드 설정, 의존 선언, 리뷰 경계) 대비 얻는 격리 효과가 없다. 소셜 기능 로드맵이 구체화될 때 `:user`에서 분리하면 충분하다.

### 4. `:global`을 분리하지 않는 이유

`:global`을 `:common`(순수 유틸)과 `:infra`(Redis/Outbox/Lock 구현체)로 나누는 안을 검토했으나 채택하지 않는다.

- `:common`만 의존하고 `:infra`는 의존하지 않는 모듈이 실질적으로 없다. 모든 도메인 모듈이 어차피 둘 다 필요해서 빌드 캐시 효과가 없다.
- 순수 도메인 단위 테스트는 Spring Context 없이 모킹으로 처리하므로 모듈 경계로 강제할 필요가 없다.
- 모듈 수와 `build.gradle.kts` 파일만 늘어난다.

### 5. `gamecommon/`을 `:game`에 포함하는 이유

초안에서는 `gamecommon/`을 `:room`에 두기로 했으나, Phase 1 실행 후 코드 탐색 결과 이 판단의 근거가 잘못됐음이 확인됐다.

**초안 근거 (잘못됨)**: "`gamecommon.flow/`가 `room.domain.JoinCode` 등 room 타입을 참조한다" — 실제로는 `gamecommon/` 전체가 Spring/Micrometer/JDK 외에 아무 도메인 타입도 참조하지 않는다. room 타입 참조는 `minigame/`에만 해당하는 설명이었다.

**실제 의존 관계**: `gamecommon/`을 참조하는 쪽은 `:game` 6개 서비스 전부이며, `:room` 내부에서는 아무도 참조하지 않는다. `gamecommon/`을 `:room`에 두면 `:game` → `:room` 의존이 생기고, 이는 게임 플로우 인프라를 위해 room 모듈 전체를 재빌드해야 하는 불필요한 결합이 된다.

`gamecommon/`은 `:game`으로 이동한다. `minigame/`은 `room.domain.JoinCode` 참조가 실재하므로 `:room`에 계속 유지한다.

### 6. `:game` 분리 효과

게임 6종은 프로젝트에서 가장 자주 변경되는 코드다. 기존 구조에서는 게임 하나를 수정해도 `room` 포함 전체가 재빌드된다. `:game` 분리 후에는 `:game` + `:app`만 재빌드된다.

### 7. 순환 의존 처리 원칙

멀티 모듈 전환 시 순환 의존이 발견되면 다음 순서로 처리한다.

1. 공통 타입을 더 낮은 모듈(`:global` → `:room` 순)로 이동
2. 이동이 어려우면 인터페이스를 하위 모듈에 정의하고 구현체를 상위 모듈에 두는 포트 패턴 적용
3. 형제 모듈 간 순환은 공통 타입을 공통 조상 모듈로 추출

### 8. `:websocket` ↔ `:room` 순환 의존 해소

멀티 모듈 전환 전 코드 탐색 결과 `:websocket` ↔ `:room` 간 양방향 참조가 확인되었다.

**`:websocket` → `:room` 참조 (6개 파일)**:

| 파일                                               | 참조 타입                                          |
|--------------------------------------------------|------------------------------------------------|
| `SessionConnectEventListener`                    | `JoinCode`, `Room`, `RoomQueryService`         |
| `PlayerDisconnectionService`                     | `JoinCode`, `PlayerName`, `RoomCommandService` |
| `DelayedPlayerRemovalService`                    | `RoomService`                                  |
| `RoomStateUpdateEventListener`                   | `RoomService`, `PlayerListUpdateEvent`         |
| `GameRecoveryService` / `GameRecoveryController` | `JoinCode`                                     |
| `LoggingSimpMessagingTemplate`                   | `JoinCode`, `GameRecoveryService`              |

**조치 분류**:

| 파일                                               | 조치                                             | 이유                                                                                                                                                                                                             |
|--------------------------------------------------|------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SessionConnectEventListener`                    | `:room.infra`로 이동                              | room 상태(입장 처리)를 직접 변경                                                                                                                                                                                          |
| `PlayerDisconnectionService`                     | `:room.infra`로 이동                              | room 상태(퇴장 처리)를 직접 변경                                                                                                                                                                                          |
| `DelayedPlayerRemovalService`                    | `:room.infra`로 이동                              | room 상태(지연 퇴장)를 직접 변경. 현재 `websocket/` 패키지에 위치하나 `RoomService` 의존이 있어 모듈 분리 전 반드시 이동 필요                                                                                                                        |
| `RoomStateUpdateEventListener`                   | `:room.infra`로 이동                              | room 이벤트에 반응해 상태 갱신                                                                                                                                                                                            |
| `GameRecoveryService` / `GameRecoveryController` | `:room`으로 이동                                   | URL `/api/rooms/{joinCode}/recovery`, `DelayedRoomRemovalService`가 cleanup 호출, 복구 단위가 방                                                                                                                        |
| `LoggingSimpMessagingTemplate`                   | **`:websocket` 유지** — `JoinCode` → `String` 교체 | `:user`의 `FriendNotifier`·`PresenceNotifier`, `:game`의 각 Notifier 등 다수 모듈이 주입한다. `:room`으로 이동하면 ADR §2에 없는 `:user → :room` 의존이 발생해 그래프와 모순된다. destination 패턴에서 추출한 값을 `String joinCode`로 다루면 room 타입 참조가 사라진다. |

이동 후 `:websocket`은 순수 STOMP 인프라(세션 추적, 메트릭, 핸드셰이크, 메시지 브로커 래핑)만 담당하고, 의존 방향은 `:room` → `:websocket` 단방향으로 정리된다.

이 이동은 Phase 1(파일 이동) 단계에서 함께 수행한다.

### 9. 테스트 배치

각 모듈의 `src/test/`는 해당 모듈 내에 위치한다. 도메인 픽스처(`RoomFixture` 등)는 해당 모듈 자신의 `testFixtures` 소스셋에 둔다.

TestContainers 부트스트랩·설정처럼 여러 모듈이 공유하는 테스트 인프라는 **`:global` 모듈의 `testFixtures`** 소스셋에 둔다. 모든 모듈이 이미 `:global`을 의존하므로 `testImplementation(testFixtures(project(":global")))`으로 바로 참조 가능하다. `:app`에 두면 `:game` 등이 `:app`을 역참조해야 하는 순환이 생기므로 적합하지 않다.

### 10. ArchUnit으로 모듈 내 계층 의존성 강제

Gradle 멀티 모듈은 **모듈 간** 의존 위반을 컴파일 타임에 차단하지만, **모듈 내부** 계층 위반은 잡지 못한다.

예를 들어 `:room` 모듈에서 `room.infra`가 `room.domain`을 직접 참조하는 건 Gradle 관점에서 동일 모듈 내부이므로 컴파일 오류가 나지 않는다. 이 빈틈을 ArchUnit 테스트로 메운다.

**배치 위치**: 각 모듈의 `src/test/` 내에 아키텍처 규칙 테스트를 위치시킨다. 공통 규칙 헬퍼는 `:global`의 `testFixtures`에 둔다.

**적용 규칙 예시**

```java
// 계층 의존 방향: domain ← application ← infra (역방향 금지)
layeredArchitecture()
    .consideringAllDependencies()
    .layer("Domain").definedBy("..domain..")
    .layer("Application").definedBy("..application..")
    .layer("Infra").definedBy("..infra..")
    .whereLayer("Domain").mayNotAccessLayersExcept()
    .whereLayer("Application").mayOnlyAccessLayers("Domain")
    .whereLayer("Infra").mayOnlyAccessLayers("Application", "Domain");

// 모듈 간 규칙: :game은 :user를 직접 참조할 수 없다
noClasses().that().resideInPackage("..game..")
    .should().dependOnClassesThat().resideInPackage("..user..")
    .check(importedClasses);

// 네이밍 컨벤션
classes().that().areAnnotatedWith(Service.class)
    .should().haveSimpleNameEndingWith("Service");
```

**Gradle 멀티 모듈과의 역할 분담**

| 위반 유형                               | Gradle | ArchUnit |
|-------------------------------------|--------|----------|
| `:game`이 `:user`를 import            | 컴파일 오류 | —        |
| `game.infra`가 `game.domain`을 역참조    | 통과     | 테스트 실패   |
| `Service`가 `Repository` 구현체를 직접 new | 통과     | 테스트 실패   |

ArchUnit 테스트는 CI에서 단위 테스트와 함께 실행된다. 피드백 시점이 컴파일러보다 늦지만, Gradle 멀티 모듈만으로는 강제할 수 없는 모듈 내 계층 규칙을 자동으로 검증한다.

Phase 1(모듈 골격 구성) 완료 후 Phase 2 의존성 정제 단계에서 함께 도입한다.

## 작업 계획

### Phase 0 — 패키지 정리 (완료, `be/refactor/1250-global`)

Gradle 모듈 경계와 패키지 경계를 일치시키는 사전 작업이다.

- [x] `global/` 내 횡단 관심사만 남기기 (websocket, zzolbot 등 격상)
- [x] `LogAspect` → `room.aspect` 이동
- [x] `global.zzolbot` → `zzolbot` 최상위 격상
- [x] `docs/architecture.md` 패키지 구조 표 갱신
- [x] `common/` → `global/` 흡수 (NameValidator 등 3개 클래스)

### Phase 1 — Gradle 멀티 모듈 골격 구성

목표: 컴파일은 그대로 통과하면서 Gradle 모듈 경계만 선언한다.

1. `settings.gradle.kts` 생성, 8개 모듈 등록
2. 루트 `build.gradle.kts` → `subprojects {}` 블록으로 공통 설정 추출
3. 각 모듈 디렉토리 + `build.gradle.kts` 생성 — Phase 2 표의 모듈 의존성을 처음부터 선언 (루트 의존성 전체 임시 복사 금지: Phase 3 부채를 키움)
4. 소스 파일을 **의존 방향 역순(리프 먼저)**으로 모듈별 순차 이동 — 각 단계마다 해당 모듈 컴파일 통과 확인
   - a. `:global` 이동 → `./gradlew :global:compileJava`
   - b. `:websocket` 이동 → `./gradlew :websocket:compileJava`
   - c. `:user` 이동 → `./gradlew :user:compileJava`
   - d. `:room` 이동 (§8 websocket room 핸들러 포함) → `./gradlew :room:compileJava`
   - e. `:game` 이동 → `./gradlew :game:compileJava`
   - f. `:admin`, `:zzolbot` 이동 → `./gradlew :admin:compileJava :zzolbot:compileJava`
   - g. `:app` 이동 → `./gradlew :app:compileJava`
5. `./gradlew build` 전체 통과 확인

> 일괄 이동 대신 모듈별 순차 이동을 택하는 이유: 한 번에 전체를 이동하면 컴파일 오류가 여러 모듈에 동시에 산발하여 원인 추적이 어렵다. 리프 모듈부터 순서대로 이동하면 각 단계의 오류 범위가 해당 모듈로 한정된다.

### Phase 2 — 의존성 선언 정제 + ArchUnit 도입

목표: 각 모듈이 실제로 필요한 의존성만 선언하고, 모듈 내 계층 규칙을 자동 검증한다.

| 모듈           | 의존 모듈                                     | 주요 외부 의존성                               |
|--------------|-------------------------------------------|-----------------------------------------|
| `:global`    | 없음                                        | Spring Core, Redis, JPA, Redisson, OTel |
| `:websocket` | `:global`                                 | Spring WebSocket, STOMP                 |
| `:user`      | `:global`, `:websocket`                   | Spring Security, OAuth2, JWT            |
| `:room`      | `:global`, `:websocket`, `:user`          | Spring Web                              |
| `:game`      | `:global`, `:websocket`, `:room`          |                                         |
| `:admin`     | `:global`, `:websocket`, `:room`, `:user` | Thymeleaf, Spring Security              |
| `:zzolbot`   | `:global`, `:room`                        | Gemini AI, JSqlParser, Resilience4j     |
| `:app`       | 모든 모듈                                     | Spring Boot 플러그인, Flyway, DB 드라이버       |

의존성 정제 완료 후 ArchUnit 도입 (§10):

1. `:global` `build.gradle.kts`에 `testFixtures` 소스셋 활성화 + `archunit` 의존성 추가
2. `:global` `testFixtures`에 계층 의존 방향 공통 규칙 헬퍼 작성
3. 각 모듈 `src/test/`에 `ArchitectureTest` 추가 — `domain ← application ← infra` 방향 강제
4. `./gradlew test --tests "*ArchitectureTest"` 전체 통과 확인

### Phase 3 — 모듈 간 의존 위반 해소

Phase 1 이후 Gradle이 잡아내는 컴파일 오류를 제거한다. 예상 발생 지점:

- **게임 모듈 간 직접 참조**: 게임 A가 게임 B의 타입을 직접 import — 공통 타입은 `:room`(`gamecommon`)으로 이동
- **`:global` 역참조**: `global` 패키지가 도메인 타입을 사용하는 경우 — 도메인 타입 제거 또는 제네릭·인터페이스로 추상화
- **`:zzolbot` → `:user` 직접 참조**: ADR §2 의존 그래프에 없는 경로 — `:room`을 경유하거나 필요 타입을 `:global`로 이동
- **`:websocket` ↔ `:room` 순환**: Phase 1 파일 이동으로 사전 해소 (§8)

### Phase 4 — 빌드 검증 및 캐시 확인

```bash
# ArchUnit 규칙 전체 실행
./gradlew test --tests "*ArchitectureTest"

# 모듈 단위 테스트 실행
./gradlew :zzolbot:test
./gradlew :game:test

# 캐시 히트 확인 — :game만 변경 시 :room이 재빌드되지 않아야 함
./gradlew :game:compileJava
./gradlew :room:compileJava  # UP-TO-DATE 여야 함

# 독립 패키징 확인
./gradlew :zzolbot:bootJar
```

## 결과

- **증분 빌드**: 변경된 모듈과 이를 의존하는 모듈만 재빌드. 게임 수정 시 `:game` + `:app`만 재빌드.
- **의존 위반 조기 발견**: 컴파일 시점에 무단 참조 차단
- **독립 배포 가능**: `./gradlew :zzolbot:bootJar` (향후 별도 배포 시)
- **팀 소유권 명확화**: 모듈 단위로 PR 리뷰 범위 구분 가능

## 미결 사항 해소 (2026-05-11)

기존 미결 사항 두 항목을 코드 탐색 후 결정했다.

**`:social` 범위** → `friend/`를 `:user`에 통합, `:social` 모듈 제거. 이유는 §3 참조.

**`:websocket` ↔ `:room` 순환 의존** → 코드 탐색으로 양방향 참조 실재 확인. 방향 A(room 관련 핸들러를 `:room`으로 이동) 채택. 세부 내용은 §8 참조.

## Phase 1 실행 기록

→ [0011-phase1-log.md](0011-phase1-log.md) — 계획 대비 실제 이동 차이, 파일 이동 외 코드 변경, build.gradle.kts 변경, Phase 1 후속 작업 상세 기록

### 계획 → 실제 이동 차이 (요약)

| 파일 | 계획 모듈 | 실제 모듈 |
|---|---|---|
| `global/exception/WebSocketExceptionHandler.java` | `:global` | `:websocket` |
| `global/config/WebMvcConfig.java` | `:global` | `:user` |
| `global/config/SwaggerConfig.java` | `:global` | `:global` (SpringDoc 의존성 이동) |
| `global/health/GracefulShutdownHealthIndicator.java` | `:global` | `:global` (포트 추출로 유지) |
| `websocket/interceptor/StompPrincipalInterceptor.java` | `:websocket` | `:user` |
| `websocket/config/WebSocketMessageBrokerConfig.java` | `:websocket` | `:app` |
| `user/application/service/UserNicknameCleanupService.java` | `:user` | `:room` |
| `friend/application/service/RoomInvitationService.java` | `:user` | `:room` |
| `friend/ui/RoomInvitationRestController.java` | `:user` | `:room` |
| `user/application/service/UserWithdrawalService.java` | `:user` | `:admin` |
| `user/ui/UserRestController.java` | `:user` | `:admin` |
| `minigame/ui/command/handler/SelectCardCommandHandler.java` | `:room` | `:game` |
| `minigame/ui/response/MiniGameStateMessage.java` | `:room` | `:game` |
| `room/application/service/player/name/PlayerNameRankingCleanupService.java` | `:room` | `:room` (포트 추출로 유지) |
| `cardgame/domain/event/dto/MiniGameStartedEvent.java` | `:game` | `:room` |
| `room/infra/GeminiPlayerNameAuditor.java` | `:room` | `:room` (`:zzolbot` 경유 후 복귀) |

### Phase 1 최종 상태 (2026-05-20)

`./gradlew assemble` + `./gradlew test` 전체 통과. 8개 모듈 모두 컴파일·테스트 성공.

```text
:global ✅  :websocket ✅  :user ✅  :room ✅
:game ✅    :admin ✅      :zzolbot ✅  :app ✅
```
