# 0009. WebSocket 인증 — Room Session Token 도입

- 날짜: 2026-05-08
- 상태: 승인

## 컨텍스트

STOMP CONNECT 시 클라이언트가 `joinCode`와 `playerName`을 헤더로 직접 전달하고, 서버는 이를 검증 없이 신뢰해 `joinCode:playerName` 형태의 Principal을 설정했다.
이 방식은 누구든 헤더를 조작하면 다른 플레이어를 사칭할 수 있다는 보안 허점을 가진다.

기존 JWT(Access Token)를 그대로 활용하면 이 문제를 해결할 수 없다.
ADR-0005에 따라 Access Token 클레임은 `userId`와 `userCode`만 담고, 방 컨텍스트(`joinCode`, `playerName`)를 포함하지 않는다.
또한 ADR-0004에 따라 Player와 User는 nullable FK로 분리되어 있어, 비로그인 익명 플레이어도 WebSocket에 연결해야 한다.

따라서 "방에 실제로 입장한 플레이어임을 증명하는 별도 토큰"이 필요했다.

## 결정

방 입장 시(`POST /api/rooms/{joinCode}/join`) 서버가 **Room Session Token(RST)** 을 발급한다.
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

| 변경 전                                       | 변경 후                    |
|--------------------------------------------|-------------------------|
| `joinCode: ABCD`                           | 제거                      |
| `playerName: 홍길동`                          | 제거                      |
| `Authorization: Bearer {accessToken}` (선택) | 유지 (선택)                 |
| —                                          | `roomToken: {RST}` (필수) |

`StompPrincipalInterceptor`는 CONNECT 수신 시 RST를 검증하고, 클레임에서 `joinCode:playerName` Principal을 설정한다.
로그인 사용자의 경우 `Authorization` 헤더가 함께 오면 Access Token도 검증하며, RST의 `userId`와 일치 여부를 확인한다.

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

- `room.removalDelay`(기본 1시간) 내 탈취된 RST는 서버 측 폐기가 불가능하다 (방이 닫히거나 플레이어가 나가면 이미 무효화되는 문제이므로 허용 범위로 판단).
- 클라이언트가 RST를 안전하게 저장하고 재접속 시 재사용해야 한다.
- 방 입장 응답 구조가 변경되어 프론트엔드 수정이 필요하다.

## 결과

### 백엔드 변경

- `POST /api/rooms/{joinCode}/join` 응답에 `roomSessionToken` 필드 추가.
- `RoomSessionTokenService`: RST 발급(`issue`) 및 검증(`verify`) 담당.
- `StompPrincipalInterceptor`: `joinCode`/`playerName` 헤더 파싱 제거 → `roomToken` 헤더 검증으로 교체. 검증 실패 시 연결 거부.
- `SessionConnectEventListener`: `joinCode`/`playerName` 헤더 직접 읽기 제거 → Principal에서 추출.
- `application.yml`에 `websocket.room-session-token.secret` 추가. TTL은 기존 `room.removalDelay`를 주입받아 사용.

### 프론트엔드 변경 (FE 팀 전달 필요)

#### 1. 방 입장 API 응답 변경

`POST /api/rooms/{joinCode}/join` 응답에 `roomSessionToken` 필드가 추가된다.

```json
{
  "playerName": "홍길동",
  "roomSessionToken": "eyJhbGciOiJIUzI1NiJ9..."
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
  Authorization: Bearer {accessToken}  ← 로그인 시만 포함
```

#### 3. 재접속(reconnect) 처리

재접속 시에도 동일한 `roomSessionToken`을 `roomToken` 헤더에 실어 CONNECT한다.
토큰 TTL은 `room.removalDelay`(기본 1시간)이므로 정상적인 게임 세션 내 재접속은 별도 갱신 없이 처리된다.
토큰이 만료된 경우 방 입장 API를 다시 호출해 새 토큰을 받아야 한다.

#### 4. 기존 헤더 제거 시점

백엔드 배포 후 즉시 기존 `joinCode`, `playerName` 헤더는 서버에서 무시된다.
배포 전 프론트엔드와 동시 릴리즈 또는 임시 하위 호환 기간을 조율해야 한다.
