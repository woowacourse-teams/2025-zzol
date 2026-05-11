# 0011. Gradle 멀티 모듈 전환

- 날짜: 2026-05-11
- 상태: 초안

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

수평 분리(api/domain/infra 계층별)는 각 계층 모듈이 서로 의존하는 구조여서 변경 격리 효과가 낮다. 도메인 단위 수직 분리를 채택한다.

### 2. 모듈 맵

```text
root
├── :common     # global.exception, global.config, global.health, global.filter
│               # + common/ (닉네임 유틸) 흡수
├── :infra      # global.redis, global.outbox, global.lock
│               # global.trace, global.ratelimit, global.metric
├── :websocket  # STOMP 인터셉터, 세션 추적, 메시지 복구
├── :user       # user/, auth/
├── :social     # friend/ (향후 소셜 기능 확장 예약)
├── :room       # room/ + gamecommon/ + minigame/
├── :game       # blockstacking/, cardgame/, laddergame/,
│               # racinggame/, speedtouch/, blindtimer/
├── :admin      # dashboard/, patchnote/, report/
├── :zzolbot    # zzolbot/
└── :app        # CoffeeShoutApplication — 진입점, 모든 모듈 조합
```

의존 방향:

```text
:common
  ↑
:infra
  ↑
:websocket ← :user ← :social
  ↑              ↑
:room ──────────┘
  ↑
:game
:admin  → :room, :user
:zzolbot→ :room
:app    → all
```

### 3. `:common` vs `:infra` 분리 이유

`global/`을 두 모듈로 나눈다.

- **`:common`**: Spring-agnostic 또는 최소 의존. 예외 계층(`BusinessException`, `ErrorCode`), 공통 설정, 헬스체크, 닉네임 유틸. 도메인 모듈이 기술 인프라 없이 컴파일 가능하도록 하는 최소 기반.
- **`:infra`**: Redis, Outbox, Lock, Trace, RateLimit, Metric 구현체. `:common`에 의존. 기술 스택 교체 시 이 모듈만 영향받는다.

이 분리로 순수 도메인 로직 테스트 시 `:infra` 의존 없이 `:common`만으로 컴파일 가능해진다.

### 4. `gamecommon/`을 `:room`에 포함하는 이유

`gamecommon.flow/`(`FlowScheduler`, `EarlyFinishTrigger`)와 `minigame/`(`MiniGameType`)은 `room.domain.JoinCode` 등 room 타입을 참조한다. 별도 `:gamecommon` 모듈로 분리하면 `:gamecommon` → `:room` 의존이 생기는데, 이렇게 되면 `:room`이 게임 플로우 추상화에 의존할 수 없어 `RoomService.startMinigame()` 같은 메서드가 구현 불가능해진다.

`:room` 모듈이 `room/ + gamecommon/ + minigame/`을 포함하면 이 의존을 모듈 내부로 해소한다.

### 5. `:game` 분리 효과

게임 6종은 프로젝트에서 가장 자주 변경되는 코드다. 기존 구조에서는 게임 하나를 수정해도 `room` 포함 전체가 재빌드된다. `:game` 분리 후에는 `:game` + `:app`만 재빌드된다. `:room`은 방 관리 로직만 담아 변경 빈도가 낮아진다.

### 6. 공유 타입 처리 원칙

멀티 모듈 전환 시 순환 의존이 발견되면 다음 순서로 처리한다.

1. 공통 타입을 더 낮은 모듈(`:common` → `:infra` → `:room` 순)로 이동
2. 이동이 어려우면 인터페이스를 하위 모듈에 정의하고 구현체를 상위 모듈에 두는 포트 패턴 적용
3. 형제 모듈 간 순환은 공통 타입을 공통 조상 모듈로 추출

### 7. 테스트 배치

각 모듈의 `src/test/`는 해당 모듈 내에 위치한다. TestContainers 설정·픽스처처럼 여러 모듈이 공유하는 테스트 인프라는 `:app` 모듈의 `testFixtures` 소스셋에 둔다.

## 작업 계획

### Phase 0 — 패키지 정리 (진행 중, `be/refactor/1250-global`)

Gradle 모듈 경계와 패키지 경계를 일치시키는 사전 작업이다.

- [x] `global/` 내 횡단 관심사만 남기기 (websocket, zzolbot 등 격상)
- [x] `LogAspect` → `room.aspect` 이동
- [x] `global.zzolbot` → `zzolbot` 최상위 격상
- [x] `docs/architecture.md` 패키지 구조 표 갱신

남은 항목:

- [ ] `common/` → `global/` 흡수 또는 `:common` 모듈 경계로 편입 확정
- [ ] `global/` 내부를 `:common` 대상(`exception`, `config`, `health`, `filter`)과 `:infra` 대상(`redis`, `outbox`, `lock`, `trace`, `ratelimit`, `metric`)으로 구분하는 패키지 정리

### Phase 1 — Gradle 멀티 모듈 골격 구성

목표: 컴파일은 그대로 통과하면서 Gradle 모듈 경계만 선언한다.

1. `settings.gradle.kts` 생성, 10개 모듈 등록
2. 루트 `build.gradle.kts` → `subprojects {}` 블록으로 공통 설정 추출
3. 각 모듈 디렉토리 + `build.gradle.kts` 생성 (초기에는 루트 의존성 전체를 임시 복사)
4. 소스 파일을 모듈별 디렉토리로 이동
5. `./gradlew build` 통과 확인

### Phase 2 — 의존성 선언 정제

목표: 각 모듈이 실제로 필요한 의존성만 선언한다.

| 모듈          | 의존 모듈                                     | 주요 외부 의존성                          |
|-------------|-------------------------------------------|-------------------------------------|
| `:common`   | 없음                                        | Spring Core, Validation             |
| `:infra`    | `:common`                                 | Redis, JPA, Redisson, OTel          |
| `:websocket`| `:common`, `:infra`                       | Spring WebSocket, STOMP             |
| `:user`     | `:common`, `:infra`, `:websocket`         | Spring Security, OAuth2, JWT        |
| `:social`   | `:common`, `:infra`, `:user`              |                                     |
| `:room`     | `:common`, `:infra`, `:websocket`, `:user`| Spring Web                          |
| `:game`     | `:common`, `:infra`, `:websocket`, `:room`|                                     |
| `:admin`    | `:common`, `:infra`, `:room`, `:user`     | Thymeleaf, Spring Security          |
| `:zzolbot`  | `:common`, `:infra`, `:room`              | Gemini AI, JSqlParser, Resilience4j |
| `:app`      | 모든 모듈                                    | Spring Boot 플러그인, Flyway, DB 드라이버  |

### Phase 3 — 모듈 간 의존 위반 해소

Phase 1 이후 Gradle이 잡아내는 컴파일 오류를 제거한다. 예상 발생 지점:

- 게임 모듈에서 다른 게임 모듈을 직접 참조하는 경우
- `:infra` 내에서 도메인 타입을 역참조하는 경우
- `:websocket`에서 `:room` 타입을 참조하는 경우 (포트 패턴으로 역전 필요)

### Phase 4 — 빌드 검증 및 캐시 확인

```bash
# 모듈 단위 테스트 실행
./gradlew :zzolbot:test
./gradlew :game:test

# 캐시 히트 확인 — :game만 변경 시 :room이 재빌드되지 않아야 함
./gradlew :game:compileJava
./gradlew :room:compileJava  # UP-TO-DATE 여야 함
```

## 결과

- **증분 빌드**: 변경된 모듈과 이를 의존하는 모듈만 재빌드. 게임 수정 시 `:game` + `:app`만 재빌드.
- **의존 위반 조기 발견**: 컴파일 시점에 무단 참조 차단
- **독립 배포 가능**: `./gradlew :zzolbot:bootJar` (향후 별도 배포 시)
- **팀 소유권 명확화**: 모듈 단위로 PR 리뷰 범위 구분 가능

## 미결 사항

- `:social` 범위 — 현재 `friend/`만 포함. 향후 소셜 피드, 활동 알림 등 추가 시 그대로 확장
- `:websocket`이 `:room` 타입(PlayerKey 등)을 참조하는지 Phase 3에서 확인 필요. 참조한다면 인터페이스를 `:common`으로 내리거나 이벤트 기반으로 역전
