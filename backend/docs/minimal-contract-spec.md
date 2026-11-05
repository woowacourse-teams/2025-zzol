# 카드게임 동작 스펙 — 최소 계약 실험용 (공개)

> 이 문서는 최소 계약 실험에서 구현 에이전트에게 공개되는 **계약의 일부**다. 함께 주어지는 문서: [원초적 규칙 명세서](minimal-contract-rules.md). 이 스펙은 밖에서 관찰되는 동작과 와이어 컨트랙트만 기술한다 — 내부 구조(클래스·패키지·저장 방식)는 규정하지 않으며, 그 설계가 과제다.
>
> 참고: 기존 `카드게임도메인모델.md`는 코드와 어긋난 항목이 있어 이 스펙이 대체한다(코드 기준, 2026-08-28).

## 1. 게임 개요

카드게임(`CARD_GAME`)은 방(`joinCode`)의 참가자들이 **2 라운드** 동안 라운드마다 카드 1장씩을 고르고, 모은 카드로 점수를 계산해 순위를 매기는 미니게임이다.

- 참가자: 방의 게이머 목록(이름·userId·colorIndex). 게임 시작 시점에 고정된다.
- 덱: **9장** — 덧셈 카드 7장 + 곱셈 카드 2장. 게임 시작 시 한 번 구성되고 두 라운드 내내 같은 덱을 쓴다.
  - 덧셈 카드 풀: `+40, +35, …, +5, 0, -5, …, -40` (5 단위)
  - 곱셈 카드 풀: `×4, ×2, ×(-1)`
- **결정성**: 덱 구성·셔플·미선택자 랜덤 배정은 `joinCode`에서 유도한 시드로 결정적이다 — 같은 방 코드는 항상 같은 덱과 같은 랜덤 배정을 만든다.
- 손패 규칙: 플레이어가 N라운드에 얻은 카드가 손패의 N번째 카드다. 카드의 "선택됨" 여부와 소유자는 **라운드 스코프**로 판정한다(1라운드에 선택된 카드도 2라운드 시점에는 미선택으로 보인다).

## 2. 상태 흐름과 타이밍

상태 집합: `READY, FIRST_LOADING, LOADING, PREPARE, PLAYING, SCORE_BOARD, DONE`

```text
READY → FIRST_LOADING → PREPARE → PLAYING → SCORE_BOARD   (1라운드)
      → LOADING ────────────────→ PLAYING → SCORE_BOARD   (2라운드 — PREPARE 없음)
      → DONE
```

- 로딩은 1라운드만 `FIRST_LOADING`, 2라운드부터 `LOADING`이다.
- 각 상태 체류 시간은 설정 값으로 제어된다(프로덕션 기준):

| 파라미터 | 값 | 의미 |
| --- | --- | --- |
| `first-loading` | 4000ms | 1라운드 로딩 체류 → `PREPARE` |
| `loading` | 3000ms | 2라운드 로딩 체류 → 곧바로 `PLAYING` |
| `prepare` | 2000ms | 준비(설명) 체류 → `PLAYING` |
| `playing` | 10250ms | 카드 선택 가능 시간 상한 |
| `score-board` | 1500ms | 점수판 체류 → 다음 라운드 또는 종료 |
| `early-finish-delay` | 2000ms | 전원 선택 완료 시, 이 지연 후 라운드 조기 종료 |

- `PLAYING`은 "제한시간 만료"와 "전원 선택 완료 후 `early-finish-delay` 경과" 중 먼저 오는 쪽으로 끝난다.
- 상태가 바뀔 때마다 상태 토픽(§4)으로 전체 스냅샷이 브로드캐스트된다.

## 3. 게임 규칙

**카드 선택**

- 선택은 `PLAYING` 상태에서만 허용된다. 그 외 상태의 선택 명령은 오류(§4 에러)로 거부된다.
- 이미 선택된 카드는 다른 플레이어가 선택할 수 없다(오류).
- 유효하지 않은 `cardIndex`(범위 밖)는 오류다.
- 플레이어는 라운드당 1장만 선택한다.
- 전원이 선택하면 라운드가 완료로 판정되고 조기 종료 경로를 탄다.

**미선택자 랜덤 배정**

- 라운드가 끝날 때(시간 만료 포함) 카드를 고르지 않은 플레이어에게는 **남은(미선택) 카드 중에서 랜덤으로** 1장이 강제 배정된다 — 따라서 모든 플레이어는 항상 라운드 수만큼 카드를 갖는다.
- 전원이 이미 선택한 상태에서 배정은 아무 변화도 만들지 않는다(멱등).

**점수와 순위**

- 점수 = (손패의 덧셈 카드 값 합계) × (손패의 곱셈 카드 값의 곱). 초기값: 합계 0, 곱 1.
  - 예: `+30, ×2` → 60 / `+30, -10` → 20 / `+10, ×(-1)` → -10 / 곱셈 2장 → 합계가 0이라 0점.
- 카드가 없는 플레이어는 0점이며, 점수·순위 결과에는 **참가자 전원**이 포함된다.
- 순위는 점수 내림차순, 동점은 같은 등수·다음 등수 건너뜀(표준 경쟁 순위: 1,1,3…).
- 게임 종료 시 결과(플레이어별 순위)가 확정되어 조회 API(§4 REST)에 반영된다.

## 4. 와이어 컨트랙트 (동결)

아래 경로·필드는 **그대로 유지해야 한다**. 필드 추가·삭제·개명 금지. FE는 수정되지 않는다.

### 인바운드 — WS 명령

`SEND /app/room/{joinCode}/minigame/command` — 공용 봉투:

```json
{"commandType": "START_MINI_GAME", "commandRequest": {"hostName": "꾹이"}}
{"commandType": "SELECT_CARD",     "commandRequest": {"playerName": "꾹이", "cardIndex": 0}}
```

- `commandType`으로 명령을 구분하고 페이로드는 `commandRequest`에 중첩된다.
- `cardIndex`는 상태 페이로드의 `cardInfoMessages` 배열 인덱스다(덱 순서 = 클라이언트 카드 인덱스).
- 게임 시작(`START_MINI_GAME`)은 방장 검증을 거친다 — 방장이 아니면 시작되지 않는다.

### 아웃바운드 — WS 토픽

모든 발행은 **`WebSocketResponse<T>`** 래퍼로 감싼다 — `:websocket` 모듈의 공용 클래스(`coffeeshout.websocket.ui.WebSocketResponse`)로, **재구현 대상이 아니라 그대로 재사용한다**. `@JsonInclude(NON_NULL)`이라 null 필드는 직렬화에서 빠진다.

| 필드 | 타입 | 성공 시 | 실패 시 |
| --- | --- | --- | --- |
| `success` | boolean | `true` | `false` |
| `data` | T | 페이로드 | 생략 |
| `errorMessage` | string | 생략 | 에러 메시지 |
| `id` | string | 복구용 메시지 ID — 발행 인프라(`LoggingSimpMessagingTemplate`)가 부여 | 동일 |

**`/topic/room/{joinCode}/gameState`** — 페이로드 `MiniGameStateMessage` (상태 변경·카드 선택 때마다 전체 스냅샷):

| 필드 | 타입 | 값 |
| --- | --- | --- |
| `cardGameState` | string | §2 상태 이름 |
| `currentRound` | string | `READY` / `FIRST` / `SECOND` |
| `cardInfoMessages` | array | 덱 9장 전체, 배열 순서 = 카드 인덱스 |
| `allSelected` | boolean | 현재 라운드 전원 선택 여부 |

`cardInfoMessages[]` 항목:

| 필드 | 타입 | 값 |
| --- | --- | --- |
| `cardType` | string | `ADDITION` / `MULTIPLIER` |
| `value` | number | 카드 값 |
| `selected` | boolean | **현재 라운드에서** 소유자가 있는지 |
| `playerName` | string/null | 현재 라운드 소유자 이름, 없으면 null |
| `colorIndex` | number/null | 소유자 색 인덱스, 없으면 null |

**`/topic/room/{joinCode}/round`** — 페이로드 `{"miniGameType": "CARD_GAME"}`. 게임 시작 시 1회 발행.

- 주의: `round`와 첫 `gameState`의 **도착 순서는 보장되지 않는다** — 클라이언트는 순서에 의존하지 않는다.

### 에러

- 오류는 브로드캐스트가 아니라 명령을 보낸 사용자의 **개인 큐 `/user/queue/errors`** 로 간다. 예: `PLAYING`이 아닐 때 선택 → 409, "현재 게임이 진행중인 상태가 아닙니다."
- 거부된 명령은 게임 상태를 바꾸지 않으며, 어떤 상태 브로드캐스트도 만들지 않는다.
- 참고: 현행(Stream 경유) 구현은 컨슈머 안에서 거부된 명령을 개인 큐로 통지하지 못한다 — **신규 구현은 거부를 개인 큐로 통지해야 한다**(홀드아웃 스위트가 검증).

### REST

| 엔드포인트 | 응답 |
| --- | --- |
| `GET /minigames/scores?joinCode=&miniGameType=` | `{"scores":[{"playerName","score"}]}` |
| `GET /minigames/ranks?joinCode=&miniGameType=` | `{"ranks":[{"playerName","rank"}]}` |
| `GET /rooms/minigames` | `["CARD_GAME", …]` |
| `GET /rooms/minigames/selected?joinCode=` | `["CARD_GAME", …]` |
| `GET /rooms/{joinCode}/miniGames/remaining` | `{"remaining":["CARD_GAME", …]}` |

- `selected`/`remaining`은 방이 없으면 404("방이 존재하지 않습니다").
- `scores`/`ranks`의 배열 순서는 보장되지 않는다.

## 5. 아키텍처 요구 (요약 — 상세는 규칙 명세서)

- **멀티 인스턴스**: 서버 인스턴스가 여러 개이고 명령은 임의 인스턴스에 도착한다. 어느 인스턴스가 받아도 게임이 올바로 진행되어야 한다.
- **외부 상태 + 이벤트**: 게임 상태는 외부 저장소에 두고, 상태 변경 후 이벤트를 발행해 후속 처리(알림·영속)를 잇는다. 명령 처리를 Redis Stream으로 우회하지 않는다.
- **타이머**: 페이즈 마감 시각을 저장소에 두고 폴링 claim으로 발화한다.
- 동시성·이벤트·알림·테스트 규율은 [원초적 규칙 명세서](minimal-contract-rules.md)의 R1–R12와 실험 수칙을 따른다.
