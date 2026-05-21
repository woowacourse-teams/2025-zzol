# 0015. 닉네임 중복 허용 + 게임 컬렉션 식별 키 PlayerName → Gamer 전환

- 날짜: 2026-05-21
- 상태: 승인

## 컨텍스트

ADR-0014로 게임 액션 메서드 파라미터는 `Gamer`로 전환됐고, 컨트롤러도 Principal에서 userId를 추출한다.
그러나 게임 내부 컬렉션(PlayerHands, BlockStackingGame의 Progress 맵, SpeedTouchPlayers 등)은
여전히 `PlayerName`을 식별 키로 사용한다.

현재 Room은 로그인/비로그인 구분 없이 **모든 닉네임 중복을 차단**한다.
이 정책이 유지되는 한 PlayerName은 방 내 유일 식별자로 작동하지만, 다음 요구사항이 발생했다.

**새 요구사항**: 로그인 사용자는 userId로 식별 가능하므로 **닉네임 중복을 허용**한다.

닉네임 중복이 허용되면 `PlayerName`은 더 이상 게임 내 유일 식별자가 될 수 없다.
`Map<PlayerName, ...>` 구조에서 동일 닉네임의 두 플레이어가 들어오면 마지막 put이 덮어쓴다.

## 결정

### 1. Room 레이어 닉네임 중복 정책 변경

새 입장 규칙:

| 입장 조합                    | 허용 여부             |
|--------------------------|-------------------|
| 비로그인 ↔ 비로그인 동일 닉네임       | ❌ 불가              |
| 로그인 ↔ 로그인 동일 닉네임         | ✅ 허용 (userId로 식별) |
| 비로그인 선점 닉네임 ↔ 로그인 동일 닉네임 | ✅ 로그인 사용자 허용      |

**변경 위치:**

- `Players.hasDuplicateName()` → `hasDuplicateNameForGuest(PlayerName)` (비로그인에게만 적용)
- `Room.validatePlayerNameNotDuplicate()` → userId가 있는 로그인 사용자는 닉네임 검사 스킵

### 2. 게임 컬렉션 식별 키 PlayerName → Gamer 전환

닉네임 중복 허용 후 `PlayerName` 기반 컬렉션은 충돌 가능성이 생긴다.
모든 게임 내부 컬렉션의 키/조회 기준을 `Gamer`로 교체한다.

`Gamer` record의 기본 `equals()`/`hashCode()`는 `name + userId` 복합이므로
동일 닉네임·다른 userId 플레이어를 구분할 수 있다.

**영향 범위:**

| 클래스                      | 현재                                        | 변경 후                                 |
|--------------------------|-------------------------------------------|--------------------------------------|
| `PlayerHands` (CardGame) | `Map<PlayerName, CardHand>`               | `Map<Gamer, CardHand>`               |
| `BlockStackingGame`      | `ConcurrentHashMap<PlayerName, Progress>` | `ConcurrentHashMap<Gamer, Progress>` |
| `SpeedTouchPlayers`      | `List<SpeedTouchPlayer>` (PlayerName 조회)  | `List<SpeedTouchPlayer>` (Gamer 조회)  |
| `BlindTimerPlayers`      | PlayerName 기반 조회                          | Gamer 기반 조회                          |
| `Runners` (RacingGame)   | PlayerName 기반 조회                          | Gamer 기반 조회                          |
| `Poles` (LadderGame)     | `List<Pole>` (PlayerName 보관)              | Gamer 보관                             |

### 3. `Playable.getScores()` 반환 타입 변경

```java
// 변경 전
Map<PlayerName, MiniGameScore> getScores();

// 변경 후
Map<Gamer, MiniGameScore> getScores();
```

동일 닉네임·다른 userId 플레이어가 존재할 때 `Map<PlayerName, ...>`은 키 충돌로 점수가 소실된다.
키를 `Gamer`로 교체해 복합 식별을 보장한다.

### 4. 응답 DTO에 userCode 포함 (당초 userId → userCode로 변경)

프론트엔드에서 동명 플레이어를 구분 표시할 수 있도록 응답 DTO에 식별자를 추가한다.

당초 결정에서는 `userId(Long)`를 노출할 계획이었으나, DB 내부 키 노출을 피하고 클라이언트 식별에 더 적합한 `userCode(String)`로 교체하기로 변경했다. 비로그인 게스트는 `userCode`가 `null`이다.

**Room 레이어 (구현 완료):**

`Player`에 `String userCode` 필드를 추가하고, `PlayerResponse`와 `WinnerResponse`에서 `userCode`를 노출한다.

```java
// PlayerResponse
record PlayerResponse(String userCode, String playerName, PlayerType playerType, ...) {}

// WinnerResponse
record WinnerResponse(String playerName, Integer colorIndex, Integer randomAngle, String userCode) {}
```

**Game 레이어 (미구현 — 별도 작업 브랜치):**

게임별 점수/순위 응답 DTO에도 `userId` 대신 `userCode`를 포함한다.

```java
// 예시 — 게임별 구체적 DTO는 구현 시 확정
record PlayerScoreDto(String playerName, String userCode, int score) {}
```

## 변경 파일 목록

### Room 레이어

- `Players.java` — `hasDuplicateNameForGuest(PlayerName)` 추가, 기존 `hasDuplicateName()` 제거 또는 내부 전용으로 변경
- `Room.java` — 로그인 입장 시 닉네임 중복 검사 스킵 로직 추가

### 게임 도메인 (6개 게임 × 컬렉션 클래스)

- `PlayerHands.java`
- `BlockStackingGame.java` (또는 내부 Progress 맵)
- `SpeedTouchPlayers.java`
- `BlindTimerPlayers.java`
- `Runners.java`
- `Poles.java`

### Playable 인터페이스 + 6개 구현체

- `Playable.java` — `getScores()` 반환 타입 변경
- 6개 게임 구현체 — `getScores()` 구현 변경

### Room 레이어 응답 DTO (구현 완료)

- `Player.java` — `String userCode` 필드 추가
- `Winner.java` — `String userCode` 필드 추가
- `PlayerResponse.java` — `Long userId` → `String userCode` 교체
- `WinnerResponse.java` — `String userCode` 추가

### 게임 레이어 응답 DTO (미구현 — 별도 작업 브랜치)

- 게임별 점수/순위 응답 DTO — `userCode` 필드 추가 (nullable)

## 검토한 대안

**대안 A: 닉네임에 suffix 붙임 ("Alice#2")**

중복 입장 시 닉네임을 자동으로 변경한다.

채택하지 않은 이유: 도메인 로직에 표현 로직이 침투하고, 프론트엔드와 닉네임 계약이 깨진다.

**대안 B: `List<GamerScore>` 반환**

`getScores()`를 `List<GamerScore>` 형태로 변경한다.

채택하지 않은 이유: 인터페이스 변경 범위가 크고, 기존 `Map` 기반 처리 로직을 모두 수정해야 한다.
`Map<Gamer, MiniGameScore>` 유지가 더 적은 변경으로 동일한 효과를 낸다.

## 핵심 제약

- 비로그인 게스트끼리는 여전히 닉네임 중복이 불가하다.
- 게임 컬렉션의 PlayerName 기반 직접 조회는 허용하지 않는다. 반드시 Gamer 기반으로 조회한다.
- `getScores()` 반환 타입이 `Map<Gamer, MiniGameScore>`로 변경되므로 이를 소비하는 응답 DTO도 함께 수정한다.
- 응답 DTO에는 `userId(Long)` 대신 `userCode(String)`를 사용한다. DB 내부 키를 클라이언트에 노출하지 않는다.
- 비로그인 게스트는 `userCode`가 `null`이다.

## 구현 중 발견된 추가 이슈

### 1. `SessionConnectEventListener` — WebSocket 세션 키에서 userId 누락

**증상**: 같은 닉네임을 가진 두 플레이어가 WebSocket에 연결하면 두 번째 플레이어가 첫 번째 플레이어의 세션을 덮어써서 하나로 통합되는 현상.

**원인**: `SessionConnectEventListener.publishSessionRegisteredEvent()`에서 `PlayerKey.parse(principal)`로 userId를 올바르게 파싱했음에도, 세션 등록 이벤트 발행 시 userId를 버리고 `PlayerKey.of(joinCode, playerName)`(userId 없음)으로 키를 생성했다.

```java
// 수정 전 — userId 누락
final String playerKey = PlayerKey.of(joinCode, playerName).toString();
// "ABCD:꾹이" → 게스트와 로그인 사용자 키 동일 → 세션 덮어씀

// 수정 후 — parsed PlayerKey 그대로 사용
final String playerKeyStr = playerKey.toString();
// 게스트:       "ABCD:꾹이"
// 로그인 사용자: "ABCD:꾹이:100"
```

`StompPrincipalInterceptor`는 토큰에서 userId를 포함한 principal(`"ABCD:꾹이:100"`)을 이미 올바르게 설정하고 있었다. `SessionConnectEventListener`만 이를 무시하고 있었던 것.

**수정 파일**: `room/src/main/java/coffeeshout/room/infra/session/SessionConnectEventListener.java`

### 2. `toColorIndexMap()` — 동일 PlayerName 키 충돌 시 IllegalStateException

**증상**: 같은 닉네임의 두 플레이어가 방에 존재하는 상태에서 `toColorIndexMap()`이 호출되면 런타임 예외 발생.

**원인**: `Collectors.toUnmodifiableMap(keyMapper, valueMapper)`는 merge function이 없는 버전으로, 중복 키 발생 시 `IllegalStateException`을 던진다.

```java
public Map<PlayerName, Integer> toColorIndexMap() {
    return players.getPlayers().stream()
            .collect(Collectors.toUnmodifiableMap(Player::getName, Player::getColorIndex));
    // PlayerName("꾹이")가 두 번 등장하면 IllegalStateException
}
```

**상태**: 미수정. 이 메서드의 호출 경로 및 필요한 수정 방향(PlayerName → Gamer 키 전환 또는 merge function 추가)을 별도로 검토해야 한다.

## 작업 브랜치

`be/feat/duplicate-nickname-game-key`
