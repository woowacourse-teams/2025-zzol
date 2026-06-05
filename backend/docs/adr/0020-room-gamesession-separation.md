# 0020. Room-GameSession 분리 — 게임 대기열 소유권을 `:game`으로 이관

- 날짜: 2026-06-05
- 상태: 승인 (구현 예정)
- 참조 구현: `be/refactor/game-entity-ownership` 브랜치 (`d7abc0ea`, `ddfee28b`, `53cf40cc`, `af0d1a70`) — 멀티 모듈 전환 이전 코드베이스에서 동일 설계를 구현·검증한 선행 작업

## 컨텍스트

ADR-0011 멀티 모듈 전환으로 게임 타입(`Playable`, `Gamer`, `MiniGameType`, `MiniGameResult`, `MiniGameScore`)은 `:game-api`로 이전됐다. 그러나 **게임 인스턴스의 소유권은 여전히 `Room`에 있다.**

```java
// room/domain/Room.java — 게임 컨테이너 역할 겸임 중
private final Queue<Playable> miniGames;       // 게임 대기열
private final List<Playable> finishedGames;    // 완료된 게임 목록

public void addMiniGame(PlayerName hostName, Playable miniGame) { ... }
public void removeMiniGame(PlayerName hostName, Playable miniGame) { ... }
public void applyMiniGameResult(MiniGameResult miniGameResult) { ... }
public List<Playable> getAllMiniGame() { ... }
public List<MiniGameType> getSelectedMiniGameTypes() { ... }
```

그 결과 다음 문제가 남아 있다.

**1. Room의 책임 과다**

Room이 "플레이어 집합 + 방 상태 + 룰렛 가중치"에 더해 게임 대기열의 추가·제거·시작·결과 반영까지 담당한다. 게임 선택 정책(개수 제한, 중복 방지)이나 게임 시작 규칙이 바뀔 때마다 `Room` 엔티티와 `RoomService`를 수정해야 한다.

**2. `:room`이 게임 수명주기에 결합**

게임 조회 경로가 `roomQueryService.getByJoinCode(joinCode)` → `room.findMiniGame(type)`이므로, `:game`의 모든 게임 서비스가 게임 인스턴스를 얻기 위해 Room 애그리거트 전체를 거친다. 게임 진행 중 발생하는 모든 조회가 Room을 통과해 `:room`이 과부하된다.

**3. REST/WebSocket 엔드포인트 위치 부적절**

미니게임 목록·선택 조회 엔드포인트(`GET /rooms/minigames`, `GET /rooms/minigames/selected`, `GET /rooms/{joinCode}/miniGames/remaining`)가 `:room`의 `RoomRestController`에 있어, `:room` UI 계층이 `Playable`/`MiniGameType`을 직접 다룬다.

## 결정

### 1. `JoinCode` 슬림화 후 `:game-api` 이전

`JoinCode`는 Room과 GameSession을 잇는 공유 식별자이므로 `:game-api`의 `gamecommon` 패키지로 이전해 `Gamer`와 나란히 둔다. `:room`은 이미 `:game-api`에 의존하므로 방향 위반이 없고, `:game`·`:game-api`의 이벤트와 GameSession이 `:room` 없이 `JoinCode` 타입을 사용할 수 있게 된다.

단, 현재 `JoinCode`는 순수 값 객체가 아니므로 **이전 전 슬림화가 선행**돼야 한다.

```java
// 현재 — 가변 QrCode 필드와 RoomErrorCode 의존이 섞여 있음
public final class JoinCode {
    private final String value;
    private QrCode qrCode;                      // 방 입장 관심사 (가변)
    public void assignQrCode(QrCode qrCode);    // 가변 메서드
    // 검증 실패 시 RoomErrorCode 사용
}
```

- `QrCode` 필드·`assignQrCode()`를 `JoinCode`에서 제거하고 `Room`의 필드로 이동한다. QR 코드는 방 입장 UX 관심사이지 식별자의 속성이 아니다.
- 검증 예외는 `RoomErrorCode` 대신 `:game-api`에 신설하는 `JoinCodeErrorCode`를 사용한다 (`BusinessException`·`ErrorCode`는 `:common` 소속이므로 사용 가능).
- 이전 후 `JoinCode`는 불변 순수 값 객체로 유지한다.

`:common` 이전도 검토했으나, `Gamer`가 이미 `gamecommon`에 자리잡아 "모듈 경계를 넘는 게임 식별자 집합"이라는 응집이 형성돼 있고, `JoinCode`를 쓰는 모듈(`:room`/`:game`/`:app`/`:zzolbot`)이 모두 `:game-api`에 도달 가능하므로 `gamecommon`을 택한다.

### 2. `GameSession` 애그리거트 신설 (`:game` 모듈)

게임 대기열과 완료 목록을 Room에서 분리해 `:game` 모듈 전용 애그리거트로 이관한다.

```java
// game/minigame/domain/GameSession.java — 신규
public class GameSession {
    private final JoinCode joinCode;            // Room과 동일한 식별자로 1:1 연결 (:game-api 타입)
    private final Gamer host;                   // 호스트 권한 검증용 (:game-api 타입)
    private final Queue<Playable> pendingGames;
    private final List<Playable> completedGames;
    private GameSessionStatus status;           // READY | PLAYING | DONE

    // 전체 교체 방식: 프론트가 선택 변경 시마다 전체 목록을 전송하는 프로토콜
    // READY 상태에서만 허용 — 게임 진행 중 목록 변경 차단
    public void replaceGames(Gamer requester, List<Playable> games);

    // READY → PLAYING 전이. Room의 플레이어 목록(Gamer)을 인자로 받아 setUp 위임
    public Playable startNextGame(Gamer requester, List<Gamer> gamers);

    // PLAYING → READY(대기열 잔존) 또는 DONE(대기열 소진). 게임 종료 시 호출
    public void finishCurrentGame();

    public Playable findCompletedGame(MiniGameType type);
    public List<MiniGameType> getSelectedTypes();
}
```

```java
// game/minigame/domain/GameSessionStatus.java — 신규
public enum GameSessionStatus {
    READY,      // 게임 선택·변경 가능
    PLAYING,    // 게임 진행 중 — 대기열 변경 불가
    DONE,       // 모든 게임 완료
}
```

- `addGame`/`removeGame` 개별 API는 만들지 않는다. WebSocket 프로토콜(`MiniGameSelectMessage`)이 변경 시마다 현재 선택 목록 전체를 전송하므로 호출 경로가 없다. `replaceGames`가 원자적으로 전체 교체하며 중복 게임 타입 선택을 함께 검증한다.
- **상태 기반 변경 가능 검증**: 게임 중 여부를 Room 상태 조회 없이 GameSession 스스로 식별한다. `replaceGames`는 `READY`에서만 허용하고, `PLAYING` 중 호출은 `GAME_IN_PROGRESS`로 거부한다. `startNextGame`은 `READY`+대기열 존재 조건에서 `PLAYING`으로 전이하며, 게임 종료(`MiniGameFinishedEvent` 처리 시점)에 `finishCurrentGame()`으로 `READY` 또는 `DONE`으로 복귀한다. Room의 `RoomState`(방 화면 흐름: READY/PLAYING/SCORE_BOARD/ROULETTE/DONE)와 역할이 다르다 — `GameSessionStatus`는 대기열 불변식 보호 전용이다.
- 호스트 검증은 Room이 아닌 GameSession에서 수행한다. 생성 시 `host`(`Gamer`)를 고정하고 `replaceGames`/`startNextGame` 진입점에서 `validateHost()`로 확인한다. Room 조회 없이 권한 검증이 끝난다. `:room`의 `PlayerName`이 아닌 `:game-api`의 `Gamer`를 사용해 GameSession 도메인의 `:room` 의존을 없앤다.
- 검증 실패는 `BusinessException` + `GameSessionErrorCode`로 처리한다.

| 코드 | HTTP | 설명 |
|------|------|------|
| `NOT_HOST` | 403 | 호스트 외 게임 세션 조작 시도 |
| `DUPLICATE_GAME` | 400 | 동일 게임 타입 중복 선택 |
| `GAME_IN_PROGRESS` | 409 | 게임 진행 중 대기열 변경 시도 |
| `NO_PENDING_GAMES` | 409 | 대기 게임 없는 상태에서 시작 요청 |
| `GAME_NOT_FOUND` | 404 | 완료된 게임 조회 실패 |

`GameSessionRepository` 인터페이스를 `minigame.domain`에 두고, `MemoryGameSessionRepository`(`ConcurrentHashMap` 기반)를 `minigame.infra`에 구현한다.

`GameSessionService`(application)는 다음 메서드로 구성한다.

| 메서드 | 용도 |
|--------|------|
| `initSession(JoinCode, Gamer)` | 방 생성 시 세션 사전 초기화 (이미 존재하면 무시) |
| `getSession(JoinCode)` | 읽기 전용 조회 (세션 반드시 존재) |
| `findSession(JoinCode)` | 세션 유무 불확실할 때 `Optional` 반환 |
| `updateGames(MiniGameSelectEvent)` | `replaceGames` 위임 |
| `deleteSession(JoinCode)` | 방 삭제 시 세션 정리 |

### 3. Room 슬림화

Room의 책임을 "플레이어 집합 + 방 상태 + 룰렛 가중치"로 좁힌다.

**제거 대상**

```text
필드:   Queue<Playable> miniGames
        List<Playable> finishedGames

메서드: addMiniGame(), removeMiniGame()
        startNextGame(), findMiniGame()
        getAllMiniGame(), getSelectedMiniGameTypes()
        isFirstStarted() 등 게임 대기열 의존 메서드 전부
```

**변경 대상**

```java
// 변경 전 — MiniGameResult(게임 타입)를 Room이 직접 수신
public void applyMiniGameResult(MiniGameResult miniGameResult)

// 변경 후 — 단순 순위 맵만 수신, Room은 게임 타입 불필요
public void applyGameResult(Map<PlayerName, Integer> rankByPlayer)
```

- `ProbabilityCalculator`는 플레이어 수·게임 횟수·가중치만 필요하므로 `:room`에 유지한다.
- 게임 횟수는 기존 `miniGames.size() + finishedGames.size()` 계산을 대체해, `applyGameResult` 호출 횟수를 Room이 내부 카운터(`completedGameCount`)로 유지한다.
- `applyGameResult` 호출 경로는 결정 5의 이벤트 흐름을 따른다. `:game`이 `RoomCommandService`를 직접 호출하지 않는다.

확률 조정 후 룰렛 이벤트(`RouletteShowEvent` 등) 발행 흐름은 `RoomCommandService` 내부에 그대로 유지한다. 이벤트 발행 로직은 슬림화 대상이 아니다.

### 4. 게임 서비스 조회 경로 변경 + 게임 내부 `Player` 의존 제거

```java
// 변경 전
Room room = roomQueryService.getByJoinCode(joinCode);
Playable game = room.findMiniGame(MiniGameType.CARD);

// 변경 후
GameSession session = gameSessionRepository.getByJoinCode(joinCode);
Playable game = session.findCompletedGame(MiniGameType.CARD);
```

`MiniGameSelectConsumer`(현재 `:room` `infra.messaging.consumer`)는 `:game`으로 이동해 `GameSessionService.updateGames()`를 호출한다. WebSocket 수신(`RoomWebSocketController` → Redis Stream 발행) 경로는 변경 없다.

**게임 내부 값도 `Player` 대신 `Gamer`를 사용한다.** 현재 `Playable` 경계 시그니처는 `Gamer`이지만, 게임 내부 도메인 21개 파일(`BlindTimerPlayers`, `PlayerHands`, `Runners`, `Poles`, `SpeedTouchPlayer` 등)이 여전히 `room.domain.player.Player`를 직접 import한다. 게임이 필요한 것은 식별(이름·userId)뿐이고 `Player`의 확률·준비 상태는 게임이 알 필요도, 변경할 권한도 없다. 내부 컬렉션·점수 보관 키를 `Gamer`(`:game-api`)로 전면 교체해 `:game` → `:room` 직접 의존을 줄인다. `setUp(List<Gamer>)`로 받은 객체를 그대로 흘려보내면 되므로 경계 변환 비용도 사라진다.

### 5. 게임 결과 전달 — `:game-api` 이벤트 경유 (in-process 동기)

게임 종료 시 `:game`이 `RoomCommandService.applyGameResult()`를 직접 호출하지 않고, `:game-api`의 이벤트를 발행해 `:room`이 수신하는 방식으로 역전한다. 참조 브랜치는 직접 호출 방식이었으나, 본 ADR에서는 모듈 결합 제거를 위해 이벤트 방식을 채택한다.

기존 `MiniGameFinishedEvent`(`:game-api` `minigame.event.dto`, 6개 게임 종료 지점에서 이미 발행 중)에 순위 맵을 추가한다.

```java
// :game-api — 기존 이벤트에 ranks 필드 추가
public record MiniGameFinishedEvent(
        String eventId,
        String joinCode,
        String miniGameType,
        Map<String, Integer> ranks    // playerName 값 → 순위 (원시 타입만 사용)
) { ... }
```

순위 맵 변환(`MiniGameResult` → `Map<String, Integer>`)은 `:game-api`의 `MiniGameResult.toRankMap()`으로 제공해 6개 게임의 중복 변환을 막는다.

수신 측은 리스너 2개가 같은 이벤트를 각자 처리한다.

```text
게임 종료 (예: CardGameStep.FINISH_GAME)
  → eventPublisher.publishEvent(new MiniGameFinishedEvent(joinCode, type, result.toRankMap()))
      ├─ MiniGameResultSaveEventListener (:game, 기존)  → 결과 DB 저장
      └─ MiniGameResultRoomListener     (:room, 신규)  → RoomCommandService.applyGameResult()
                                                          → 확률 조정 + 룰렛 이벤트 발행
```

**동기 실행이 전제 조건이다.** 현재 코드베이스에는 `@EnableAsync`·`@Async`·커스텀 `ApplicationEventMulticaster`가 없어 Spring 이벤트 리스너는 발행자 스레드에서 동기 실행된다. 따라서 `publishEvent()` 반환 시점에 확률 조정이 완료되므로, 직접 호출과 동일한 순서·예외 전파를 보장하면서 모듈 결합만 제거된다. Redis Stream을 경유하지 않는 이유는 검토한 대안 B를 참조한다.

### 6. GameSession 생명주기 자동 관리 (이벤트 기반)

GameSession 생성·정리를 서비스 호출 누락에 의존하지 않도록 Room 생명주기 이벤트에 연결한다.

```text
RoomCreateEvent  (기존)  → GameSessionInitConsumer    (:game) → initSession()
RoomRemovedEvent (신설)  → GameSessionCleanupListener (:game) → deleteSession()
```

- `RoomRemovedEvent`를 `room.domain.event`에 신설하고, `DelayedRoomRemovalService`가 방 삭제 완료 후 발행한다.
- 현재 `EventDispatcher`(`:infra`)는 이벤트 타입당 Consumer 1개만 지원한다(미등록 시 warn 후 스킵). `RoomCreateEvent`를 기존 `RoomCreateConsumer`와 신규 `GameSessionInitConsumer`가 함께 처리해야 하므로, `ObjectProvider.stream()`으로 동일 타입 Consumer 전체를 수집해 순차 실행하는 **팬아웃 방식으로 변경**한다.

### 7. 미니게임 REST 엔드포인트 `:game` 이전

| 엔드포인트 | 현재 위치 | 이전 위치 | 변경 사항 |
|---|---|---|---|
| `GET /rooms/minigames` | `:room` `RoomRestController` | `:game` `MiniGameRestController` | 없음 |
| `GET /rooms/minigames/selected` | `:room` `RoomRestController` | `:game` `MiniGameRestController` | `Room` → `GameSession.getSelectedTypes()` |
| `GET /rooms/{joinCode}/miniGames/remaining` | `:room` `RoomRestController` | `:game` `MiniGameRestController` | `RemainingMiniGameResponse`도 `:game`으로 이전, `Playable` 의존 제거 |

URL 경로는 클라이언트 호환을 위해 유지한다.

## 최종 의존 구조

```text
변경 전:  :room → :game-api (게임 타입 + 게임 인스턴스 보유)
          :game → :room     (게임 조회를 Room 경유)

변경 후:  :room → :game-api (JoinCode·Gamer 식별자 + MiniGameFinishedEvent 리스너 참조)
          :game → :room     (게임 시작 시 RoomQueryService 플레이어 조회 등 최소 참조만 잔존)
```

- `:room`의 도메인 코드(`room.domain`)에서 `Playable`, `MiniGameType`, `MiniGameResult`, `MiniGameScore` import가 사라진다.
- `GameSession` 도메인은 `JoinCode`·`Gamer`·`Playable` 모두 `:game-api` 타입만 사용하므로 `:room` 의존이 없다.
- 게임 내부 도메인(`Runners`, `PlayerHands` 등)의 `room.domain.player.Player` import 21곳이 `Gamer`로 대체된다.
- 게임 결과 반영 경로에서 `:game` → `RoomCommandService` 호출 결합이 제거된다.
- 새 게임 종류를 추가·제거할 때 `:room` 코드를 건드리지 않는다 (OCP — CLAUDE.md 아키텍처 핵심 제약과 일치).

## 검토한 대안

**대안 A: 게임 결과를 `RoomCommandService.applyGameResult()` 직접 호출로 전달**

참조 브랜치가 택한 방식. `:game`이 게임 종료 시 `roomCommandService.applyGameResult(joinCode, rankMap)`를 동기 호출한다.

채택하지 않은 이유: `:game` → `:room` 서비스 결합이 결과 반영 경로에 남는다. `MiniGameFinishedEvent`가 이미 `:game-api`에 존재하고 in-process 리스너가 동기 실행되므로, 이벤트 방식(결정 4)이 동일한 순서·예외 보장을 제공하면서 결합만 제거한다. 비용 차이가 거의 없어 이벤트를 택한다.

**대안 B: 게임 결과를 Redis Stream 경유로 비동기 전달**

`MiniGameFinishedEvent`를 Redis Stream에 발행하고 `:room` Consumer가 수신해 확률을 조정한다.

채택하지 않은 이유: 확률 조정은 게임 종료 처리와 같은 시점에 완료돼야 한다. 비동기 전달 시 클라이언트가 스코어보드/룰렛 확률을 조회하는 시점에 조정이 끝나지 않았을 수 있다(eventual consistency). Redis Stream 경유는 클라이언트 커맨드 수신 흐름(Handler → Stream → Consumer)에 적용되는 규칙이며, 단일 인스턴스 내 도메인 상태 전이에는 in-process 동기 이벤트로 충분하다.

**대안 C: Room 분리 없이 서비스 계층만 재구성**

Room 엔티티를 그대로 두고 게임 서비스가 Room 대신 별도 캐시를 참조하도록 한다.

채택하지 않은 이유: 근본 원인(Room이 게임 컨테이너 역할 겸임)을 해소하지 못하고, Room 엔티티와 캐시 간 불일치 문제만 새로 만든다.

**대안 D: 참조 브랜치를 그대로 머지**

`be/refactor/game-entity-ownership`를 be/dev에 머지한다.

채택하지 않은 이유: 해당 브랜치는 멀티 모듈 전환 이전 시점(`e34ce51e`)에서 분기해 be/dev와 구조가 크게 다르다 (`:game-api` 부재, ADR 번호 충돌, Gamer 도입 이전 시그니처 혼재). 충돌 해소 비용이 재구현 비용보다 크고, Gamer 소유권 검증 등 이번 범위 밖 변경이 섞여 있다. 검증된 설계만 가져와 be/dev 위에 새로 구현한다.

## 핵심 제약

- `GameSession`은 `JoinCode`로 Room과 1:1 연결된다. Room 삭제 시 `RoomRemovedEvent`로 GameSession도 반드시 정리한다.
- `:room` 도메인 코드는 `Playable`, `MiniGameType`, `MiniGameResult`, `MiniGameScore`를 import하지 않는다. `:game-api` 참조는 `MiniGameFinishedEvent` 리스너 한 곳만 허용한다.
- 게임 결과 전달은 `MiniGameFinishedEvent`(`:game-api`) in-process 동기 리스너로 처리한다. 해당 리스너에 `@Async` 적용 금지 — `publishEvent()` 반환 시점에 확률 조정 완료가 보장돼야 룰렛/스코어보드 조회 타이밍이 깨지지 않는다.
- `:game`은 `RoomCommandService`를 직접 호출하지 않는다.
- `JoinCode`는 `:game-api` `gamecommon` 소속 불변 순수 값 객체로 유지한다. `QrCode` 등 방 입장 관심사를 다시 들이지 않는다.
- 게임 대기열 쓰기(`replaceGames`, `startNextGame`)는 GameSession의 호스트 검증과 상태 검증(`READY`에서만 변경 가능)을 모두 통과해야 한다.
- `:game`의 게임 도메인은 `room.domain.player.Player`를 import하지 않는다. 식별이 필요하면 `Gamer`를 사용한다.
- `Room.applyGameResult()`는 완료 게임 횟수를 내부 카운터로 유지하며, 이 값이 `ProbabilityCalculator`에 전달된다.

## 작업 계획

### Step 1 — `JoinCode` 슬림화 및 `:game-api` 이전

- `QrCode` 필드·`assignQrCode()`를 `JoinCode`에서 `Room`으로 이동
- `JoinCodeErrorCode` 신설 (`:game-api`), `RoomErrorCode`의 JOIN_CODE_* 항목 이전
- `JoinCode`를 `:game-api` `gamecommon`으로 이동, 전 모듈 import 경로 갱신

### Step 2 — `GameSession` 신설

- `GameSession` + `GameSessionStatus` + `GameSessionRepository` + `GameSessionErrorCode` (`:game` `minigame.domain`)
- `MemoryGameSessionRepository` (`:game` `minigame.infra`)
- `GameSessionService` (`:game` `minigame.application`)
- 단위 테스트 작성 — 호스트 검증·상태 전이(READY→PLAYING→READY/DONE)·`GAME_IN_PROGRESS` 차단 포함

### Step 3 — 게임 내부 `Player` → `Gamer` 전환

- `:game` 내 `room.domain.player.Player` import 21개 파일을 `Gamer` 기반으로 교체 (`BlindTimerPlayers`, `PlayerHands`, `Runners`, `Poles`, `SpeedTouchPlayer` 등)
- 게임별 점수·상태 보관 키를 `Gamer`로 통일

### Step 4 — 게임 서비스 조회 경로·결과 전달 변경

- 게임 서비스(`CardGameService` 등 6종)가 GameSession을 통해 게임 인스턴스를 조회하도록 변경
- `MiniGameSelectConsumer`를 `:game`으로 이동, `GameSessionService.updateGames()` 호출
- `MiniGameFinishedEvent`에 `ranks` 필드 추가, `MiniGameResult.toRankMap()` 신설
- `:room`에 `MiniGameResultRoomListener` 신설 — `RoomCommandService.applyGameResult()` 호출, 기존 6곳의 `room.applyMiniGameResult()` 직접 호출 제거
- 게임 종료 처리에서 `GameSession.finishCurrentGame()` 호출로 상태 복귀

### Step 5 — Room 슬림화

- `miniGames`/`finishedGames` 필드 및 게임 관련 메서드 제거
- `applyMiniGameResult` → `applyGameResult(Map<PlayerName, Integer>)` + `completedGameCount` 카운터
- `RoomService`/`RoomCommandService`에서 게임 관련 책임 제거

### Step 6 — 생명주기 이벤트 연결

- `RoomRemovedEvent` 신설, `DelayedRoomRemovalService`에서 발행
- `GameSessionInitConsumer`/`GameSessionCleanupListener` (`:game`)
- `EventDispatcher` 팬아웃 전환 + 기존 Consumer 회귀 테스트

### Step 7 — REST 엔드포인트 이전

- 미니게임 엔드포인트 3종을 `:game` `MiniGameRestController`로 통합
- `RemainingMiniGameResponse` 이전 및 `Playable` 의존 제거

### Step 8 — 검증

```bash
./gradlew :room:compileJava    # Playable/MiniGameType import 없어야 함
./gradlew :game:compileJava
./gradlew :room:test :game:test
./gradlew :app:test            # 통합 테스트 (Docker 필요)
```

- `:game`에서 `room.domain.player.Player` import 0건 확인
- ArchUnit으로 `:room` → 게임 타입 import 금지, `:game` → `Player` import 금지 규칙 추가 검토
