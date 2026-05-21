# 0009. WebSocket 인증 — Room Session Token 도입

- 날짜: 2026-05-08
- 상태: 승인 (2026-05-15 부분 개정 — 인증 분기 정책 확장 / 2026-05-21 부분 개정 — PlayerKey Principal에 userId 포함)

## 컨텍스트

STOMP CONNECT 시 클라이언트가 `joinCode`와 `playerName`을 헤더로 직접 전달하고, 서버는 이를 검증 없이 신뢰해 `joinCode:playerName` 형태의 Principal을 설정했다.
이 방식은 누구든 헤더를 조작하면 다른 플레이어를 사칭할 수 있다는 보안 허점을 가진다.

기존 JWT(Access Token)를 그대로 활용하면 이 문제를 해결할 수 없다.
ADR-0005에 따라 Access Token 클레임은 `userId`와 `userCode`만 담고, 방 컨텍스트(`joinCode`, `playerName`)를 포함하지 않는다.
또한 ADR-0004에 따라 Player와 User는 nullable FK로 분리되어 있어, 비로그인 익명 플레이어도 WebSocket에 연결해야 한다.

따라서 "방에 실제로 입장한 플레이어임을 증명하는 별도 토큰"이 필요했다.

## 결정

방 입장 시(`POST /api/rooms/{joinCode}`) 서버가 **Room Session Token(RST)** 을 발급한다.
RST는 HS256 서명 JWT이며, 클레임은 다음과 같다.

```json
{
  "joinCode": "ABCD",
  "playerName": "홍길동",
  "userId": 42,
  "exp": 1234567890
}
```

- `userId`는 익명 플레이어의 경우 `null`로 발급한다.
- TTL은 `room.removalDelay` 값을 그대로 따른다 (기본값 `1h`). 방이 제거되는 시점과 토큰 만료 시점을 일치시켜 별도 만료 설정 없이 일관성을 유지한다.
- 서명 키는 `websocket.room-session-token.secret`으로 별도 관리한다.

STOMP CONNECT 헤더 변경은 다음과 같다.

| 변경 전                                       | 변경 후                                   |
|--------------------------------------------|----------------------------------------|
| `joinCode: ABCD`                           | 제거                                     |
| `playerName: 홍길동`                          | 제거                                     |
| `Authorization: Bearer {accessToken}` (선택) | `roomToken` 부재 시 User Principal 인증에 사용 |
| —                                          | `roomToken: {RST}` (방 연결 시 필수)         |

`StompPrincipalInterceptor`는 CONNECT 수신 시 아래 순서로 인증을 처리한다.

```text
1. roomToken 헤더 존재
   → RST 검증
   → 로그인 사용자(userId 있음): PlayerKey("joinCode:playerName:userId") Principal 설정
   → 비로그인 사용자(userId null): PlayerKey("joinCode:playerName") Principal 설정

2. roomToken 헤더 없음 + Authorization: Bearer 헤더 존재
   → Access Token 검증 → UserPrincipal("user:{userId}") Principal 설정

3. 두 헤더 모두 없음 또는 토큰 검증 실패
   → sessionId를 Principal로 설정 (익명 연결)
```

**연결 타입 구분**

| 연결 타입      | 사용 헤더            | Principal 형식                 | 예시 사용처         |
|------------|------------------|------------------------------|----------------|
| 방 연결 (로그인) | `roomToken` (필수) | `joinCode:playerName:userId` | 게임 진행, 룸 이벤트   |
| 방 연결 (익명)  | `roomToken` (필수) | `joinCode:playerName`        | 게임 진행, 룸 이벤트   |
| 사용자 연결     | `Authorization`  | `user:{userId}`              | 친구 알림, 사용자 이벤트 |
| 익명 연결      | 없음               | `{sessionId}`                | 미인증 클라이언트      |

`PlayerKey.isValid(principalName)`은 두 포맷(`2-part`, `3-part`)을 모두 유효로 판정한다.
`PlayerKey.parse(principalName).userId()`로 userId를 꺼낼 수 있으며, 비로그인이면 `null`이다.

> **주의**: `roomToken`과 `Authorization`을 교차 검증하지 않는다. `roomToken`이 있으면 RST 클레임의 `userId`와 Access Token의 `userId` 일치 여부를 확인하지 않는다. RST 단독으로 방 연결을 승인하고, 사용자 연결은 Access Token 단독으로 승인한다.

## 고려한 대안

| 대안                               | 장점         | 단점                                |
|----------------------------------|------------|-----------------------------------|
| 현행 유지 (joinCode + playerName 헤더) | 변경 없음      | 사칭 가능, 인증 없음                      |
| Access Token(JWT)으로 대체           | 별도 토큰 불필요  | 방 컨텍스트 없음, 익명 플레이어 처리 불가          |
| DB/Redis 저장 세션 토큰                | 서버 측 폐기 가능 | 연결마다 DB/Redis 조회, stateless 이점 상실 |
| RST를 짧게(10분) + 갱신 엔드포인트          | 탈취 피해 최소화  | 재접속 시 갱신 흐름 추가 구현 필요              |

## 트레이드오프

**장점**

- 서버 서명 검증으로 사칭 불가 — 입장하지 않은 방의 토큰을 만들 수 없다.
- 익명·로그인 플레이어를 단일 흐름으로 처리한다.
- `joinCode:playerName` 식별 체계가 유지되므로 하위 레이어(Notifier, Consumer, EventListener 등) 변경이 없다.
- Stateless 검증 — WS CONNECT마다 Redis/DB 조회가 없다.

**단점**

- **RST 탈취 시 TTL 내 서버 측 폐기 불가**: RST는 Stateless JWT이므로 `room.removalDelay`(기본 1시간)가 만료되거나 방이 닫힐 때까지 서버에서 강제 무효화할 수 없다.

  탈취 시 공격 흐름 예시:

  1. 공격자가 플레이어 A의 RST를 획득한다.
  2. RST로 WebSocket CONNECT → `ABCD:홍길동` Principal로 인증된다.
  3. 같은 방에 A가 이미 접속 중이면 중복 세션이 형성되거나, 구현에 따라 A의 기존 세션이 강제 종료된다.
  4. TTL이 남아 있는 한 공격자는 A로 위장한 채 접속을 유지할 수 있다.

  **리스크 수용 근거**: 방 내에서 교환되는 정보는 게임 진행 데이터(카드 패, 선택 결과 등)이며 금융·개인정보가 아니다. 또한 방이 종료되면 RST도 함께 무효화되므로 탈취 피해가 게임 세션 단위로 한정된다. 이러한 이유로 현재 단계에서 허용 범위로 판단한다.

  선택적 완화책:

  - **중복 연결 탐지**: 동일 RST로 짧은 시간 내 다수 CONNECT 시도를 감지하면 이후 연결을 거부한다.
  - **기존 세션 강제 종료 정책**: 동일 `joinCode:playerName` Principal로 신규 CONNECT 시 기존 세션을 즉시 종료해 공격자와 피해자가 동시에 접속하는 상황을 방지한다.
  - **TTL 단축 + 갱신 API**: RST TTL을 수분 단위로 짧게 설정하고 `/api/rooms/{joinCode}/token/refresh` 엔드포인트로 재발급 흐름을 추가한다 (ADR 고려 대안 중 "RST를 짧게(10분) + 갱신 엔드포인트" 참조).
- 클라이언트가 RST를 안전하게 저장하고 재접속 시 재사용해야 한다.
- 방 입장 응답 구조가 변경되어 프론트엔드 수정이 필요하다.

## 결과

### 백엔드 변경

- `POST /api/rooms` (방 생성) 및 `POST /api/rooms/{joinCode}` (방 입장) 응답에 `roomSessionToken` 필드 추가. HOST는 방 생성 시 자동 입장하므로 생성 응답에도 RST가 포함된다.
- `RoomSessionTokenService`: RST 발급(`issue`) 및 검증(`verify`) 담당.
- `StompPrincipalInterceptor`: `joinCode`/`playerName` 헤더 파싱 제거 → `roomToken` → `Authorization` 순으로 인증 분기. 방 연결은 RST 필수, 사용자 연결은 Access Token, 이외에는 sessionId로 폴백.
- `SessionConnectEventListener`: `joinCode`/`playerName` 헤더 직접 읽기 제거 → Principal에서 추출.
- `application.yml`에 `websocket.room-session-token.secret` 추가. TTL은 기존 `room.removalDelay`를 주입받아 사용.

### 프론트엔드 변경 (FE 팀 전달 필요)

#### 1. 방 입장 API 응답 변경

`POST /api/rooms/{joinCode}` 응답에 `roomSessionToken` 필드가 추가된다.

```json
{
  "joinCode": "ABCD",
  "roomSessionToken": "<ROOM_SESSION_TOKEN_JWT>"
}
```

이 토큰은 해당 플레이어가 해당 방에 실제로 입장했음을 증명하는 서명 토큰이며, 유효 기간은 서버의 `room.removalDelay` 설정을 따른다 (기본 1시간).

#### 2. STOMP CONNECT 헤더 변경

기존 `joinCode`, `playerName` 헤더를 제거하고 `roomToken` 헤더를 추가한다.

```text
변경 전:
  joinCode: ABCD
  playerName: 홍길동
  Authorization: Bearer {accessToken}  ← 로그인 시만 포함

변경 후:
  roomToken: {roomSessionToken}        ← 필수
```

#### 3. 재접속(reconnect) 처리

재접속 시에도 동일한 `roomSessionToken`을 `roomToken` 헤더에 실어 CONNECT한다.
토큰 TTL은 `room.removalDelay`(기본 1시간)이므로 정상적인 게임 세션 내 재접속은 별도 갱신 없이 처리된다.
토큰이 만료된 경우 방 입장 API를 다시 호출해 새 토큰을 받아야 한다.

#### 4. 기존 헤더 제거 시점

백엔드 배포 후 즉시 기존 `joinCode`, `playerName` 헤더는 서버에서 무시된다.
배포 전 프론트엔드와 동시 릴리즈 또는 임시 하위 호환 기간을 조율해야 한다.

---

## 개정 이력

### 2026-05-15 — 인증 분기 정책 확장

**배경**: 친구 관리 기능 도입(`#1234`)으로 방 컨텍스트(`joinCode`)가 없는 사용자 연결이 필요해졌다.
친구 알림 등 사용자 수준 WebSocket 이벤트는 RST를 발급받을 방이 없으므로 기존 정책(RST 단일 인증)으로는 연결 불가.

**변경 내용**

- 초기 결정: `roomToken` 없으면 연결 거부, `Authorization` 헤더 무시
- 개정 후: `roomToken` 없으면 `Authorization` 헤더로 폴백, 둘 다 없거나 검증 실패 시 `sessionId`로 익명 연결

**보안 분석**

- 방 연결(RST 경로)의 보안 모델은 변경 없다. RST 검증은 그대로 단일 수단이며 `Authorization`과 교차 검증하지 않는다.
- 사용자 연결(Access Token 경로)은 기존 REST API와 동일한 JWT 검증을 재사용한다.
- sessionId 폴백은 방 수준 권한이 없는 연결에만 적용되므로 방 이벤트 수신은 불가하다.

**영향 범위**: `StompPrincipalInterceptor`, 친구 알림 구독 경로

### 2026-05-21 — PlayerKey Principal에 userId 포함

**배경**: `RoomSessionClaim`은 처음부터 `userId`를 포함했으나, `StompPrincipalInterceptor`에서
RST claim의 `userId`를 무시하고 Principal을 `joinCode:playerName`으로만 설정하고 있었다.
게임 레이어(`Gamer.userId`)에서 사용자 신원 검증이 필요해졌으므로 Principal에 userId를 포함시켰다.

**변경 내용**

- 로그인 사용자의 방 연결 Principal 포맷: `joinCode:playerName` → `joinCode:playerName:userId`
- `PlayerKey` 레코드에 `userId(Long, nullable)` 필드 추가. `toString()`/`parse()`/`isValid()`가 두 포맷을 모두 지원
- `PlayerKeyErrorCode.INVALID_PLAYER_KEY_FORMAT` 신설 — parse 실패 시 `BusinessException` 발행
- `StompPrincipalInterceptor`: `PlayerKey.of(claim.joinCode(), claim.playerName(), claim.userId())` 호출

**보안 분석**

- RST 검증 로직은 변경 없다. userId는 RST 클레임에서 추출하므로 클라이언트가 임의로 지정할 수 없다.
- `PlayerKey.isValid()`는 3번째 파트가 숫자(Long)인 경우에만 유효로 판정하므로 비정상 포맷 주입이 차단된다.

**영향 범위**: `PlayerKey`, `StompPrincipalInterceptor`, `SessionConnectEventListener`(parse 호환성 자동 유지)
