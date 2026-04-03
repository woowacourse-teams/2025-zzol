# 0002. BlockStacking 게임 서버 설계

- 날짜: 2026-04-03
- 상태: 승인

## 컨텍스트

BlockStacking은 플레이어가 좌우로 이동하는 블록을 탭하여 쌓는 미니게임이다. 게임 로직(블록 이동, 슬라이싱, 렌더링)은 **클라이언트 로컬에서 낙관적으로 처리**되며, 서버는 다음 두 가지 역할만 담당한다.

1. **실시간 랭킹 관리** — 각 플레이어의 현재 층수를 수신해 전체 플레이어에게 브로드캐스트
2. **공식 점수 확정** — 20초 타이머 만료 시 서버가 가진 층수를 최종 점수로 기록

클라이언트가 잘못된 탭 좌표를 전송할 수 있다는 점, 게임 흐름을 어떻게 종료할지, FlowScheduler 포트를 어디에 위치시킬지 등의 설계 결정이 필요했다.

## 결정

### 1. 잘못된 progress 이벤트 처리 — 무시 + 경고 로그

서버가 수신한 탭 좌표로 overlap을 재계산한다.

```java
double overlap = Math.min(movingBlockX + stackTopWidth, stackTopX + stackTopWidth)
               - Math.max(movingBlockX, stackTopX);
```

overlap ≤ 0이거나 floor 값이 비연속적으로 증가하면 **해당 이벤트를 무시하고 경고 로그만 남긴다**. 프론트에 에러 응답을 보내지 않으며, 재전송을 요청하지 않는다. 서버가 인정한 마지막 `currentFloor`가 최종 점수가 된다.

재전송(retry) 패턴을 채택하지 않은 이유:

- 프론트는 낙관적 업데이트 방식이므로 서버 응답을 기다리지 않고 즉시 로컬 상태를 갱신한다
- 서버가 reject 응답을 보낼 시점에 블록 위치가 이미 달라져 재현 불가능하다
- 파티 게임 특성상 단일 이벤트 거부가 게임 흐름 전체를 방해해선 안 된다

### 2. 게임 종료 기준 — 서버 20초 타이머 단일 기준

전원 제출을 기다리는 `EarlyFinishTrigger` 패턴을 적용하지 않는다. 타이머 만료 시점에 서버가 보유한 각 플레이어의 `currentFloor`를 최종 점수로 확정한다. 별도 `submit` 커맨드도 두지 않는다. 플레이어 블록이 이탈해도 이후 progress를 보내지 않으면 그 층수가 자연스럽게 최종값이 된다.

### 3. 플레이어 진행 상태 — 단일 객체로 관리

`Map<Player, Integer> currentFloors`와 `Map<Player, Integer> finalFloors`를 별도로 두지 않고, `BlockStackingPlayerProgress` 도메인 객체 하나로 통합 관리한다.

```java
class BlockStackingPlayerProgress {
    PlayerName playerName;
    int currentFloor;   // progress 이벤트마다 갱신, 타이머 만료 시 최종값
}
```

### 4. FlowScheduler — global 포트로 승격

`FlowScheduler`, `FlowHandle`, `EarlyFinishTrigger` 모두 `cardgame/application/port/`에서 `global/flow/`로 이동한다. 각 게임은 별도 빈으로 등록된 `CompletableFuture` 구현체를 주입받아 스레드풀을 독립적으로 관리한다.

### 5. progress 수신 — 전용 WebSocket 컨트롤러 + Redis Stream

progress 이벤트를 공유 `CommandType` 라우터(`/app/room/{joinCode}/minigame/command`)를 거치지 않고 전용 엔드포인트로 직접 수신한다.

```
STOMP SEND /app/room/{joinCode}/block-stacking/progress
{
  "playerName": "꾹이",
  "floor": 3,
  "movingBlockX": 100.0,
  "stackTopX": 85.0,
  "stackTopWidth": 150.0
}
```

`tapX`(터치 좌표)는 서버가 블록의 실시간 이동 위치를 알 수 없어 overlap 계산에 사용할 수 없으므로 제거했다. overlap 검증에는 `movingBlockX`, `stackTopX`, `stackTopWidth`만 사용한다.

`BlockStackingWebSocketController`는 수신한 요청을 `BlockStackingCommandEvent`로 변환해 `StreamKey.BLOCK_STACKING_EVENTS` Redis Stream에 발행한다. `BlockStackingCommandEventConsumer`가 이를 소비해 `BlockStackingService.recordProgress()`를 호출한다.

```
WebSocketController
  → StreamPublisher.publish(BLOCK_STACKING_EVENTS, event)
  → BlockStackingCommandEventConsumer.accept(event)
  → BlockStackingService.recordProgress()
  → BlockStackingCommandService.recordProgress()
  → game.recordProgress() + notifier.notifyProgressUpdated()
```

### 6. PLAYING 상태 — 서버 종료 타임스탬프 동기화

PLAYING 상태로 전환할 때 서버 기준 게임 종료 시각을 함께 전달한다. 클라이언트는 `Date.now()`와의 차이로 남은 시간을 계산하므로 네트워크 지연이 자동 보정된다.

```json
// PLAYING 전환 시
{ "state": "PLAYING", "endTimeEpochMs": 1712140000000 }

// PREPARE / DONE
{ "state": "PREPARE" }
{ "state": "DONE" }
```

`endTimeEpochMs`는 `Instant.now().plus(timing.playing())`으로 계산되며, `@JsonInclude(NON_NULL)`로 PLAYING 이외의 상태에서는 직렬화에서 제외된다.

### 7. 상태 응답 통일 — BlockStackingStateResponse

PREPARE / PLAYING / DONE 세 가지 상태 전환 알림을 단일 `BlockStackingStateResponse`로 통일한다. 별도 complete 토픽(`/block-stacking/complete`)을 두지 않는다. 게임 최종 결과는 `room.applyMiniGameResult()`로 Room Score에 반영되므로 DONE 알림에 랭킹 데이터를 포함할 필요가 없다.

### 8. FlowOrchestrator — 단일 체인

```text
startFlow()
  → [즉시] PREPARE
  → [prepare 3초] PLAYING
  → [playing 20초] FINISH_GAME → MiniGameFinishedEvent 발행
```

라운드 반복, 조기 종료 트리거 없음.

## 고려한 대안

| 대안                                          | 장점                | 단점                                   |
|---------------------------------------------|-------------------|--------------------------------------|
| 잘못된 값 수신 시 에러 응답 + 재전송 요청                   | 서버-클라이언트 상태 동기화   | 낙관적 업데이트와 충돌, 재현 불가                  |
| 잘못된 값 수신 시 WebSocket 연결 끊기                  | 강력한 치팅 차단         | 파티 게임에 과도한 제재, UX 파괴                 |
| 전원 제출 완료 시 조기 종료 (EarlyFinishTrigger)       | 전원 완료 즉시 결과 확인 가능 | 미제출 플레이어로 인한 대기, 별도 제출 커맨드 필요        |
| currentFloors / finalFloors 분리 관리           | 제출 전후 상태 명시적 구분   | 두 Map 동기화 부담, 클래스 외부에서 관리 규칙 추론 필요   |
| FlowScheduler를 각 게임 패키지에 유지                 | 게임별 완전 독립         | 동일 패턴 중복, 의존성 방향 불명확                 |
| progress 이벤트를 공유 CommandType 라우터 사용         | 기존 인프라 재사용        | 게임별 독립 진화 어려움, payload 구조 공유 라우터에 노출 |
| progress 컨트롤러에서 서비스 직접 호출 (Redis Stream 없음) | 구조 단순, 지연 최소      | Redis Stream 기반 소비자 패턴 일관성 깨짐        |
| complete 토픽 별도 유지                           | 명시적 완료 신호         | state 토픽과 중복, 프론트 구독 대상 증가           |

## 트레이드오프

**감수한 것들**

- 잘못된 값 무시 정책으로 인해 클라이언트가 보여주는 층수와 서버의 공식 점수가 다를 수 있다. 버그가 있는 클라이언트는 높은 층수를 보더라도 서버 점수가 낮게 기록된다.
- 20초 타이머 기준이므로 블록이 일찍 이탈한 플레이어도 타이머 만료까지 결과를 모른다.
- FlowScheduler를 global로 올리면 CardGame 참조 경로 수정이 선행되어야 한다.

**얻은 것들**

- 서버 로직이 단순해져 유지보수 비용이 낮다.
- 클라이언트 구현의 복잡도(retry, ack 대기)가 줄어들어 게임 반응성이 높다.
- `BlockStackingPlayerProgress` 단일 객체로 도메인 규칙이 한 곳에 집중된다.
- FlowScheduler global 승격으로 신규 게임 추가 시 동일 인프라를 재사용할 수 있다.
- 전용 컨트롤러로 인해 progress payload 스키마가 공유 라우터(`CommandType`)에 독립적으로 진화 가능하다.
- `endTimeEpochMs` 동기화로 클라이언트 타이머가 네트워크 지연에 강해진다.
- 상태 응답 통일(`BlockStackingStateResponse`)로 프론트엔드 구독 대상이 단순해진다.

## 결과

- `global/flow/FlowScheduler`, `global/flow/FlowHandle` 신규 위치 (CardGame에서 이동)
- `blockstacking/` 패키지 신설: `BlockStackingGame`, `BlockStackingPlayerProgress`, `BlockStackingGameStep`, `BlockStackingFlowOrchestrator`, `BlockStackingNotifier`, `BlockStackingCommandService`
- `MiniGameType`에 `BLOCK_STACKING` 추가
- progress 수신 전용 엔드포인트: `/app/room/{joinCode}/block-stacking/progress` (`CommandType` 미사용)
- 상태 알림 토픽: `/topic/room/{joinCode}/block-stacking/state` (PREPARE / PLAYING / DONE 통일)
- 랭킹 알림 토픽: `/topic/room/{joinCode}/block-stacking/progress`
- PLAYING 전환 시 `endTimeEpochMs` (UTC epoch ms) 포함
- 검증 실패 시 warn 로그 기록 위치: `BlockStackingGame.recordProgress()`
- 타이밍 설정: `block-stacking.timing.prepare=3s`, `block-stacking.timing.playing=20s`
