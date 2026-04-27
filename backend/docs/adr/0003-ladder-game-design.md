# 0003. LadderGame 게임 서버 설계

- 날짜: 2026-04-26
- 상태: 승인

## 컨텍스트

LadderGame은 플레이어들이 DRAWING 구간(5초) 동안 인접한 기둥 사이를 터치하여 가로선을 만들고,
RESULT 구간에서 각자의 기둥이 선을 따라 내려가며 바닥의 순위가 결정되는 미니게임이다.

다음 설계 결정이 필요했다.

- 선의 수직 위치(row)를 어떻게 결정할 것인가
- DRAWING 중 다른 플레이어에게 선을 어떻게 공개할 것인가
- 결과 계산을 서버와 클라이언트 중 어디서 담당할 것인가
- FlowOrchestrator 구조를 어떻게 적용할 것인가

## 결정

### 1. 선 배치 알고리즘 — 삽입 순서 기반 row 할당 + 인접 충돌 해소

서버가 선을 수신한 순서가 수직 위치(row)를 결정한다. 배치 시 두 가지 제약을 순서대로 적용한다.

**규칙 1 — 같은 구간 내 중복 시 아래 배치**

segmentIndex S에 이미 선이 있으면 새 선의 초기 후보 row = S에서 가장 큰 row + 1이다.

**규칙 2 — 인접 구간 동일 row 금지**

후보 row에 인접 구간(S-1, S+1)의 선이 존재하면 row를 1씩 증가시키며 충돌이 없을 때까지 반복한다.

```text
add(playerName, segmentIndex S):
  candidate = max row in segment S + 1  (기본값 1)
  while exists line at (S-1 or S+1) with row == candidate:
    candidate++
  insert LadderLine(playerName, S, candidate)
```

이 알고리즘으로 "같은 높이에 인접 두 선 불가" 규칙이 항상 보장된다.

### 2. DRAWING 중 선 실시간 브로드캐스트 — 누가 그었는지 포함

선이 추가되는 즉시 전체 참여자에게 브로드캐스트한다. `LadderLine`에 `playerName`을 포함하여
클라이언트가 플레이어별 색깔로 선을 구분할 수 있게 한다.

```json
// /topic/room/{joinCode}/ladder/line 브로드캐스트
{
  "playerName": "꾹이",
  "segmentIndex": 2,
  "row": 3
}
```

클라이언트는 이 메시지를 수신하는 즉시 해당 위치에 해당 플레이어 색깔로 선을 렌더링한다.

### 3. 선 그리기 — 낙관적 표시 후 서버 row로 보정

클라이언트는 터치 즉시 ghost 선(반투명)을 표시하고, 서버 브로드캐스트 수신 후 실제 row로 교체한다.
row는 서버에서만 결정되므로 완전한 낙관적 업데이트는 불가능하다. 2초 내 브로드캐스트가 없으면
검증 실패로 간주하고 ghost를 제거한다.

### 4. 결과 계산 — 서버 확정, 클라이언트 애니메이션

경로 계산은 서버가 담당하여 최종 순위를 확정한다. RESULT 상태 전환 직전에 `tracePaths()`를
호출하여 순위를 확정하고, 이후 선 추가 이벤트가 결과에 영향을 주지 않도록 보장한다.

경로 애니메이션은 클라이언트가 이미 보유한 선 데이터로 자체 계산한다. 서버가 경로 전체를 전송할
필요가 없으며, 최종 순위와 애니메이션 재생 시간만 함께 제공한다.

`animationDurationMs`는 `ladder.timing.result` 설정값과 동일하며, 서버도 이 시간 후 DONE으로
전환한다. 클라이언트는 이 값으로 애니메이션 타이머를 구동한다.

```json
// /topic/room/{joinCode}/ladder/state — RESULT 전환 시
{
  "state": "RESULT",
  "rankings": {
    "꾹이": 1,
    "철수": 3,
    "영희": 2
  },
  "animationDurationMs": 5000
}
```

```json
// /topic/room/{joinCode}/ladder/state — 그 외 상태 전환 시
{
  "state": "DESCRIPTION"
}
```

### 5. DRAWING 조기 종료 없음 — 항상 5초 대기

BlockStacking과 달리 DRAWING은 항상 5초를 채운다. 모든 플레이어가 선을 그었더라도 타이머가
만료될 때까지 추가 입력을 허용하지만, 플레이어당 1개 제한이므로 이미 그은 플레이어의 입력은
무시된다.

피지컬 요소가 핵심이므로 조기 종료는 게임 긴장감을 해친다.

### 6. 상태 전환 흐름

```text
startFlow()
  → [즉시] DESCRIPTION
  → [description 5초] PREPARE
  → [prepare 3초] DRAWING
  → [drawing 5초] tracePaths() 호출 → RESULT
  → [result 고정값] DONE → MiniGameFinishedEvent 발행
```

RESULT 지속 시간은 플레이어 수와 무관하게 `ladder.timing.result` 고정값을 사용한다.
브로드캐스트 시 `animationDurationMs`로 그 값을 클라이언트에 전달한다.

### 7. 전용 WebSocket 엔드포인트 + Redis Stream

선 그리기 이벤트는 공유 CommandType 라우터를 거치지 않고 전용 엔드포인트로 수신한다.

```text
STOMP SEND /app/room/{joinCode}/ladder/draw
{
  "playerName": "꾹이",
  "segmentIndex": 2
}
```

처리 흐름은 다음과 같다.

```text
LadderWebSocketController
  → StreamPublisher.publish(LADDER_EVENTS, LadderDrawEvent)
  → LadderDrawEventConsumer.accept(event)
  → LadderService.drawLine()
  → LadderCommandService.drawLine()
  → game.drawLine() + notifier.notifyLineDrawn()
```

검증 실패(이미 그은 플레이어, 유효하지 않은 segmentIndex) 시 warn 로그만 기록하고 클라이언트에
에러 응답을 보내지 않는다.

## 고려한 대안

| 대안 | 장점 | 단점 |
|---|---|---|
| 터치 위치(좌표)로 row 결정 | 직관적 | 서버가 화면 좌표를 정규화해야 함, 동시 입력 처리 복잡 |
| DRAWING 중 선 숨기고 RESULT에서 일괄 공개 | RESULT 극적 효과 | 피지컬 긴장감 약화, 실시간 반응 없음 |
| 서버가 경로 전체 데이터 제공 | 클라이언트 구현 단순 | 불필요한 데이터 전송 (클라이언트가 이미 보유) |
| 모든 플레이어 제출 시 조기 종료 | 불필요한 대기 제거 | 피지컬 긴장감 파괴 |
| 공유 CommandType 라우터 사용 | 인프라 재사용 | 게임별 독립 진화 어려움 |

## 트레이드오프

**감수한 것들**

- 인접 충돌 해소 시 row가 예상보다 아래에 배치될 수 있다. 극단적으로 모든 플레이어가 같은
  구간에 몰릴 경우 row가 N까지 밀린다.
- 클라이언트가 경로 추적 로직을 자체 구현해야 한다.

**얻은 것들**

- row 계산이 서버에서만 이루어지므로 클라이언트-서버 간 선 위치 불일치가 없다.
- 실시간 선 표시로 피지컬 긴장감과 시각적 재미가 극대화된다.
- 전용 엔드포인트로 payload 스키마가 공유 라우터에 독립적으로 진화 가능하다.
- BlockStacking과 동일한 FlowOrchestrator + FlowScheduler 패턴을 재사용한다.

## 결과

- `laddergame/` 패키지 신설: `LadderGame`, `Poles`, `Pole`, `LadderLines`, `LadderLine`,
  `BottomRanks`, `LadderGameState`, `LadderGameScore`, `LadderFlowOrchestrator`,
  `LadderNotifier`, `LadderCommandService`
- `MiniGameType`에 `LADDER_GAME` 추가
- 선 그리기 엔드포인트: `/app/room/{joinCode}/ladder/draw`
- 선 브로드캐스트 토픽: `/topic/room/{joinCode}/ladder/line`
- 상태 브로드캐스트 토픽: `/topic/room/{joinCode}/ladder/state`
- 타이밍 설정: `ladder.timing.description=5s`, `ladder.timing.prepare=3s`,
  `ladder.timing.drawing=5s`, `ladder.timing.result=5s` (고정값, 플레이어 수 무관)
- 검증 실패 시 warn 로그 기록 위치: `LadderCommandService.drawLine()`
