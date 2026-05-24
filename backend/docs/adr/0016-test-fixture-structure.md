# ADR-0016: 테스트 픽스처 구조 — testFixtures 소스 셋과 유형별 네이밍 전략

## 상태

적용됨 (2026-05-24)

## 컨텍스트

멀티 모듈 전환(ADR-0011) 이후 픽스처를 어디에 두어야 하는지 기준이 필요해졌다.

두 가지 문제가 있었다.

첫째, 도메인 객체 생성 코드가 각 모듈 `src/test/java/`에 중복 존재해 `:app` 통합 테스트에서
재사용할 수 없었다. 예를 들어 `:room`에 `PlayerFixture`가 있어도 `:app`은 이를 가져다
쓰지 못하고 자체적으로 다시 만들어야 했다.

둘째, 픽스처가 순수 팩토리인지, DB에 영속화하는 헬퍼인지, 행위를 모방하는 테스트 더블인지
구분 없이 섞여 있어 코드 읽는 사람이 의도를 파악하기 어려웠다.

## 결정

### 1. 공유 픽스처는 `src/testFixtures/java/coffeeshout/fixture/`에 둔다

모듈 간 공유가 필요한 픽스처는 Gradle `java-test-fixtures` 플러그인이 활성화된
소스 셋에 위치시킨다.

```text
:room, :user, :game, :admin  → src/testFixtures/java/coffeeshout/fixture/
:app                          → testImplementation(testFixtures(project(":room"))) 등으로 소비
```

모듈 내부에서만 쓰이는 픽스처(`TestDataHelper` 등)는 `src/test/java/coffeeshout/fixture/`에 둔다.

### 2. 패키지명은 항상 `coffeeshout.fixture`

모듈이 달라도 패키지명을 통일한다. `:app`에서 모든 모듈의 픽스처를 같은 패키지로 접근한다.

### 3. 유형별 네이밍 규칙

| 유형 | 명칭 패턴 | 특징 |
|---|---|---|
| 도메인 객체 팩토리 | `*Fixture` | 순수 Java 정적 팩토리, 스프링 컨텍스트 불필요 |
| DB 영속화 헬퍼 | `TestDataHelper` | `@Component`, 통합 테스트에서 DI로 사용 |
| 경량 대체 구현 | `*Fake` | 실제 로직을 갖지만 외부 의존을 제거한 구현 |
| 최소 더미 구현 | `*Dummy` | 인터페이스 계약을 최소한으로 충족, 로직 없음 |
| 반환값 제어 | `Stub*` | 특정 메서드의 반환값을 고정하거나 무력화 |

### 4. 픽스처 메서드명은 한글 도메인 용어 사용

```java
PlayerFixture.호스트꾹이()         // 플레이어 객체 생성
RoomFixture.호스트_꾹이()           // 방 객체 생성
TestDataHelper.방_생성(...)         // DB에 방 영속화
```

## 소스 셋별 구조

```text
src/testFixtures/java/coffeeshout/fixture/   ← 모듈 간 공유 픽스처
  ├── *Fixture.java    (도메인 객체 정적 팩토리)
  ├── *Fake.java       (경량 대체 구현)
  ├── *Dummy.java      (최소 구현 테스트 더블)
  └── Stub*.java       (반환값 고정)

src/test/java/coffeeshout/fixture/           ← 모듈 내부 전용 픽스처
  └── TestDataHelper.java    (@Component, DB 영속화)
```

Gradle 설정:

```kotlin
// :room, :user, :game, :admin — testFixtures 활성화
plugins {
    `java-test-fixtures`
}

// :app — testFixtures 소비
testImplementation(testFixtures(project(":room")))
testImplementation(testFixtures(project(":user")))
testImplementation(testFixtures(project(":game")))
testImplementation(testFixtures(project(":admin")))
```

## 고려한 대안

### 대안 A: 모든 픽스처를 `:test-support`에 집중

- 장점: 단일 위치에서 관리
- 단점: `:test-support`가 모든 도메인 타입에 의존하게 되어 모듈 경계 훼손
- 기각 이유: 픽스처는 해당 도메인 모듈이 소유해야 컴파일 의존성이 명확해짐

### 대안 B: 도메인별 픽스처 패키지 (`coffeeshout.room.fixture`, `coffeeshout.game.fixture`)

- 장점: 모듈 귀속이 패키지명으로 명시됨
- 단점: `:app` 통합 테스트에서 여러 픽스처를 임포트할 때 경로가 분산됨
- 기각 이유: 패키지 통일로 임포트 경로가 단순해지며, Gradle 소스 셋이 이미 모듈 귀속을 구분함

## 결과

- `:app` 통합 테스트가 `testFixtures(project(":room"))` 한 줄로 `:room`의 픽스처 전체를 재사용한다
- 클래스명 패턴(`*Fixture`, `*Fake`, `*Dummy`, `Stub*`, `TestDataHelper`)으로 픽스처 의도를 즉시 파악한다
- 순수 단위 테스트는 `*Fixture` 정적 팩토리만으로 데이터를 구성하고, DB 의존 통합 테스트는 `TestDataHelper`로 영속화한다
