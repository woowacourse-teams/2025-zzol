# 0025. Room-GameSession 분리 — 게임 대기열 소유권을 `:game`으로 이관

- 날짜: 2026-06-05
- 상태: 승인 (구현 완료)
- 참조 구현: `be/refactor/game-entity-ownership` 브랜치 (`d7abc0ea`, `ddfee28b`, `53cf40cc`, `af0d1a70`) — 멀티 모듈 전환 이전 코드베이스에서 동일 설계를 구현·검증한 선행 작업

## 구현 중 개정 (2026-06-10)

구현 단계에서 잔여 `:game → :room` 의존을 더 제거하면서 결정 2·4·6이 다음과 같이 바뀌었다. 아래 개정이 본문 해당 결정의 세부 서술보다 우선한다.

1. **결정 4 — 게임 시작 흐름을 이벤트로 분리한다.** 원안은 `:game` 컨슈머가 `RoomQueryService.getByJoinCode`로 플레이어 명단을 조회하는 "최소 잔존 참조"를 유지했다. 개정안은 시작 커맨드(`StartMiniGameCommandEvent`, `:game-api`로 이전)를 **`:room`의 `MiniGameStartConsumer`가 먼저 소비**해 `validateStartable`·플레이어 명단 수집을 수행하고, **in-process 동기 이벤트 `GameStartReadyEvent`(`:game-api`)** 로 `:game`(`MiniGameEventService.onGameStartReady`)에 시작을 위임한다. 이로써 `MiniGameEventService`의 `Room`/`RoomQueryService` 의존이 사라진다.
   - **Redis Stream이 아니라 in-process 동기 이벤트를 쓰는 이유**: 스트림 리스너가 컨슈머 그룹을 쓰지 않아(각 인스턴스가 모든 메시지를 독립 소비) 중간 단계를 스트림으로 쪼개면 인스턴스 수만큼 중복 발행돼 대기열이 깨진다. in-process는 발행 인스턴스에서 한 번만 동기 실행되고, 예외가 발행 측으로 전파돼 순서·원자성을 보장하므로 결정 5의 `MiniGameFinishedEvent` 패턴과 대칭이다.
   - **두 PLAYING 전이의 원자성(중요).** 방 `markPlaying`은 `MiniGameStartConsumer`가 하지 않는다. `onGameStartReady`가 `startGame`으로 GameSession을 `PLAYING`으로 전이한 **직후**, 실패 가능 I/O(`miniGameService.start`·결과 영속 `@RedisLock`+`@Transactional`)보다 **먼저** in-process 동기 `GameSessionStartedEvent`(`:game-api`)를 발행하고, `:room`의 `RoomGameStartListener`가 이를 받아 `markPlaying` 한다. 이렇게 두 전이를 I/O 앞에 한 묶음으로 끝내야, 이후 I/O가 실패해도 GameSession·Room이 **모두 PLAYING으로 일관**되게 남는다. `markPlaying`을 I/O 뒤에 두면 `startGame` 성공 후 I/O 실패 시 GameSession=PLAYING / Room=READY로 찢어지고 재전송이 `GAME_IN_PROGRESS`로 막혀 복구 불가하다. `startGame` 자체가 실패하면 전이 이벤트가 발행되지 않아 둘 다 READY로 남고 재전송으로 복구된다.
2. **결정 2 / Step 4 — `updateGames` 지연 생성 폴백을 제거한다(Option B).** 미니게임 선택 호스트 검증을 `MiniGameSelectConsumer`가 `RoomQueryService`로 하던 것을 제거하고 **GameSession이 단독 수행**한다. 세션은 방 생성 시 `GameSessionInitConsumer`가 권위 있는 호스트로 사전 생성하므로(지연 생성 없음), `replaceGames`의 호스트 검증이 "select가 주장한 hostName == 권위 호스트 이름"을 보증한다(기존 Room 검증과 보안 등가). init보다 select가 먼저 도달하는 극히 짧은 창에서는 `getSession`이 예외를 던져 `EventDispatcher`가 격리·스킵하고 클라이언트 재전송으로 복구된다.
3. **결정 6 — 생명주기 이벤트를 `:game-api`로 이전하고 중립 네이밍한다.** `RoomCreateEvent`/`RoomRemovedEvent`(`:room.domain.event`)를 **`GameRoomCreatedEvent`/`GameRoomRemovedEvent`(`:game-api` `gamecommon`)** 로 이전한다. `BaseEvent`가 `:common`에 있어 의존 위반이 없고, `:game`이 `:room` 이벤트를 import하던 참조 2건이 사라진다(`:game → :game-api`만 남음). `:room`은 생산자로서, `:game`은 소비자로서 모두 `:game-api`만 본다.
4. **부수 — `PlayerHands`의 `room.domain.RoomErrorCode`를 공용 `GameErrorCode`(`:game-api` `gamecommon`)로 교체**한다. 게임 전반의 횡단 개념(플레이어 식별 실패 등)을 담는 공용 에러 코드를 신설한다.

> `MiniGamePersistenceService`의 `Room`/`RoomState`/`PlayerEntity` 의존(JPA FK 계열)은 본 개정 범위 밖이며, `MiniGameEntity`의 `RoomEntity` FK·`PlayerEntity` 영속 책임 분리는 별도 후속 작업으로 미룬다.

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

    // 선택 게임 총수(대기 + 진행 중 + 완료) — 확률 조정 분모(roundCount)로 사용
    public int roundCount();
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

- `addGame`/`removeGame` 개별 API는 만들지 않는다. WebSocket 프로토콜(`MiniGameSelectMessage`)이 변경 시마다 현재 선택 목록 전체를 전송하므로 호출 경로가 없다. `replaceGames`가 원자적으로 전체 교체하며 중복 게임 타입 선택과 개수 상한(최대 5개, 현재 `Room.addMiniGame`의 `miniGames.size() <= 5` 제약 계승)을 함께 검증한다.
- **상태 기반 변경 가능 검증**: 게임 중 여부를 Room 상태 조회 없이 GameSession 스스로 식별한다. `replaceGames`는 `READY`에서만 허용하고, `PLAYING` 중 호출은 `GAME_IN_PROGRESS`로 거부한다. `startNextGame`은 `READY`+대기열 존재 조건에서 `PLAYING`으로 전이하며, 게임 종료(`MiniGameFinishedEvent` 처리 시점)에 `finishCurrentGame()`으로 `READY` 또는 `DONE`으로 복귀한다. Room의 `RoomState`(방 화면 흐름: READY/PLAYING/SCORE_BOARD/ROULETTE/DONE)와 역할이 다르다 — `GameSessionStatus`는 대기열 불변식 보호 전용이다.
- 호스트 검증은 Room이 아닌 GameSession에서 수행한다. 생성 시 `host`(`Gamer`)를 고정하고 `replaceGames`/`startNextGame` 진입점에서 `validateHost()`로 확인한다. Room 조회 없이 권한 검증이 끝난다. `:room`의 `PlayerName`이 아닌 `:game-api`의 `Gamer`를 사용해 GameSession 도메인의 `:room` 의존을 없앤다.
- **호스트 동일성은 이름으로 검증한다.** 방은 닉네임 중복을 막으므로(`Room.validatePlayerNameNotDuplicate` → `DUPLICATE_PLAYER_NAME`, 409) 방 안에서 이름이 곧 유니크 식별자다. 기존 Room의 호스트 검증도 전부 `host.sameName(hostName)` 이름 기준이다. 따라서 `validateHost()`는 `Gamer.name()`을 비교하며 `Gamer` 객체 동등성에 의존하지 않는다. init host는 `userId` 없이 이름만으로 구성(`Gamer.guest(hostName)`)할 수 있어 `RoomCreateEvent` 변경이 필요 없다.
- ⚠️ **이 이름 기반 식별은 "방 내 닉네임 유니크" 불변식에 의존한다 — 이는 영구 보장이 아니다.** 현재는 복잡도 때문에 중복 닉네임을 차단만 하고 있으나 차후 리팩터로 허용될 예정이다. 허용 시 이름은 더 이상 방-유니크 식별자가 아니므로, GameSession의 호스트/플레이어 식별을 방-유니크 키로 이관해야 한다. 회원은 `UserCode`로 가능하지만 게스트는 `UserCode`가 없으므로 `PlayerKey`(ADR-0009, 방-유니크) 같은 토큰이나 합성 식별자가 필요하다. 이 이관은 후속 고려 사항의 외부 식별 ADR과 한 묶음으로 설계한다.
- 검증 실패는 `BusinessException` + `GameSessionErrorCode`로 처리한다.

| 코드                 | HTTP | 설명                  |
|--------------------|------|---------------------|
| `NOT_HOST`         | 403  | 호스트 외 게임 세션 조작 시도   |
| `DUPLICATE_GAME`   | 400  | 동일 게임 타입 중복 선택      |
| `TOO_MANY_GAMES`   | 400  | 선택 게임 수가 상한(5개) 초과  |
| `GAME_IN_PROGRESS` | 409  | 게임 진행 중 대기열 변경 시도   |
| `NO_PENDING_GAMES` | 409  | 대기 게임 없는 상태에서 시작 요청 |
| `GAME_NOT_FOUND`   | 404  | 완료된 게임 조회 실패        |

`GameSessionRepository` 인터페이스를 `minigame.domain`에 두고, `MemoryGameSessionRepository`(`ConcurrentHashMap` 기반)를 `minigame.infra`에 구현한다.

`GameSessionService`(application)는 다음 메서드로 구성한다.

| 메서드                                                        | 용도                                                                              |
|------------------------------------------------------------|---------------------------------------------------------------------------------|
| `initSession(JoinCode, Gamer)`                             | 방 생성 시 세션 사전 초기화 (이미 존재하면 무시)                                                   |
| `getSession(JoinCode)`                                     | 읽기 전용 조회 (세션 반드시 존재)                                                            |
| `findSession(JoinCode)`                                    | 세션 유무 불확실할 때 `Optional` 반환                                                      |
| `updateGames(MiniGameSelectEvent)`                         | `replaceGames` 위임                                                               |
| `startGame(JoinCode, Gamer requester, List<Gamer> gamers)` | `startNextGame` 위임 — `READY`+대기열 조건에서 `PLAYING` 전이, 시작 `Playable` 반환            |
| `finishGame(JoinCode)`                                     | `finishCurrentGame` 위임 — `PLAYING` → `READY`/`DONE`, 갱신된 라운드 수(`roundCount`) 반환 |
| `deleteSession(JoinCode)`                                  | 방 삭제 시 세션 정리                                                                    |

`startGame`의 `List<Gamer>`는 호출부(게임 시작 흐름의 `:game` 컨슈머)가 `RoomQueryService`에서 현재 플레이어를 조회해 전달한다 — 결정 4가 명시한 `:game` → `:room` 최소 잔존 참조다. `finishGame`이 반환하는 `roundCount`(= 대기 + 진행 중 + 완료 = 세션 선택 게임 총수)는 결정 5에서 `MiniGameFinishedEvent`에 실려 `:room`의 확률 조정에 사용된다.

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
// 변경 전 — MiniGameResult(게임 타입)와 게임 횟수를 Room이 직접 계산·수신
public void applyMiniGameResult(MiniGameResult miniGameResult)

// 변경 후 — 순위 맵 + 라운드 수만 수신, Room은 게임 타입·게임 수 상태 불필요
public void applyGameResult(Map<PlayerName, Integer> rankByPlayer, int roundCount)
```

- `ProbabilityCalculator`는 플레이어 수·`roundCount`·가중치만 필요하므로 `:room`에 유지한다.
- **`roundCount`는 "선택된 전체 게임 수(총 라운드 수)"이지 "완료 게임 수"가 아니다.** `ProbabilityCalculator`는 이 값을 조정폭의 분모(`초기확률 / roundCount`)로 사용하므로 누적 조정폭이 유계가 되려면 라운드마다 변하지 않는 전체 게임 수여야 한다. 기존 `Room.calculateMiniGameCount()`(`miniGames.size() + finishedGames.size()`)가 바로 이 값이었다. 완료 수(1→2→3 증가)를 넣으면 1라운드에서 `/1`로 과조정되어 확률 수학이 깨진다.
- **게임 수 상태는 Room이 보유하지 않고 GameSession이 소유한다.** GameSession이 `roundCount()`(= 대기 + 진행 중 + 완료)를 제공하고, 게임 종료 시 결정 5의 `MiniGameFinishedEvent`에 실어 전달한다. Room은 어떤 게임 카운터도 유지하지 않으므로 게임 관련 책임이 완전히 사라진다.
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

**게임 내부 값도 `Player` 대신 `Gamer`를 사용한다.** 현재 `Playable` 경계 시그니처는 `Gamer`이지만, 게임 내부 도메인 파일들(`BlindTimerPlayers`, `PlayerHands`, `Runners`, `Poles`, `SpeedTouchPlayer` 등)이 여전히 `room.domain.player.Player`를 직접 import한다. 게임이 필요한 것은 식별(이름·userId)뿐이고 `Player`의 확률·준비 상태는 게임이 알 필요도, 변경할 권한도 없다. 내부 컬렉션·점수 보관 키를 `Gamer`(`:game-api`)로 전면 교체해 `:game` → `:room` 직접 의존을 줄인다. `setUp(List<Gamer>)`로 받은 객체를 그대로 흘려보내면 되므로 경계 변환 비용도 사라진다.

**색상(`colorIndex`)은 `Gamer`가 함께 운반한다.** 게임은 식별 외에 색상도 화면 렌더링에 쓴다(카드 소유자·사다리 막대 색). 색상은 Room이 입장 시 부여하는 표시 상태인데, 게임 내부를 `Gamer`로 바꾸면 응답 DTO가 더 이상 `Player.getColorIndex()`를 읽을 수 없다. 이를 응답 조립마다 Room에서 재조회하는 대신 `Gamer`에 `colorIndex`를 실어 게임이 이미 흘려보내는 객체로 일관되게 처리한다. 단 **식별 동등성은 `name`+`userId`만으로 정의**하고 `colorIndex`는 `equals`/`hashCode`에서 제외한다 — 색상은 표시 상태일 뿐 식별자가 아니므로, 색상 유무와 무관하게 동일 식별의 `Gamer`가 score 맵 키로 일관되게 매칭돼야 한다(색상 없는 `Gamer.guest(host)`로 조회해도 색상을 가진 키와 매칭). 이 '전체 필드 동등성'과의 불일치를 표면화하기 위해 `Gamer`는 record가 아닌 불변 class로 둔다. 색상은 `Player.toGamer()`가 채우며, 색상이 불필요한 지점(호스트 초기화 `Gamer.guest`)은 null을 허용한다.

### 5. 게임 결과 전달 — `:game-api` 이벤트 경유 (in-process 동기)

게임 종료 시 `:game`이 `RoomCommandService.applyGameResult()`를 직접 호출하지 않고, `:game-api`의 이벤트를 발행해 `:room`이 수신하는 방식으로 역전한다. 참조 브랜치는 직접 호출 방식이었으나, 본 ADR에서는 모듈 결합 제거를 위해 이벤트 방식을 채택한다.

기존 `MiniGameFinishedEvent`(`:game-api` `minigame.event.dto`, 6개 게임 종료 지점에서 이미 발행 중)에 순위 맵과 라운드 수를 추가한다.

```java
// :game-api — 기존 이벤트에 ranks·roundCount 필드 추가 (원시 타입만 사용)
public record MiniGameFinishedEvent(
        String eventId,
        String joinCode,
        String miniGameType,
        Map<String, Integer> ranks,   // playerName 값 → 순위
        int roundCount                // 세션 선택 게임 총수 (확률 조정 분모, 결정 3 참조)
) { ... }
```

순위 맵 변환(`MiniGameResult` → `Map<String, Integer>`)은 `:game-api`의 `MiniGameResult.toRankMap()`으로 제공해 6개 게임의 중복 변환을 막는다.

**발행 순서가 불변식이다.** 게임 종료 지점은 (1) `finishGame()`으로 상태를 먼저 전이(`PLAYING` → `READY`/`DONE`)하면서 `roundCount`를 확정한 뒤, (2) 그 `roundCount`를 실어 이벤트를 발행한다. 이 순서여야 수신 측 확률 조정이 정확한 라운드 수로 수행되고, 동기 리스너가 반환된 시점엔 이미 `READY`로 복귀해 있어 직후 클라이언트의 다음 게임 선택(`replaceGames`)이 허용된다.

수신 측은 리스너 2개가 같은 이벤트를 각자 처리한다.

```text
게임 종료 (예: CardGameStep.FINISH_GAME)
  → int roundCount = gameSessionService.finishGame(joinCode)   // ① 상태 전이 + 라운드 수 확정
  → eventPublisher.publishEvent(                               // ② 확정된 값으로 발행
        new MiniGameFinishedEvent(joinCode, type, result.toRankMap(), roundCount))
      ├─ MiniGameResultSaveEventListener (:game, 기존)  → 결과 DB 저장
      └─ MiniGameResultRoomListener     (:room, 신규)  → RoomCommandService.applyGameResult(rankMap, roundCount)
                                                          → 확률 조정 + 룰렛 이벤트 발행
```

`MiniGameResultRoomListener`가 이벤트의 `Map<String, Integer>`를 `Map<PlayerName, Integer>`로 변환한다(`PlayerName`은 `:room` 타입이므로 변환은 `:room` 경계에서 일어난다).

**동기 실행이 전제 조건이다.** 현재 코드베이스에는 `@EnableAsync`·`@Async`·커스텀 `ApplicationEventMulticaster`가 없어 Spring 이벤트 리스너는 발행자 스레드에서 동기 실행된다. 따라서 `publishEvent()` 반환 시점에 확률 조정이 완료되므로, 직접 호출과 동일한 순서·예외 전파를 보장하면서 모듈 결합만 제거된다. Redis Stream을 경유하지 않는 이유는 검토한 대안 B를 참조한다.

### 6. GameSession 생명주기 자동 관리 (이벤트 기반)

GameSession 생성·정리를 서비스 호출 누락에 의존하지 않도록 Room 생명주기 이벤트에 연결한다.

```text
RoomLifecycleEvent.Created (:game-api) → GameSessionInitConsumer    (:game) → initSession()
RoomLifecycleEvent.Removed (:game-api) → GameSessionCleanupConsumer (:game) → deleteSession()
```

- **생성·정리 모두 Redis Stream Consumer로 통일한다.** 두 생명주기 이벤트 모두 `EventDispatcher`를 경유하는 `Consumer`로 처리하며, in-process 리스너를 섞지 않는다. `GameSession`은 `MemoryGameSessionRepository`(인스턴스 로컬 `ConcurrentHashMap`)에 저장되므로, 정리 이벤트도 생성과 동일한 Stream 경로를 타야 세션을 소유한 인스턴스에 일관되게 도달한다(in-process 이벤트는 발행 인스턴스에만 전달되어 불일치 위험).
- `RoomLifecycleEvent.Removed`를 `:game-api` `gamecommon`에 신설하고, `DelayedRoomRemovalService`가 방 삭제 완료 후 Redis Stream으로 발행한다(`StreamPublisher` 경유, 기존 이벤트와 동일). (개정: 원안은 `room.domain.event.RoomRemovedEvent`였고, 이후 `GameRoomRemovedEvent` 단독 클래스였다 — 현재는 `RoomLifecycleEvent` sealed 패밀리의 중첩 record. 상단 「구현 중 개정」 참조.)
- `GameSessionInitConsumer`는 `RoomLifecycleEvent.Created`의 `hostName`만으로 host를 구성한다(`initSession(joinCode, Gamer.guest(hostName))`). 호스트 검증이 이름 기준이므로(결정 2) `userId`는 불필요해 이벤트에 `userId` 필드가 없다.
- 현재 `EventDispatcher`(`:infra`)는 이벤트 타입당 Consumer 1개만 지원한다(미등록 시 warn 후 스킵). `RoomLifecycleEvent.Created`를 기존 `RoomCreateConsumer`와 신규 `GameSessionInitConsumer`가 함께 처리해야 하므로, `ObjectProvider.stream()`으로 동일 타입 Consumer 전체를 수집해 순차 실행하는 **팬아웃 방식으로 변경**한다. `RoomLifecycleEvent.Removed`는 현재 Consumer가 1개(`GameSessionCleanupConsumer`)뿐이지만 동일 팬아웃 경로를 그대로 사용한다.

### 7. 미니게임 REST 엔드포인트 `:game` 이전

| 엔드포인트                                       | 현재 위치                        | 이전 위치                            | 변경 사항                                                       |
|---------------------------------------------|------------------------------|----------------------------------|-------------------------------------------------------------|
| `GET /rooms/minigames`                      | `:room` `RoomRestController` | `:game` `MiniGameRestController` | 없음                                                          |
| `GET /rooms/minigames/selected`             | `:room` `RoomRestController` | `:game` `MiniGameRestController` | `Room` → `GameSession.getSelectedTypes()`                   |
| `GET /rooms/{joinCode}/miniGames/remaining` | `:room` `RoomRestController` | `:game` `MiniGameRestController` | `RemainingMiniGameResponse`도 `:game`으로 이전, `Playable` 의존 제거 |

URL 경로는 클라이언트 호환을 위해 유지한다.

## 최종 의존 구조

```text
변경 전:  :room → :game-api (게임 타입 + 게임 인스턴스 보유)
          :game → :room     (게임 조회를 Room 경유)

변경 후:  :room → :game-api (JoinCode·Gamer·MiniGameResultType 값/식별자 + 게임 이벤트 in-process 리스너 3종)
          :game → :room     (MiniGamePersistenceService의 PlayerEntity 영속 참조만 잔존 — 결정 4 개정으로 RoomQueryService 조회는 제거됨)
```

- `:room`의 도메인 코드(`room.domain`)에서 `Playable`, `MiniGameType`, `MiniGameResult`, `MiniGameScore` import가 사라진다. 단 `room.domain.roulette`(`ProbabilityCalculator`·`Probability`)는 순위→승패 분류를 위해 `:game-api`의 값 enum `MiniGameResultType`을 참조한다 — 게임 인스턴스·타입이 아닌 결과 분류 값이므로 소유권 분리에 위배되지 않는다.
- `GameSession` 도메인은 `JoinCode`·`Gamer`·`Playable` 모두 `:game-api` 타입만 사용하므로 `:room` 의존이 없다.
- 게임 내부 도메인(`Runners`, `PlayerHands` 등)의 `room.domain.player.Player` import 21곳이 `Gamer`로 대체된다.
- 게임 결과 반영 경로에서 `:game` → `RoomCommandService` 호출 결합이 제거된다.
- 새 게임 종류를 추가·제거할 때 `:room` 코드를 건드리지 않는다 (OCP — CLAUDE.md 아키텍처 핵심 제약과 일치).

## 검토한 대안

**대안 A: 게임 결과를 `RoomCommandService.applyGameResult()` 직접 호출로 전달**

참조 브랜치가 택한 방식. `:game`이 게임 종료 시 `roomCommandService.applyGameResult(joinCode, rankMap)`를 동기 호출한다.

채택하지 않은 이유: `:game` → `:room` 서비스 결합이 결과 반영 경로에 남는다. `MiniGameFinishedEvent`가 이미 `:game-api`에 존재하고 in-process 리스너가 동기 실행되므로, 이벤트 방식(결정 5)이 동일한 순서·예외 보장을 제공하면서 결합만 제거한다. 비용 차이가 거의 없어 이벤트를 택한다.

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

- `GameSession`은 `JoinCode`로 Room과 1:1 연결된다. Room 삭제 시 `GameRoomRemovedEvent`(`:game-api`) → `GameSessionCleanupConsumer`(Redis Stream)로 GameSession도 반드시 정리한다. 생성·정리 모두 Stream Consumer로 통일하고 in-process 리스너를 섞지 않는다.
- `:room` 도메인 코드는 `Playable`, `MiniGameType`, `MiniGameResult`, `MiniGameScore`를 import하지 않는다(결과 분류 값 enum `MiniGameResultType`은 `room.domain.roulette`에서 참조 허용 — 게임 인스턴스가 아닌 값). `:room` 애플리케이션의 `:game-api` 이벤트 in-process 리스너는 3종(`MiniGameResultRoomListener` ← `MiniGameFinishedEvent`, `RoomGameStartListener` ← `GameSessionStartedEvent`, `PlayerSnapshotListener` ← `PlayerSnapshotRequiredEvent`)이며 ArchUnit이 이 목록으로 동결한다(결정 4 개정으로 `RoomGameStartListener`가, PlayerEntity 영속 분리 후속 작업으로 `PlayerSnapshotListener`가 추가됐다).
- 게임 결과 전달은 `MiniGameFinishedEvent`(`:game-api`) in-process 동기 리스너로 처리한다. 해당 리스너에 `@Async` 적용 금지 — `publishEvent()` 반환 시점에 확률 조정 완료가 보장돼야 룰렛/스코어보드 조회 타이밍이 깨지지 않는다.
- `:game`은 `RoomCommandService`를 직접 호출하지 않는다.
- `JoinCode`는 `:game-api` `gamecommon` 소속 불변 순수 값 객체로 유지한다. `QrCode` 등 방 입장 관심사를 다시 들이지 않는다.
- 게임 대기열 쓰기(`replaceGames`, `startNextGame`)는 GameSession의 호스트 검증과 상태 검증(`READY`에서만 변경 가능), 개수 상한(5개)을 모두 통과해야 한다.
- 호스트 검증은 이름 기준(`Gamer.name()` 비교)이며 기존 `sameName`과 일치한다. ⚠️ 이 검증은 "방 내 닉네임 유니크" 불변식에 의존하고, 해당 불변식(중복 닉네임 차단)은 차후 리팩터 대상이다. 중복 허용 시 GameSession 식별을 방-유니크 키(회원 `UserCode`, 게스트 `PlayerKey`/합성 식별자)로 이관해야 한다 — 후속 고려 사항 참조.
- `:game`의 게임 도메인은 `room.domain.player.Player`를 import하지 않는다. 식별이 필요하면 `Gamer`를 사용한다.
- `Gamer`는 식별(`name`+`userId`)과 표시 상태(`colorIndex`)를 함께 갖는 불변 class이되, 동등성은 식별만으로 정의한다(`colorIndex`는 `equals`/`hashCode` 제외). 색상은 `Player.toGamer()`가 채우며, 게임 응답이 색상을 표시할 때 Room을 재조회하지 않고 `Gamer`에서 읽는다. ⚠️ 색상이 `Gamer` 페이로드로 흐르므로 ADR-0024의 외부 노출 점검 대상에 `colorIndex`도 포함한다.
- 게임 수 상태는 `GameSession`이 단독 소유한다. Room은 게임 카운터를 보유하지 않으며, `ProbabilityCalculator`에 넘기는 `roundCount`(선택 게임 총수, 완료 수 아님)는 `MiniGameFinishedEvent`로 전달받는다. 게임 종료 시 `finishGame()`으로 `roundCount`를 확정한 뒤 이벤트를 발행하는 순서를 지킨다.

## 후속 고려 사항 (범위 밖)

### 사용자 식별 통합 — 중복 닉네임 허용 + `name#UserCode` (별도 ADR)

본 ADR은 방 내부 식별을 **이름**으로 처리한다(현재 닉네임 유니크 불변식). `userId`도 `UserCode`도 GameSession 로직에 들이지 않는다. 그러나 서로 맞물린 두 변경이 예정돼 있어, 한 묶음으로 다룰 별도 ADR(**ADR-0024**)로 분리한다.

1. **중복 닉네임 허용** — 현재는 복잡도 때문에 방 입장 시 중복 닉네임을 차단(`DUPLICATE_PLAYER_NAME`)하지만 차후 리팩터로 허용 예정이다. 허용되면 이름이 더는 방-유니크 식별자가 아니므로, GameSession의 이름 기반 호스트/플레이어 식별을 방-유니크 키로 이관해야 한다.
2. **전역 식별자 `UserCode`** — 프론트엔드는 전역 범위에서 닉네임이 중복되므로 `name#UserCode`(ADR-0006, 5자리 불변)로 사용자를 식별하기로 확정했다. 내부 PK인 `userId`는 외부에 노출하지 않는다.

별도 ADR 설계 시 핵심 제약:

- **외부로 나가는 신원 표현은 `UserCode`로 통일**한다(`userId` 페이로드 노출 금지). `Gamer`가 `:game-api` 이벤트·WebSocket 페이로드로 흐르는 지점을 점검해 `userId` 누출을 막는다.
- **게스트는 `UserCode`가 없다**(가입 시 발급, ADR-0006). 따라서 중복 닉네임 허용 시 식별은 회원=`UserCode`, 게스트=방-유니크 토큰(`PlayerKey`(ADR-0009) 또는 합성 식별자)의 이원 구조가 된다. 이 둘을 어떤 단일 추상으로 묶어 `Gamer`/페이로드에 실을지가 그 ADR의 핵심 질문이다.

본 ADR은 위 변경 전까지 유효한 이름 기반 식별로 진행하며, 두 변경이 GameSession 식별 모델에 직접 영향을 준다는 점을 결정 2·핵심 제약에 ⚠️로 표시해 둔다.

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

- `:game` 내 `room.domain.player.Player`/`PlayerName` import 파일을 `Gamer` 기반으로 교체 (`BlindTimerPlayers`, `PlayerHands`, `Runners`, `Poles`, `SpeedTouchPlayer` 등)
- 게임별 점수·상태 보관 키를 `Gamer`로 통일
- `Gamer`에 `colorIndex` 추가(불변 class, 식별 동등성은 `name`+`userId`만) — 색상을 읽던 응답 DTO(`MiniGameStateMessage`, `LadderStateResponse`/`PoleInfo`)는 `Gamer.colorIndex()`에서 읽도록 변경
- `MiniGameResultSaveEventListener`는 게임 내부가 아니라 Room의 player 목록(`room.getPlayers()`)과 `room.findMiniGame()`을 쓰므로 **Step 4로 연기**한다(`room.findMiniGame()` → GameSession 조회 전환과 함께 정리). 따라서 Step 3 종료 시점에 `:game`의 `Player` import는 이 한 파일만 남고, `grep = 0`은 Step 4 완료 시 달성한다

### Step 4 — 게임 서비스 조회 경로·결과 전달 변경

- 게임 서비스(`CardGameService` 등 6종)가 GameSession을 통해 게임 인스턴스를 조회하도록 변경
- 게임 시작 흐름이 `GameSessionService.startGame()`을 호출하도록 변경(플레이어 목록은 `RoomQueryService`에서 조회해 전달)
- `MiniGameSelectConsumer`를 `:game`으로 이동, `GameSessionService.updateGames()` 호출
- `MiniGameFinishedEvent`에 `ranks`·`roundCount` 필드 추가, `MiniGameResult.toRankMap()` 신설
- 게임 종료 처리에서 `GameSessionService.finishGame()`을 **먼저** 호출해 `roundCount` 확정·상태 복귀 후 이벤트 발행(순서 불변식)
- `:room`에 `MiniGameResultRoomListener` 신설 — `RoomCommandService.applyGameResult(rankMap, roundCount)` 호출, 기존 6곳의 `room.applyMiniGameResult()` 직접 호출 제거

### Step 5 — Room 슬림화

- `miniGames`/`finishedGames` 필드 및 게임 관련 메서드 제거(`calculateMiniGameCount()` 포함)
- `applyMiniGameResult(MiniGameResult)` → `applyGameResult(Map<PlayerName, Integer>, int roundCount)` — Room은 게임 카운터를 보유하지 않음
- `RoomService`/`RoomCommandService`에서 게임 관련 책임 제거
- **전환기 이중 쓰기 해제** — `MiniGameSelectConsumer`(`:game`)의 `roomService.updateMiniGames(event)` 호출 제거, `gameSessionService.updateGames(event)` 단일 경로만 남긴다. 이에 따라 `RoomService.updateMiniGames(MiniGameSelectEvent)`·`RoomCommandService.updateMiniGames(...)`를 제거한다(이 둘이 `:room`의 마지막 `miniGameFactoryMap` 소비처)
- **`MiniGameFactoryConfig`를 `:app` → `:game`으로 이관** — 위 소비처 제거로 `:room`이 더는 `miniGameFactoryMap`을 주입받지 않으면, 맵 소비처는 `:game`의 `GameSessionService` 단 하나가 된다. `:game`은 자기 팩토리 빈을 전부 보므로 조립을 composition root에 둘 이유가 사라진다. 별도 `@Bean`/`@Configuration` 없이 `GameSessionService`가 생성자에서 `List<MiniGameFactory>`를 주입받아 `EnumMap<MiniGameType, MiniGameFactory>`로 조립하고(B안), `:app`의 `MiniGameFactoryConfig`는 삭제한다. `:app`이 `coffeeshout.minigame.domain.MiniGameType`을 import하던 게임 도메인 설정 누수가 해소된다(ADR-0014에서 `:app`에 두기로 한 사유 — `:room` 소비처 — 가 사라지므로 위치 변경 타당)

### Step 6 — 생명주기 이벤트 연결

- `RoomRemovedEvent` 신설, `DelayedRoomRemovalService`에서 Redis Stream 발행 + Stream 구독 등록
- `GameSessionInitConsumer`/`GameSessionCleanupConsumer` (`:game`)
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

## 구현 중 발견한 모델 정리 후보 (범위 밖)

ADR-0025 구현 중 `GameSession` 내부에서 관찰된 모델링 흠이다. 본 ADR의 결정(소유권 이관)과 무관하며 본 작업에서는 손대지 않는다. 별도 리팩터로 다루기 위해 기록만 남긴다.

`Playable`을 별도 Repository로 분리하지 않고 `GameSession` 애그리거트 안에 두는 현행 구조는 유효하다(생명주기·일관성 경계를 세션과 완전히 공유). 분리는 오케스트레이션 상태(`Queue` + `status` + `roundCount`)에 durable/분산 저장이 필요하고 라이브 게임 상태는 휘발성으로 둬도 되는 시점에 재검토한다.

### 1. `completedGames`가 진행 중 게임을 담는다

`GameSession.startNextGame()`이 폴링한 게임을 곧바로 `completedGames`에 넣으므로, `status == PLAYING`인 동안 현재 플레이 중인 게임이 "완료" 목록에 존재한다. 그 결과 `findCompletedGame(type)`이 진행 중 게임을 반환하며, 컬렉션 이름이 모델을 속인다. 라이브 인스턴스 보관 슬롯과 완료 이력을 한 컬렉션에 합친 흔적이다. 라이브 슬롯 분리 또는 명명 정리(예: `currentGame` 슬롯 도입)로 다룰 수 있다.

### 2. 선택 시점 eager 인스턴스 생성

`GameSessionService.updateGames()`가 선택된 모든 타입의 `Playable`을 선택 시점에 즉시 생성하고, 일부 게임은 생성자에서 초기 상태까지 만든다(예: `CardGame` 생성자가 덱 생성). 한 번에 하나만 플레이하지만 선택된 게임 전부를 미리 만든다. 인메모리·단일 소비자라 실질 영향은 작으나, 모델상 대기열이 "계획(타입)"이 아니라 "완성된 인스턴스"를 들고 있다. 대기열을 `Queue<MiniGameType>`으로 두고 `startNextGame` 시점에 팩토리로 생성하는 방식이 대안이다.
