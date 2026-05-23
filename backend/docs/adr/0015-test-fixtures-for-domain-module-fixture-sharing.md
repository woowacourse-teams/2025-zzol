# ADR-0015: `java-test-fixtures`를 통한 도메인 모듈 fixture 공유 전략

## 상태

적용됨 (2026-05-24)

## 컨텍스트

ADR-0013(도메인 모듈 테스트 독립 실행)로 각 도메인 모듈이 자체 통합 테스트를 실행할 수 있게 됐다.
그러나 `PlayerFixture`, `RoomFixture`, `UserFixture` 같은 fixture 클래스들이 `:room`, `:game`, `:user`, `:app` 등 여러 모듈에 내용이 동일한 채로 복사되는 문제가 발생했다.

복사가 증가한 배경:

1. `:app:test` 중심 시대에는 모든 fixture가 `:app`에 있었다.
2. ADR-0013 적용으로 `:room:test`가 독립 실행되면서 필요한 fixture를 로컬에 복사했다.
3. `:game:test`도 동일한 이유로 복사했다.

이 상태를 방치하면 fixture 변경 시 n개 모듈을 동기화해야 하는 drift 문제가 생긴다.

## 결정

Gradle 내장 플러그인 `java-test-fixtures`를 각 도메인 모듈에 적용해
**fixture의 소유권을 해당 도메인 모듈에 귀속**시킨다.

### 소유권 원칙

fixture가 의존하는 도메인 타입을 소유한 모듈이 그 fixture를 소유한다.

| 모듈 | 소유 fixture | 의존 도메인 타입 |
|------|-------------|----------------|
| `:room` | `PlayerFixture`, `PlayersFixture`, `RoomFixture`, `RoomSessionClaimFixture`, `RouletteFixture`, `FixedLastValueGenerator`, `PlayerNameAuditFixture` | `coffeeshout.room.domain.*`, `coffeeshout.room.infra.*` |
| `:user` | `UserFixture`, `FriendshipFixture` | `coffeeshout.user.domain.*`, `coffeeshout.friend.domain.*` |
| `:game` | `CardGameDeckStub`, `CardGameFake`, `StubDeck`, `MiniGameDummy`, `MiniGameResultFixture` | `coffeeshout.cardgame.domain.*`, `coffeeshout.minigame.domain.*` |
| `:admin` | `PatchNoteFixture`, `ReportFixture` | `coffeeshout.patchnote.*`, `coffeeshout.report.*` |

### 소스 디렉토리 구조

```text
:room
  src/
    main/java/          ← 프로덕션 코드
    test/java/          ← room 전용 테스트 (TestApplication, 통합 테스트)
    testFixtures/java/  ← 다른 모듈과 공유할 fixture
```

### 소비 방법

fixture를 사용하는 모듈은 `testImplementation(testFixtures(project(":module")))` 으로 선언한다.

```kotlin
// :app/build.gradle.kts
testImplementation(testFixtures(project(":room")))
testImplementation(testFixtures(project(":game")))
testImplementation(testFixtures(project(":user")))
testImplementation(testFixtures(project(":admin")))

// :room/build.gradle.kts — UserFixture가 RoomServiceMemberTest에서 필요
testImplementation(testFixtures(project(":user")))

// :game/build.gradle.kts — MiniGameResultFixture가 PlayerFixture에 의존
"testFixturesImplementation"(project(":game-api"))
"testFixturesImplementation"(testFixtures(project(":room")))
testImplementation(testFixtures(project(":room")))

// :admin/build.gradle.kts — ReportFixture가 MiniGameType(game-api) 사용
"testFixturesImplementation"(project(":game-api"))
```

### 순환 의존 검증

`testFixtures`는 production 코드와 별개의 소스 셋이므로 기존 모듈 의존 방향을 바꾸지 않는다.

```text
:room testFixtures  ← :game testFixtures
:user testFixtures  ← :room testFixtures (소비)
:room testFixtures  ← :app test (소비)
:game testFixtures  ← :app test (소비)
:user testFixtures  ← :app test (소비)
```

`:room` → `:user` production 의존은 기존에도 있었으며 `:user testFixtures` 소비는 test 스코프이므로 순환이 발생하지 않는다.

## 고려한 대안

### 대안 A: fixture를 `:test-support`로 이동

`:test-support`에 `PlayerFixture` 등을 두고 모든 모듈이 소비하는 방안.

- **기각 이유**: `:test-support` → `:room` → `:test-support` 순환 의존 발생.
  `:room`은 현재 `testImplementation(project(":test-support"))`를 선언하고 있으므로
  `:test-support`가 `:room` 타입에 의존하는 fixture를 갖게 되면 순환이 생긴다.

### 대안 B: 복사 허용 (현행 유지)

각 모듈이 자기 scope에 맞는 fixture를 직접 복사해 갖는다.

- **기각 이유**: 이미 `PlayerFixture`가 `:room`, `:game`, `:app` 3곳에 완전히 동일한 내용으로 존재하는 것을 확인했다.
  fixture 인터페이스가 변경될 때 n개 모듈을 동기화해야 하며 drift가 발생하면 테스트 환경만 달라지는 묵시적 버그가 생긴다.

## 결과

### 긍정적 효과

- fixture 변경이 한 곳에서만 일어난다 (SSOT).
- 소유 모듈을 보면 fixture가 의존하는 도메인 경계를 즉시 파악할 수 있다.
- `:room`, `:game`, `:user` 모듈에서 14개 fixture 파일의 중복이 제거된다.

### 부정적 효과 / 트레이드오프

- `java-test-fixtures` 플러그인을 추가한 모듈은 Gradle이 `testFixturesCompileClasspath`, `testFixturesRuntimeClasspath` 등의 추가 configuration을 관리한다. 의존성 해석 비용이 미미하게 증가한다.
- 여러 도메인 타입을 조합하는 cross-cutting fixture (예: `:room` + `:game` 타입을 동시에 사용)는 상위 모듈(`:app:test`)에 두는 것이 맞다. `testFixtures`는 단일 도메인 경계 내의 fixture만 소유해야 한다.
- `testFixtures` 소스셋은 루트 `build.gradle.kts`의 `testCompileOnly` lombok 설정을 상속하지 않는다. fixture에서 `@NonNull` 등 lombok 어노테이션을 사용할 수 없으며, 필요 시 각 모듈 `build.gradle.kts`에 `"testFixturesCompileOnly"("org.projectlombok:lombok")`를 명시해야 한다.

### 미적용 대상

- `ExceptionAssertions`는 `:test-support`에 이미 `coffeeshout.support.ExceptionAssertions`로 존재한다.
  각 모듈의 `src/test/java/coffeeshout/fixture/ExceptionAssertions.java` 로컬 복사본 제거는 별도 cleanup 이슈에서 처리한다.
