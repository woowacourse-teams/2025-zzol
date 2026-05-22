# 0011. Gradle 멀티 모듈 전환

- 날짜: 2026-05-11
- 상태: 적용됨 (2026-05-22)

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

## 결정

### 1. 분리 기준 — 도메인 수직 분리

**수직 분리 (도메인별: :user / :room / :game …)**

도메인 경계를 모듈 경계와 일치시키는 구조를 채택한다.

1. **빌드 격리**: 게임 6종은 가장 자주 바뀌는 코드다. `:game` 분리 후 게임 수정 시 `:game` + `:app`만 재빌드된다.
2. **독립 배포**: `./gradlew :zzolbot:bootJar` 한 줄로 zzolbot 단독 패키징이 가능해진다.
3. **의존 위반 조기 발견**: 모듈 간 무단 참조를 컴파일 시점에 차단한다.

수직 분리는 모듈 내부 계층 위반을 Gradle이 잡지 못한다. 이 빈틈은 §10의 ArchUnit으로 보완한다.

### 2. 최종 모듈 맵 (10모듈)

```text
root
├── :common     # Spring 무관 순수 추상 (ErrorCode, BaseEvent, VO, TraceInfo 등)
├── :infra      # Spring + JPA + Redis + Outbox + Lock + IpBlock + Health + Metric
├── :websocket  # STOMP 플랫폼 (도메인 무지)
├── :game-api   # 게임 SPI (Playable, MiniGameFactory, FlowScheduler, Gamer 등)
├── :user       # user/, auth/, friend/
├── :room       # Room aggregate + Player + Roulette + RoomSessionToken
│               # + websocket room 핸들러 (SessionConnectEventListener 등)
├── :game       # 6게임 구현체 + minigame orchestration
├── :admin      # dashboard/, patchnote/, report/
├── :zzolbot    # AI 운영자 어시스턴트
└── :app        # Spring Boot 진입점, 모든 모듈 조합
```

의존 방향:

```text
:common
  ↑
:infra ──────────────────────────┐
  ↑                              │
:websocket    :game-api          │
  ↑    ↑      ↑                  │
:user  └──:room ──→ :game        │
             ↑         ↑        │
           :admin     :zzolbot   │
             ↑──────────┘        │
           :app ←────────────────┘
```

핵심 규칙:
- `:room`은 `:game-api`(Playable 추상)에 의존하지만 `:game`(구체 게임)은 모른다
- `:game`은 `:room`(Player, JoinCode)과 `:game-api`(Playable 구현 대상) 모두 의존
- `:common`은 Spring 의존이 없어 모든 모듈이 안전하게 import 가능

### 3. `:common`과 `:infra` 분리 이유

초안에서는 `:global`을 분리하지 않는 안을 검토했으나 최종적으로 분리를 채택했다.

`:common`은 Spring 의존이 전혀 없는 순수 추상(ErrorCode, BaseEvent, TraceInfo, NameValidator 등)을 담는다. 이를 Spring 설정/인프라 구현체와 분리하면:

- `:common`만 변경해도 `:infra`는 UP-TO-DATE — 빌드 캐시 효과
- `:infra`를 변경(Redis Config 수정 등)해도 `:common`은 재빌드 불필요
- `:common`이 Spring-free임을 컴파일 수준에서 보장

### 4. `:game-api` 분리 이유

`Playable`, `MiniGameFactory` 등 게임 SPI를 별도 모듈로 분리한다.

- `:room`은 어떤 게임이 있는지 모르고 `Playable` 추상만 안다 (OCP)
- `:game`이 `:room`을 의존하고 `:room`이 `:game`을 의존하면 순환이 생김
- `:game-api`를 중간 계약 레이어로 두면 `room → game-api ← game` 단방향으로 정리됨
- 새 게임 추가 시 `:game-api`의 `MiniGameType` enum에 항목 1줄 + `:game`에 구현만 추가

`Gamer` record도 여기 위치한다. 게임 구현체가 플레이어를 다룰 때 room의 `Player` 대신 `Gamer(String name, Long userId)`를 사용해 `:game → :room` 직접 의존 없이 플레이어 정보를 다룰 수 있다.

### 5. `gamecommon/`을 `:game-api`에 포함하는 이유

`FlowScheduler`, `EarlyFinishTrigger` 등 게임 플로우 추상은 `:game-api`에 위치한다. 이전 초안에서 `:room`에 포함하는 안을 검토했으나, 게임 플로우 추상화는 room 개념이 아니라 게임 구현체가 사용하는 SPI이므로 `:game-api`가 더 적절하다.

### 6. `:social`을 `:user`에 통합하는 이유

`friend/` 하나를 위해 모듈을 두는 비용 대비 얻는 격리 효과가 없다. 소셜 기능 로드맵이 구체화될 때 `:user`에서 분리하면 충분하다.

### 7. 순환 의존 처리 원칙

1. 공통 타입을 더 낮은 모듈로 이동
2. 이동이 어려우면 인터페이스를 하위 모듈에 정의하고 구현체를 상위 모듈에 위치하는 포트 패턴 적용
3. 형제 모듈 간 순환은 공통 타입을 공통 조상 모듈로 추출

### 8. `:websocket` ↔ `:room` 순환 의존 해소

멀티 모듈 전환 전 `:websocket` ↔ `:room` 간 양방향 참조를 확인했다.

`SessionConnectEventListener`, `PlayerDisconnectionService`, `DelayedPlayerRemovalService`, `RoomStateUpdateEventListener`, `GameRecoveryService` 등 room 상태를 직접 변경하는 6개 파일을 `:room.infra.websocket`으로 이동했다.

이동 후 `:websocket`은 순수 STOMP 인프라(세션 추적, 메트릭, 핸드셰이크, 메시지 브로커 래핑)만 담당하고, 의존 방향은 `:room` → `:websocket` 단방향으로 정리됐다.

`RoomSessionToken*` 관련 코드도 `:room.infra.auth`로 이동했다.

### 9. 테스트 배치

- 각 모듈의 `src/test/java/`는 해당 모듈 내 테스트를 담는다
- 도메인 픽스처(`RoomFixture` 등)는 각 모듈의 `src/test/java/coffeeshout/fixture/`에 위치
- 여러 모듈이 동일 픽스처가 필요한 경우 해당 모듈 `src/test/`에 복사본을 둔다
- TestContainers 부트스트랩은 `:app/src/test/`에 위치 (Spring context 전체 필요)

### 10. ArchUnit으로 모듈 내 계층 의존성 강제

Gradle 멀티 모듈은 **모듈 간** 의존 위반을 컴파일 타임에 차단하지만, **모듈 내부** 계층 위반은 잡지 못한다. 이 빈틈을 ArchUnit으로 메운다.

**배치 위치**: `:app/src/test/java/coffeeshout/arch/`에 아키텍처 규칙 테스트를 위치시킨다. 전체 클래스패스 가시성이 필요하기 때문이다.

**적용 규칙**:
- 계층 의존 방향: `domain ← application ← infra` (역방향 금지)
- 게임 6종 간 직접 참조 금지
- minigame orchestration이 개별 게임을 직접 참조 금지
- admin 내 dashboard ↔ patchnote ↔ report 상호 참조 금지
- game 모듈 내부 6개 게임의 domain→infra, application→ui 역방향 금지

## 작업 결과

모든 Phase 완료 (2026-05-22 기준).

### 완료 사항

- [x] 10개 Gradle 모듈 구성 및 소스 이동
- [x] 순환 의존 R1~R9 해소
- [x] ArchUnit 30개+ 규칙 도입
- [x] 테스트 분리: 모듈별 단위 테스트 + `:app` 통합 테스트
- [x] `Playable` → `:game-api`, `MiniGameFactory` SPI 도입 (OCP)
- [x] `PlayerView` → `Gamer` record 교체 (unchecked cast 제거)
- [x] `RankingNicknameProvider` port 방식 → 이벤트 방식 전환 (room 단독 기동 가능)
- [x] 불필요 의존 제거 (`:game-api`의 `:infra`, `micrometer` 등)

## 결과

- **증분 빌드**: 변경된 모듈과 이를 의존하는 모듈만 재빌드
- **의존 위반 조기 발견**: 컴파일 시점에 무단 참조 차단
- **독립 배포 가능**: `./gradlew :zzolbot:bootJar`
- **테스트 격리**: 단위 테스트는 모듈별, 통합 테스트는 `:app`에서 실행
