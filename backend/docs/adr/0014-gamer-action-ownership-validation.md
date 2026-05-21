# 0014. 게임 액션 소유권 검증 — 도메인 메서드 파라미터 PlayerName → Gamer 전환

- 날짜: 2026-05-21
- 상태: 구현 완료

## 컨텍스트

ADR-0013 구현 기록에서 "2단계(미구현)"로 남긴 게임 액션 소유권 검증 문제다.

현재 모든 게임의 플레이어 액션 메서드가 `PlayerName`만으로 플레이어를 식별한다.

```java
// 현재 — 닉네임만으로 식별
cardGame.selectCard(new PlayerName(playerName), cardIndex);
racingGame.updateSpeed(new PlayerName(playerName), tapCount, calculator, now);
```

이로 인해 두 가지 취약점이 존재한다.

**1. 중복 닉네임 충돌**

방 입장 시 닉네임 중복을 차단하지만, 게임 도메인은 `PlayerName` 기준으로만 플레이어를 조회한다.
`stream().filter(name.equals(...))` 방식에서 중복 닉네임이 존재하면 첫 번째 매칭 플레이어에게 액션이 적용된다.
`Map<PlayerName, ...>` 구조(CardGame, BlockStackingGame)에서는 마지막 put이 덮어쓴다.

**2. 타인 플레이어 사칭**

현재 일부 WebSocket 컨트롤러(SpeedTouch, RacingGame, BlindTimer, CardGame)는
커맨드 페이로드의 `playerName` 필드를 그대로 사용한다.
악의적 클라이언트가 다른 플레이어의 닉네임으로 액션을 전송할 수 있다.

```java
// SpeedTouchGameWebSocketController — command.playerName()은 클라이언트 제어 값
final BaseEvent event = TouchProgressCommandEvent.create(
    joinCode, command.playerName(), command.touchedNumber()
);
```

ADR-0013에서 `Gamer(PlayerName name, Long userId)` 값 객체를 도입해 `setUp(List<Gamer>)`으로
게임이 userId를 보유하게 됐지만, 액션 메서드는 여전히 `PlayerName`만 받는다.
PlayerKey Principal에 userId가 포함(ADR-0013 1단계)됐으므로, 이제 검증 로직을 추가할 수 있다.

## 결정

### 1. 게임 도메인 액션 메서드: `PlayerName` → `Gamer`

6개 게임의 플레이어 액션 메서드 시그니처를 `Gamer`로 교체하고,
`setUp`에서 저장한 `gamers` 목록과 대조해 소유권을 검증한다.

```java
// 변경 전
public boolean selectCard(PlayerName playerName, Integer cardIndex)
public boolean touch(PlayerName playerName, int number, Instant now)
public void updateSpeed(PlayerName playerName, int tapCount, SpeedCalculator calc, Instant now)
public boolean stop(PlayerName playerName, Instant now)
public boolean recordProgress(PlayerName playerName, int floor, ...)
public boolean recordFailure(PlayerName playerName)
public LadderLine drawLine(PlayerName playerName, int segmentIndex)

// 변경 후
public boolean selectCard(Gamer gamer, Integer cardIndex)
public boolean touch(Gamer gamer, int number, Instant now)
public void updateSpeed(Gamer gamer, int tapCount, SpeedCalculator calc, Instant now)
public boolean stop(Gamer gamer, Instant now)
public boolean recordProgress(Gamer gamer, int floor, ...)
public boolean recordFailure(Gamer gamer)
public LadderLine drawLine(Gamer gamer, int segmentIndex)
```

### 2. `Gamer.validateAgainst(List<Gamer>)` — 소유권 검증 메서드

`Gamer`에 인스턴스 메서드를 추가한다.

```java
public void validateAgainst(List<Gamer> registeredGamers) {
    boolean valid = registeredGamers.stream()
        .anyMatch(g -> g.name().equals(this.name)
            && (!g.isLoggedIn() || Objects.equals(g.userId(), this.userId)));
    if (!valid) {
        throw new BusinessException(GamerErrorCode.UNAUTHORIZED_GAMER, ...);
    }
}
```

검증 규칙:

| 등록된 Gamer | 요청 Gamer | 결과 |
|---|---|---|
| `{name: "Alice", userId: 123}` | `{name: "Alice", userId: 123}` | ✅ PASS |
| `{name: "Alice", userId: 123}` | `{name: "Alice", userId: null}` | ❌ FAIL |
| `{name: "Alice", userId: 123}` | `{name: "Alice", userId: 456}` | ❌ FAIL |
| `{name: "Bob", userId: null}` | `{name: "Bob", userId: null}` | ✅ PASS |
| 없음 | 아무 Gamer | ❌ FAIL |

로그인 사용자(`isLoggedIn() == true`)는 userId까지 일치해야 통과한다.
비로그인 게스트는 닉네임 일치만으로 통과한다 (게스트는 userId가 없으므로 더 이상 검증 불가).

### 3. `userId`를 WebSocket 컨트롤러 → 도메인까지 전달

**컨트롤러**: 커맨드 페이로드의 `playerName` 대신 Principal에서 추출한 값을 사용한다.

```java
// 변경 전 — 클라이언트 입력값 사용
TouchProgressCommandEvent.create(joinCode, command.playerName(), command.touchedNumber())

// 변경 후 — Principal에서 추출 (인증된 값)
PlayerKey playerKey = PlayerKey.parse(principal.getName());
TouchProgressCommandEvent.create(joinCode, playerKey.playerName(), playerKey.userId(), command.touchedNumber())
```

**이벤트**: 모든 게임 커맨드 이벤트에 `Long userId` 필드 추가.

**컨슈머 → 서비스**: `Long userId` 파라미터 추가.

**서비스**: `Gamer` 생성 후 도메인 메서드 호출.

```java
final Gamer gamer = new Gamer(new PlayerName(playerName), userId);
cardGame.selectCard(gamer, cardIndex);
```

### 4. `MiniGameCommandHandler` 인터페이스에 `PlayerKey` 추가

CardGame의 `selectCard` 커맨드는 `MiniGameWebSocketController` → `MiniGameCommandDispatcher` →
`SelectCardCommandHandler`를 경유한다. Principal을 핸들러까지 전달하기 위해 인터페이스를 변경한다.

```java
// 변경 전
void handle(String joinCode, T command);

// 변경 후
void handle(String joinCode, T command, PlayerKey playerKey);
```

`StartMiniGameCommandHandler`는 `playerKey`를 사용하지 않지만 인터페이스 일관성을 위해 시그니처를 맞춘다.

## 변경 파일 목록

### 신규

- `game/minigame/domain/GamerErrorCode.java`

### 도메인 (7개)

- `Gamer.java` — `validateAgainst()` 추가
- `CardGame.java`, `SpeedTouchGame.java`, `RacingGame.java`, `BlindTimerGame.java`,
  `BlockStackingGame.java`, `LadderGame.java` — 액션 메서드 파라미터 변경

### 이벤트 (7개)

- `SelectCardCommandEvent`, `TouchProgressCommandEvent`, `TapCommandEvent`,
  `StopCommandEvent`, `LadderDrawCommandEvent`, `BlockStackingCommandEvent`,
  `BlockStackingFailEvent` — `Long userId` 필드 추가

### 컨슈머 (7개)

- 위 이벤트에 대응하는 Consumer 7개 — `event.userId()` 전달

### 서비스 / 핸들러 (8개)

- `CardGameService`, `SpeedTouchGameProgressHandler`, `RacingGameService`,
  `RacingGameFacade`, `BlindTimerGameProgressHandler`, `BlockStackingService`,
  `LadderService`, `LadderCommandService` — `Long userId` 파라미터 추가 및 `Gamer` 생성

### WebSocket 컨트롤러 / 디스패처 (10개)

- `MiniGameCommandHandler` 인터페이스, `MiniGameCommandDispatcher`,
  `MiniGameWebSocketController`, `SelectCardCommandHandler`, `StartMiniGameCommandHandler`
- `SpeedTouchGameWebSocketController`, `RacingGameWebSocketController`,
  `BlindTimerGameWebSocketController`, `BlockStackingWebSocketController`,
  `LadderWebSocketController`

## 검토한 대안

**대안 A: WebSocket 레이어에서만 차단 (게임 도메인 변경 없음)**

컨트롤러 또는 인터셉터에서 "커맨드의 playerName == Principal의 playerName"인지만 검사한다.

채택하지 않은 이유: 방 입장 시 닉네임 중복이 완전히 차단된다고 가정하면 단기적으로 동작하지만,
게임 도메인이 플레이어 식별에 userId를 전혀 활용하지 않는 구조적 문제가 남는다.
도메인 메서드 시그니처가 의도를 표현하지 못하며, 게임 결과를 userId 기준으로 추적할 수 없다.

**대안 B: 플레이어 컬렉션(PlayerHands, Runners 등)에 Gamer 도입**

`PlayerHands`, `SpeedTouchPlayers` 등 내부 컬렉션이 `PlayerName` 대신 `Gamer`를 보관한다.

채택하지 않은 이유: 컬렉션 클래스 변경 범위가 크고, 검증 로직이 컬렉션 레이어로 내려간다.
각 게임 도메인 클래스에서 `validateAgainst()` 한 번 호출로 검증하면 충분하며,
컬렉션은 플레이어 이름 기반 조회만 하면 된다.

## 핵심 제약

- 모든 게임 도메인 액션 메서드는 `PlayerName` 단독 파라미터를 허용하지 않는다. 반드시 `Gamer`를 받는다.
- 게임 도메인은 `Gamer.validateAgainst(this.gamers)`를 상태 검증 직후, 실제 플레이어 조회 전에 호출한다.
- 컨트롤러는 커맨드 페이로드의 `playerName` 필드를 플레이어 식별에 사용하지 않는다. 반드시 `Principal`에서 추출한다.
- 비로그인 게스트(userId == null)는 닉네임 일치만으로 검증 통과한다. userId 강제는 없다.

## 구현 기록 (2026-05-21)

### 도메인 레이어 (이전 세션 완료)

6개 게임 액션 메서드 시그니처 `PlayerName` → `Gamer` 전환 완료.
`Gamer.validateAgainst(List<Gamer>)` 구현. `GamerErrorCode.UNAUTHORIZED_GAMER` 신규.

### 이벤트·컨슈머·서비스·컨트롤러 레이어 (2026-05-21 완료)

**Gamer 생성 패턴** (서비스/핸들러 공통):

```java
final Gamer gamer = userId != null
        ? Gamer.loggedIn(new PlayerName(playerName), userId)
        : Gamer.guest(new PlayerName(playerName));
```

**컨트롤러 userId 추출 패턴** (Principal → PlayerKey):

```java
final PlayerKey playerKey = PlayerKey.parse(principal.getName());
// playerKey.playerName(), playerKey.userId() 사용
```

`StompPrincipalInterceptor`가 roomToken 검증 시 `PlayerKey.toString()`을 Principal 이름으로 등록하므로,
`PlayerKey.parse(principal.getName())`으로 서버 인증된 playerName + userId를 꺼낼 수 있다.

**MiniGameCommandHandler 인터페이스 시그니처 변경:**

```java
// 변경 전
void handle(String joinCode, T command);

// 변경 후
void handle(String joinCode, T command, PlayerKey playerKey);
```

`StartMiniGameCommandHandler`는 playerKey를 사용하지 않지만 인터페이스 일관성을 위해 시그니처를 맞춘다.
