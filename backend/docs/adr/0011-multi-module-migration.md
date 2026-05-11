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
├── :global     # exception, filter, health, lock, metric, outbox, ratelimit, redis, trace, config
│               # common/nickname 유틸 흡수 (3개 클래스, 별도 모듈 불필요)
├── :websocket  # STOMP 인터셉터, 세션 추적, 메시지 복구 (:global 의존)
├── :user       # user, auth, friend (:global 의존)
├── :room       # room + gamecommon + minigame + 각 게임 6종 (:global, :websocket, :user 의존)
├── :zzolbot    # AI 운영 어시스턴트 (:global, :room 의존)
├── :admin      # dashboard, patchnote, report (:global, :room, :user 의존)
└── :app        # CoffeeShoutApplication — 진입점, 모든 모듈 조합
```

**게임 6종을 `:room` 안에 포함하는 이유**

각 게임은 `room.domain.JoinCode`, `room.application.*` 에 강하게 결합되어 있어 `:room` 없이는 독립 컴파일이 불가능하다. 게임 단위 독립 배포 계획도 없다. 게임 모듈을 개별 분리하면 모듈 수만 늘고 빌드 설정 복잡도가 이익을 초과한다.

**`:core`/`:shared` 모듈을 두지 않는 이유**

도메인 간 공유 타입(`JoinCode`, `ErrorCode`)이 이미 의존 방향에 맞게 배치되어 있다. 별도 공유 모듈을 도입하면 의존 그래프에 다이아몬드가 생기고, 현재 공유 범위는 그 비용을 정당화하지 않는다.

### 3. 순환 의존 처리 원칙

멀티 모듈 전환 시 순환 의존이 발견되면 **상위 모듈 기준**으로 처리한다.

- 하위 모듈(`:global`)이 상위 모듈(`:room`)을 역참조하는 경우 → 공통 타입을 `:global`로 내리거나, 인터페이스를 `:global`에 정의하고 구현체를 `:room`에 두는 포트 패턴 적용
- 형제 모듈 간 순환 → 공통 타입을 더 낮은 공통 조상 모듈로 이동

### 4. 테스트 배치

각 모듈의 `src/test/`는 해당 모듈 내에 위치한다. TestContainers 설정·픽스처처럼 여러 모듈이 공유하는 테스트 인프라는 `:app` 모듈의 `testFixtures` 소스셋에 둔다.

## 작업 계획

### Phase 0 — 패키지 정리 (완료, `be/refactor/1250-global`)

Gradle 모듈 경계와 패키지 경계를 일치시키는 사전 작업이다.

- [x] `global/` 내 횡단 관심사만 남기기 (websocket, zzolbot 등 격상)
- [x] `LogAspect` → `room.aspect` 이동
- [x] `global.zzolbot` → `zzolbot` 최상위 격상

남은 항목:

- [ ] `common/` → `global/` 흡수 (NameValidator 등 3개 클래스)
- [ ] `docs/architecture.md` 패키지 구조 표 갱신

### Phase 1 — Gradle 멀티 모듈 골격 구성

목표: 컴파일은 그대로 통과하면서 Gradle 모듈 경계만 선언한다.

1. `settings.gradle.kts` 생성, `include(":global", ":websocket", ":user", ":room", ":zzolbot", ":admin", ":app")` 등록
2. 루트 `build.gradle.kts` → `subprojects {}` 블록으로 공통 설정(Java 버전, 공통 플러그인) 추출
3. 각 모듈 디렉토리 + `build.gradle.kts` 생성 (초기에는 루트 의존성 목록 전체를 임시로 복사)
4. 소스 파일을 모듈별 디렉토리로 이동
5. `./gradlew build` 통과 확인

### Phase 2 — 의존성 선언 정제

목표: 각 모듈이 실제로 필요한 의존성만 선언한다.

| 모듈 | 의존 모듈 | 주요 외부 의존성 |
|---|---|---|
| `:global` | 없음 | Spring Core, AOP, Redis, JPA, JSqlParser 제외 |
| `:websocket` | `:global` | Spring WebSocket, STOMP |
| `:user` | `:global` | Spring Security, OAuth2, JWT |
| `:room` | `:global`, `:websocket`, `:user` | Spring Web, WebSocket |
| `:zzolbot` | `:global`, `:room` | Gemini AI, JSqlParser, Resilience4j |
| `:admin` | `:global`, `:room`, `:user` | Thymeleaf, Spring Security |
| `:app` | 모든 모듈 | Spring Boot 플러그인, Flyway, 드라이버 |

### Phase 3 — 모듈 간 의존 위반 해소

Phase 1 이후 Gradle이 잡아내는 컴파일 오류(잘못된 패키지 참조)를 제거한다. 예상 발생 지점:

- 게임 모듈에서 `room.*` 이 아닌 다른 게임 모듈을 참조하는 경우
- `:global` 내에서 도메인 타입을 참조하는 경우 (역의존)

### Phase 4 — 빌드 검증 및 캐시 확인

```bash
# 모듈 단위 테스트 실행
./gradlew :zzolbot:test
./gradlew :room:test

# 캐시 히트 확인 — :global만 변경 시 :room이 재빌드되는지
./gradlew :room:compileJava --rerun-tasks
```

## 결과

- **증분 빌드**: 변경된 모듈과 이를 의존하는 모듈만 재빌드
- **의존 위반 조기 발견**: 컴파일 시점에 무단 참조 차단
- **zzolbot 독립 실행 가능**: `./gradlew :zzolbot:bootJar` (향후 별도 배포 시)
- **팀 소유권 명확화**: 모듈 단위로 PR 리뷰 범위 구분 가능

## 미결 사항

- `gamecommon/`, `minigame/`을 `:room` 안에 두는 것이 아니라 `:game-common` 중간 모듈로 분리할지 — 게임 종류가 늘어날 경우 재검토
- `:admin`의 독립 배포 필요성 — 현재는 `:app` 합산 배포로 충분하다면 Phase 1에서 `:app` 안에 포함시킬 수 있음
