# 0013. Room-Game 도메인 분리 — GameSession 애그리거트 도입

- 날짜: 2026-05-20
- 상태: 완료 (2026-05-21)

## 컨텍스트

현재 `:room` 모듈의 `Room` 클래스가 게임 컨테이너 역할을 겸하고 있다.

```java
// Room.java — 게임 관련 필드 3개
Queue<Playable> miniGames;       // 게임 대기열
List<Playable> finishedGames;    // 완료된 게임 목록
double adjustmentWeight;          // 룰렛 확률 가중치
```

그 결과 두 가지 문제가 겹쳐 있다.

**1. `:room` ↔ `:game` 양방향 결합**

```text
:room → :game  Room이 Playable(인터페이스), MiniGameType, MiniGameResult를 직접 보유
:game → :room  모든 게임 서비스가 RoomQueryService로 Room을 조회하고 room.findMiniGame() 호출
```

**2. 게임 타입이 `:room` 모듈 소스에 혼재**

`coffeeshout.minigame.domain` 패키지(`MiniGameResult`, `MiniGameType`, `MiniGameScore`)는 개념상 게임 도메인이지만 물리적으로 `:room` 모듈 소스트리(`room/src/main/java/`)에 위치한다.
`Playable` 인터페이스도 `room.domain` 패키지에 있다.

**증상**: 새 게임을 추가하거나 게임 타입을 변경할 때마다 `Room` 엔티티와 그 서비스를 함께 수정해야 한다. `:game` 단독 작업이 불가능하다.

## 결정

### 1. 게임 타입을 `:game` 모듈로 이전

다음 타입들을 `:room` 소스에서 `:game` 소스로 물리적으로 이전한다.

| 타입 | 현재 위치 | 이전 위치 |
|------|---------|---------|
| `Playable` | `room/domain/Playable.java` | `game/minigame/domain/Playable.java` |
| `MiniGameType` | `room/minigame/domain/MiniGameType.java` | `game/minigame/domain/MiniGameType.java` |
| `MiniGameResult` | `room/minigame/domain/MiniGameResult.java` | `game/minigame/domain/MiniGameResult.java` |
| `MiniGameScore` | `room/minigame/domain/MiniGameScore.java` | `game/minigame/domain/MiniGameScore.java` |

`Playable` 인터페이스의 `setUp`·`getScores` 시그니처에서 `Player` 대신 `PlayerName`을 사용하도록 변경한다.

```java
// 변경 전 — Player 객체 전체를 게임에 노출
void setUp(List<Player> players);
Map<Player, MiniGameScore> getScores();

// 변경 후 — 게임이 필요한 것은 이름뿐
void setUp(List<PlayerName> playerNames);
Map<PlayerName, MiniGameScore> getScores();
```

`MiniGameResult`의 맵 키도 `Player` → `PlayerName`으로 교체한다. 게임은 점수·순위를 플레이어 이름 기준으로만 관리하면 충분하며, 룰렛 확률(`Player.probability`)은 게임이 알 필요가 없다.

### 2. `GameSession` 애그리거트 신설 (`:game` 모듈)

게임 대기열과 완료 목록을 Room에서 분리해 `:game` 모듈 내 전용 애그리거트로 이관한다.

```java
// :game 모듈 신규
public class GameSession {
    private final JoinCode joinCode;            // Room과 동일한 식별자로 연결
    private final PlayerName hostName;          // 호스트 권한 검증용
    private final Queue<Playable> pendingGames;
    private final List<Playable> completedGames;

    // 전체 교체 방식: 프론트가 변경 시마다 전체 목록을 전송
    void replaceGames(PlayerName hostName, List<Playable> games);

    // Room의 플레이어 목록을 인자로 받아 setUp 위임
    Playable startNextGame(PlayerName hostName, List<PlayerName> playerNames);

    Playable findCompletedGame(MiniGameType type);
    List<MiniGameType> getSelectedTypes();
}
```

`GameSessionRepository` 인터페이스를 `:game`에 두고, `MemoryGameSessionRepository`(인메모리 구현체)를 함께 제공한다. 저장소 구현은 `Room`과 동일하게 Redis 기반 직렬화를 사용한다.

### 3. Room에서 게임 관련 코드 제거

Room의 책임을 "플레이어 집합 + 상태 + 룰렛 가중치"로 좁힌다.

**제거 대상**

```text
필드:   Queue<Playable> miniGames
        List<Playable> finishedGames

메서드: addMiniGame(), removeMiniGame(), clearMiniGames()
        startNextGame(), findMiniGame()
        getAllMiniGame(), getSelectedMiniGameTypes()
        isFirstStarted()
```

**변경 대상**

```java
// 변경 전 — MiniGameResult(게임 타입)를 Room이 직접 수신
void applyMiniGameResult(MiniGameResult miniGameResult)

// 변경 후 — 단순 순위 맵만 수신, Room은 게임 타입 불필요
void applyGameResult(Map<PlayerName, Integer> rankByPlayer)
```

`ProbabilityCalculator`는 플레이어 수·게임 횟수·가중치만 필요하므로 `:room`에 유지한다. 게임 횟수는 `applyGameResult` 호출 횟수를 Room이 내부적으로 카운트한다.

### 4. 게임 서비스 경로 변경

게임 인스턴스 조회를 Room이 아닌 GameSession에서 수행한다.

```java
// 변경 전
Room room = roomQueryService.getByJoinCode(joinCode);
Playable game = room.findMiniGame(MiniGameType.CARD);

// 변경 후
GameSession session = gameSessionRepository.getByJoinCode(joinCode);
Playable game = session.findCompletedGame(MiniGameType.CARD);
```

게임 종료 후 Room에 결과 반영:

```java
// 변경 전
room.applyMiniGameResult(miniGameResult);

// 변경 후 — 순위 맵(단순 값)만 전달
roomCommandService.applyGameResult(joinCode, result.toRankMap());
```

`result.toRankMap()`은 `MiniGameResult`의 `Map<PlayerName, Integer>` 뷰를 반환하는 메서드다.

게임이 끝날 때마다 룰렛 확률이 갱신되고 클라이언트에 이벤트가 발행된다. 이 흐름은 `RoomCommandService.applyGameResult()` 내부에서 확률 조정 후 기존 이벤트(`RouletteShowEvent` 등)를 그대로 발행하므로 변경이 없다. 이벤트 발행 로직은 Room 슬림화 대상이 아니다.

## 최종 의존 구조

```text
변경 전:  :room ←→ :game  (양방향)
변경 후:  :room  ← :game  (단방향)

:room   게임 타입 전혀 모름. Players, JoinCode, RoomState, 룰렛 가중치만 관리.
:game   PlayerName, JoinCode, RoomQueryService, RoomCommandService 최소 참조 유지.
```

새 게임 종류를 추가하거나 제거할 때 `:room` 코드를 건드리지 않아도 된다.

## 검토한 대안

**대안 A: 이벤트 기반 통신으로 완전 분리**

Room ↔ Game 간 통신을 도메인 이벤트로 전환한다. `GameFinishedEvent` → Room이 수신해 확률 조정.

채택하지 않은 이유: 이벤트 흐름이 추가되면 트랜잭션 경계·순서 보장·테스트 복잡도가 크게 높아진다. GameSession 도입만으로도 `:room → :game` 방향 결합을 끊을 수 있으므로 현재 규모에서 이벤트 분리는 과도한 복잡도다.

**대안 B: Room 분리 없이 서비스 계층만 재구성**

Room 엔티티를 그대로 두고 게임 서비스가 Room 대신 별도 캐시를 참조하도록 한다.

채택하지 않은 이유: 근본 원인(Room이 게임 컨테이너 역할 겸임)을 해소하지 못하고, Room 엔티티 불일치 문제만 새로 만든다.

## 핵심 제약

- `GameSession`은 `JoinCode`로 Room과 연결된다. Room 삭제 시 GameSession도 함께 삭제한다.
- `Room.applyGameResult()`는 완료된 게임 횟수를 내부 카운터로 유지하며, 이 값이 `ProbabilityCalculator`에 전달된다.
- `:room`은 `Playable`, `MiniGameType`, `MiniGameResult`, `MiniGameScore`를 import하지 않는다.

## 작업 계획

### Step 1 — 게임 타입 이전 + `Playable` 시그니처 변경 ✅

- `Playable`, `MiniGameType`, `MiniGameResult`, `MiniGameScore`를 `:game` 소스로 이동
- `Playable.setUp` / `getScores` 시그니처를 `PlayerName` 기반으로 변경
- 6개 게임 구현체 컴파일 통과 확인

### Step 2 — `GameSession` 신설 ✅

- `GameSession` 도메인 클래스 + `GameSessionRepository` 인터페이스 작성
- `MemoryGameSessionRepository` 구현체 작성
- `GameSessionErrorCode` 신설, `BusinessException` 전환
- 단위 테스트 작성

### Step 3 — Room 슬림화 ✅

- `miniGames`, `finishedGames` 필드 제거
- 게임 관련 메서드 제거
- `applyMiniGameResult` → `applyGameResult(Map<PlayerName, Integer>)` 변경
- `completedGameCount` 내부 카운터 추가

### Step 4 — 게임 서비스 경로 변경 ✅

- 게임 서비스에서 GameSession을 통해 게임 인스턴스 조회하도록 변경
- 게임 종료 후 `roomCommandService.applyGameResult(joinCode, rankMap)` 호출
- `RoomCommandService.updateMiniGames()`가 GameSession을 사용하도록 변경

### Step 5 — 컴파일 및 테스트 검증 ✅

```bash
./gradlew :room:compileJava    # Playable import 없어야 함
./gradlew :game:compileJava
./gradlew :room:test
./gradlew :game:test
```

### Step 6 — GameSession 생명주기 자동 관리 ✅

- `RoomRemovedEvent` 신설 (`room.domain.event`): 방 삭제 완료 시 발행
- `DelayedRoomRemovalService`: 방 삭제 후 `RoomRemovedEvent` 발행 추가
- `GameSessionCleanupListener` (`:game`): `RoomRemovedEvent` 수신 시 `GameSessionService.deleteSession()` 호출
- `GameSessionInitConsumer` (`:game`): `RoomCreateEvent` 수신 시 `GameSessionService.initSession()` 호출
- `EventDispatcher`: 동일 이벤트 타입에 여러 Consumer 등록 가능하도록 단일 → 팬아웃 방식으로 변경

### Step 7 — 삭제된 REST 엔드포인트 `:game` 모듈로 복구 ✅

ADR-0013 적용 시 `:room` 모듈의 세 엔드포인트가 `Playable`/`MiniGameType` 의존으로 인해 삭제됐다.
`MiniGameRestController` (`:game`)에서 `GameSession` 기반으로 재구현했다.

| 엔드포인트 | 이전 위치 | 복구 위치 | 변경 사항 |
|---|---|---|---|
| `GET /rooms/minigames` | `:room` `RoomRestController` | `:game` `MiniGameRestController` | 없음 |
| `GET /rooms/minigames/selected` | `:room` `RoomRestController` | `:game` `MiniGameRestController` | `Room` → `GameSession.getSelectedTypes()` |
| `GET /rooms/{joinCode}/miniGames/remaining` | `:room` `RoomRestController` | `:game` `MiniGameRestController` | 응답을 `RemainingMiniGameResponse` 래퍼로 복원 |

`RemainingMiniGameResponse`는 `room.ui.response`에서 `game.minigame.ui.response`로 이전했다. 팩토리 메서드도 `from(List<Playable>)` → `of(List<String>)` 로 변경해 `Playable` 의존을 제거했다.

## 구현 기록

### 설계 대비 변경 사항

**`addGame` / `removeGame` 미도입 → `replaceGames` 채택**

WebSocket 프로토콜(`MiniGameSelectEvent`)이 변경 시마다 현재 선택 목록 전체를 전송한다.
개별 추가·제거 API는 호출 경로가 없으므로, `replaceGames(hostName, List<Playable>)` 단일 메서드로 대체했다.
`replaceGames`는 원자적으로 전체 교체하면서 중복 게임 타입 선택을 방지한다.

**`GameSession`에 `hostName` 필드 추가**

생성자 `GameSession(JoinCode, PlayerName)`에서 호스트를 고정하고, `replaceGames` / `startNextGame` 호출 시마다 `validateHost(hostName)`로 검증한다.

**`GameSessionErrorCode` 신설**

`spring.util.Assert` 기반 예외 처리를 `BusinessException`으로 전환하면서 아래 에러코드를 추가했다.

| 코드 | HTTP | 설명 |
|------|------|------|
| `NOT_HOST` | 403 | 호스트 외 게임 세션 조작 시도 |
| `DUPLICATE_GAME` | 400 | 동일 게임 타입 중복 선택 |
| `NO_PENDING_GAMES` | 409 | 대기 게임 없는 상태에서 시작 요청 |
| `GAME_NOT_FOUND` | 404 | 완료된 게임 조회 실패 |

**`GameSessionService` 메서드 분리**

| 메서드 | 용도 |
|--------|------|
| `getOrCreateSession(JoinCode, PlayerName)` | 게임 시작 시 세션 생성 또는 조회 |
| `getSession(JoinCode)` | 읽기 전용 조회 (세션 반드시 존재) |
| `findSession(JoinCode)` | 세션 유무 불확실할 때 Optional 반환 |
| `initSession(JoinCode, PlayerName)` | 방 생성 시 세션 사전 초기화 (이미 존재하면 무시) |

**`GameRecoveryService` → `RoomRecoveryService` 리네이밍**

`GameRecoveryService`는 실제로 WebSocket 재연결 시 방 상태를 복구하는 역할로, 게임 복구가 아닌 방 복구 서비스다. 이름을 `RoomRecoveryService`로 정정했다. 컨트롤러(`GameRecoveryController` → `RoomRecoveryController`)와 API 인터페이스도 함께 변경됐다.

**`EventDispatcher` 팬아웃 방식 전환**

`RoomCreateEvent`를 기존 `RoomCreateConsumer`와 신규 `GameSessionInitConsumer`가 동시에 처리해야 하는 상황이 생겼다. 기존 `EventDispatcher`는 동일 타입 Consumer를 하나만 지원했으므로, `ApplicationContext.getBeanProvider().stream()`으로 전체 Consumer를 수집한 뒤 순차 실행하는 방식으로 변경했다.
