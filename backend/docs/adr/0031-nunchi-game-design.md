# 0031. 눈치게임(Nunchi) 서버 설계 — 순차 카운팅·동시 입력 순위 판정과 FE/BE 조율

- 날짜: 2026-06-21
- 상태: 승인 (FE/BE 조율 완료 — 구 미결 질문 Q1~Q7 모두 확정; 2026-06-21 구현 메커니즘 확정 — 「구현 노트」 절 참조; 2026-06-24 개정 — 시작 `DESCRIPTION` 단계 추가, 결정 8 구독 스냅샷을 description 종료 자동 재발행으로 대체. 「결정 9」 절 참조)

## 컨텍스트

눈치게임은 플레이어들이 서로의 눈치를 보며 **숫자를 순서대로 외치듯 한 명씩 키패드를
누르는**(=일어서는) 파티 게임이다. 공유 카운터가 1, 2, 3… 으로 올라가고, 각 플레이어는
자기 차례를 노려 다음 숫자를 누른다. **같은 숫자를 둘 이상이 동시에 누르면 충돌**이다.
화면 상단 절반은 현재 숫자 / 일어선 플레이어 / 일어서기 애니메이션을, 하단 절반은 입력용
키패드를 보여준다. 핵심은 **언제 누가 눌렀는가**이며, 순위 규칙은 다음과 같다.

- 누른 순서대로 순위가 매겨진다 (먼저 누른 사람이 상위).
- 동시에 누른 사람들은 단독 입력자 전원보다 아래로 강등된다. 단 **모든 충돌이 한 꼴등으로
  뭉치는 것이 아니라, 충돌이 일어난 순서대로 아래에서부터 스택처럼 쌓인다**(먼저 충돌한
  그룹이 더 아래, 나중 충돌한 그룹이 그 위).
- 예: 처음에 2명이 동시에 눌렀고 그다음 1명이 단독으로 눌렀다면, 동시 누른 2명은 단독
  입력자 아래로 내려가고 단독으로 누른 사람이 1등이 된다(이 예엔 미입력자가 없어 충돌 2명이
  최하다 — 일반적으로는 미입력자가 그 아래에 온다).

이 게임의 본질은 **밀리초 단위 입력 타이밍 판정**이다. 그런데 이 백엔드의 입력 처리 경로는
`Handler → Redis Stream(XADD) → Consumer(비동기) → Service`이며(아키텍처 불변 규칙),
**Consumer가 이벤트를 처리하는 순서는 플레이어가 실제로 누른 순서가 아니다.** 스트림 발행
시점은 WS 수신 시각에 좌우되고, 클라이언트마다 RTT(왕복 지연)가 다르므로 순서가 뒤집히거나
실제 동시가 아닌 입력이 동시처럼 보일 수 있다.

따라서 "누른 순서"와 "동시"를 **무엇으로 판정할 것인가**가 이 게임의 설계 축이며, 이는
프론트엔드와 백엔드가 합의해야 하는 영역이다. 본 ADR은 확정된 부분과 열린 질문을 분리해
조율의 출발점을 만든다. 게임 통합 구조 자체는 기존 게임(ADR-0002 BlockStacking, ADR-0003
LadderGame)의 SPI·Flow 패턴을 그대로 따른다.

## 결정 (확정)

### 1. 게임 통합 — 기존 SPI 패턴 재사용

신규 게임 추가 절차를 그대로 따른다. 새로운 인프라를 만들지 않는다.

- `MiniGameType`에 `NUNCHI_GAME` 추가
- `Playable`(`game-api/gamecommon`) 구현 + `MiniGameFactory` 등록(`@Component`)
- `:game` 모듈에 `nunchi/` 패키지 신설(domain / application / ui / infra / config)
- 입력은 전용 WebSocket 엔드포인트 → Redis Stream → Consumer 경유(Application Service 직접
  호출 금지). 잘못된 입력(중복 press 등)은 **warn 로그만** 남기고 에러 응답·재전송 요청을 하지
  않는다(ADR-0002/0003 일관).
- `FlowOrchestrator` + `FlowScheduler`(`global/flow`) 재사용
- `:game`은 `Gamer`만 사용하고 `Player`를 import하지 않는다(ADR-0025 제약)

### 2. 순위 규칙 — 3계층 스택

플레이어는 **1인 1회** 누른다(한 번 일어서면 끝, 추가 입력은 무시 — ADR-0003 "1인 1선" 패턴).
최종 순위는 좋음→나쁨 순으로 3계층이다.

1. **정상(단독) 입력** — 충돌 없이 깨끗하게 누른 사람들. 누른 순서대로 상위 rank(먼저 = 1등).
2. **충돌 실패** — 동시(300ms 이내)에 누른 그룹들. 한 꼴등으로 뭉치지 않고 **발생 순서대로
   스택**된다: 가장 먼저 충돌한 그룹이 아래(더 나쁨), 나중 충돌한 그룹이 그 위(덜 나쁨). 한
   그룹 안의 동시 입력자끼리는 동점.
3. **미입력(타임아웃) 실패** — 제한 시간 동안 끝내 누르지 않은 사람들. **제일 나쁜 계층(제일
   꼴등)**이며 서로 동점.

원리: 정상 입력이 가장 좋고, 그다음이 충돌(늦게 충돌할수록 덜 나쁨), 끝내 누르지 않은 사람이
제일 나쁘다.

예시(발생 순서):

| 시각     | 입력     | 분류      |
|--------|--------|---------|
| t1     | A·B 동시 | 충돌 그룹 1 |
| t2     | C·D 동시 | 충돌 그룹 2 |
| t3     | E 단독   | 정상      |
| (안 누름) | F      | 미입력     |

최종 순위(좋음→나쁨): **E(1등) → C·D(공동 2등, 나중 충돌) → A·B(공동 4등, 먼저 충돌) →
F(6등, 미입력·제일 꼴등).**

### 3. 순위 표현 — 기존 `MiniGameResult` 재사용 (새 타입 금지)

`MiniGameResult.of(scores, comparator)`는 정렬 후 **동점(`MiniGameScore.equals`)에 동일 rank**를
부여하는 standard-competition 랭킹이다(예: `1,2,2,4,6`). 결정 2의 3계층 스택이 이 메커니즘으로
그대로 표현되므로, `NunchiScore`(도메인 점수)만 정의하고 별도 결과 타입을 만들지 않는다.
`NunchiScore`는 전용 `Comparator`가 아니라 **계층·시각을 단일 `long`(`getValue()`)에 밴드
인코딩**한다(`SpeedTouchScore` 선례 — 구현 노트 N4). 점수는 좋음→나쁨 순으로 다음과 같이
정렬되게 설계한다.

1. **정상(단독)**: 각자 권위 시각으로 **유일한** 점수(먼저 = 더 좋음) → 서로 동점 아님.
2. **충돌**: 각 그룹은 자신의 충돌 시점을 키로 한 점수를 **그룹 안에서 공유**(→ 동점), 그룹
   사이에서는 **먼저 충돌할수록 나쁜** 점수.
3. **미입력**: 모두 **단일 "최악" 점수**를 공유 → 서로 동점이며 제일 꼴등.

검증: A·B 동시 + C 단독 → `{C:1, A:2, B:2}`. 여기에 D·E가 더 늦게 충돌하면
`{C:1, D:2, E:2, A:4, B:4}`(나중 충돌이 위에 쌓임). F가 끝내 미입력이면 F는 최하 rank에 붙는다.

> 주의: `MiniGameResult`는 `comparator`로 정렬하되 rank 동일 여부는 `MiniGameScore.equals()`로
> 판정한다. 같은 그룹/계층이 동일 rank를 받으려면 **comparator로 동치이면서 `equals()`로도
> 동치**여야 한다. `NunchiScore`의 `compareTo`/`equals` 일관성(consistent with equals)을
> 반드시 맞춘다.

### 4. 결과는 서버 확정 — 클라이언트 타임스탬프는 권위가 아니다

순위 확정은 서버가 담당한다(ADR-0002/0003 선례). 클라이언트가 보낸 탭 시각(`pressedAt`)은
**보조·안티치트 입력**일 수는 있어도 권위 신호가 될 수 없다(시계 스큐·조작 가능). 따라서
Q1은 "클라이언트 vs 서버"가 아니라 **"어떤 서버 신호인가"**의 문제로 좁혀진다.

권위 신호는 **서버 WS 수신 `Instant`**(컨트롤러에서 스트림 발행 직전 기록)이다. 결정적으로,
컨슈머는 입력을 **스트림 도착 순서가 아니라 이 `Instant` 순서로 정렬**해 숫자(카운터)·순위를
매긴다. STOMP 인바운드가 멀티스레드라 도착 순서는 `Instant`와 어긋날 수 있으므로, 도착 순서로
카운터를 전진시키면 "처리 순서 ≠ 누른 순서"라는 이 ADR의 전제가 깨진다.

### 5. 종료 조건 — 전원 입력 / 무입력 idle 타임아웃 + 하드 안전 캡

1인 1press이므로 누를 사람이 다 누르면 더 진행할 것이 없다. 종료 트리거는 세 가지다.

1. **전원 입력 완료** — 모든 플레이어가 (정상/충돌로) 입력을 마치면 즉시 종료한다
   (`raceTimeout` 조기 종료, ADR-0002 `EarlyFinishTrigger` 패턴).
2. **무입력 idle 타임아웃** — 새 입력 없이 일정 시간(예: 10초)이 지나면 종료한다. 남은
   미입력자는 미입력 실패(제일 꼴등)로 확정된다. **idle 타이머는 입력마다 리셋되고,
   `COLLISION_COOLDOWN` 동안은 일시정지**한다(쿨다운을 idle로 오인하지 않게).
3. **하드 안전 캡** — 라운드 전체가 일정 시간(예: 30초)을 넘기면 강제 종료한다. 질질 끌기·방
   점유를 막는 안전망일 뿐 정상 종료 경로가 아니다.

**고정 총 타이머(단일 15초)를 1차 기준에서 뺀 이유**: 충돌·쿨다운이 그 시간을 잠식해 *아직
누를 사람이 남았는데 게임이 잘려나가는* 문제가 있다. idle 타임아웃은 "실제로 플레이가 멈췄을
때"만 끝나므로 충돌 빈도와 무관하게 자연스럽게 종료된다. 하드 캡은 idle의 드래그(질질 끌기)
위험만 막는 보조 장치다. 모든 시간 값은 결정 7대로 `nunchi.timing.*` 설정이다.

### 6. 충돌 후 재개 — 카운터 reset + 쿨다운 + 서버 타임스탬프 동기화

같은 숫자에서 충돌이 나면 그 숫자가 비므로, 공유 카운터를 **그 숫자로 되돌리고(reset)** 다음
클린 입력자가 다시 차지하게 한다(결정 2의 "정상=누른 순"이 그대로 성립). 이때 **언제부터 그
숫자를 다시 눌러도 되는지**를 플레이어에게 명확히 전달해야 한다.

충돌 직후 즉시 재개하면 (a) 화면이 충돌→재개로 깜빡여 인지가 어렵고 (b) 같은 긴장으로 재충돌이
잦다. 따라서 **충돌 후 짧은 쿨다운을 두고 재개**한다. 게임은 `PLAYING`(카운터 활성) ↔
`COLLISION_COOLDOWN`(대기)을 오가며 진행한다.

재개 시점은 **서버 기준 epoch ms(`resumeAtEpochMs`)로 브로드캐스트**한다. 클라이언트는
`Date.now()`와의 차이로 남은 대기 시간을 카운트다운하므로 네트워크 지연이 자동 보정된다 —
ADR-0002 §6의 `endTimeEpochMs` 패턴을 그대로 재사용한다. `resumeAtEpochMs` 이전에 들어온
입력은 무시(warn 로그)한다.

```json
// /topic/room/{joinCode}/nunchi/state — 충돌 발생 시
{
  "state": "COLLISION_COOLDOWN",
  "number": 1,
  "collided": ["철수", "영희"],
  "serverNowEpochMs": 1712140002800,
  "resumeAtEpochMs": 1712140003000
}
```

쿨다운 동안에는 무입력 idle 타임아웃(결정 5)을 일시정지해 쿨다운을 idle로 오인하지 않는다.
전체 시간은 하드 안전 캡(결정 5)이 bound한다.

### 7. 타이밍 값은 외부 설정으로 분리

동시 판정 윈도우(300ms), 충돌 쿨다운, 무입력 idle 타임아웃, 하드 안전 캡은 코드 상수가 아니라
**외부 설정값**(`nunchi.timing.*`)으로 분리해 런타임/환경별로 조정 가능하게 한다. 기존 게임의
`*TimingProperties`(`@ConfigurationProperties`) 패턴을 그대로 따른다(ADR-0002/0003 일관).

### 8. WebSocket 상태 컨트랙트 — 모든 타이밍을 서버 epoch ms로 통보

FE가 카운트다운을 정확히 그릴 수 있도록 모든 시간 정보를 **서버 기준 epoch ms**로 `state`
토픽에 실어 보낸다. FE는 `Date.now()` 차이로 남은 시간을 계산하므로 네트워크 지연이 자동
보정된다(ADR-0002 §6 패턴). 상태 머신은 `DESCRIPTION → PLAYING ↔ COLLISION_COOLDOWN → DONE`이다
(`DESCRIPTION`은 2026-06-24 개정 — 결정 9).

```json
// PLAYING (시작·충돌 후 재개·재접속 스냅샷) — 현재 숫자 + 일어선 사람 + 종료 데드라인
{ "state": "PLAYING", "currentNumber": 1, "stood": ["민수"], "serverNowEpochMs": 1712140000000, "idleDeadlineEpochMs": 1712140010000, "hardCapEpochMs": 1712140030000 }

// COLLISION_COOLDOWN (충돌 발생) — 누가 충돌했고 언제 재개되는지 (결정 6)
{ "state": "COLLISION_COOLDOWN", "number": 1, "collided": ["철수", "영희"], "serverNowEpochMs": 1712140002800, "resumeAtEpochMs": 1712140003000 }

// DONE
{ "state": "DONE" }
```

```json
// /topic/room/{joinCode}/nunchi/stand — 한 명이 번호를 차지(첫 press 즉시·낙관적, N2)
// number는 카운터 값이지 등수가 아니다(stand는 rank 미포함). 충돌은 stand가 아니라
// COLLISION_COOLDOWN state로 통보된다.
{ "name": "민수", "number": 1, "serverNowEpochMs": 1712140000000, "idleDeadlineEpochMs": 1712140010000 }
```

- **타임아웃 통보**: `idleDeadlineEpochMs`(무입력 자동 종료 예정 시각)는 유효 입력마다 갱신되며,
  갱신값을 `stand` 브로드캐스트에 함께 실어 FE가 카운트다운을 리셋한다. `hardCapEpochMs`는
  고정 상한이다.
- **대기 통보**: 충돌 시 `resumeAtEpochMs`로 "잠시 후 재개" 카운트다운(결정 6).
- **재시작 통보**: 쿨다운 종료 시 새 `PLAYING` 메시지(리셋된 `currentNumber` + 갱신 데드라인).
- **시계 스큐 보정**: 모든 state/stand 메시지에 `serverNowEpochMs`(서버 현재 시각)를 함께 싣는다.
  FE는 메시지 도착마다 `offset = serverNowEpochMs - Date.now()`를 갱신하고
  `남은시간 = deadline - (Date.now() + offset)`로 계산한다. 절대 epoch ms 데드라인만으로는 클라
  시계가 1~2초 빠를 때 짧은 쿨다운(수백 ms)이 음수로 시작해 즉시 끝나버린다(짧은 카운트다운 특유
  문제 — BlockStacking 20초에선 안 보였다). 스큐는 순위엔 영향이 없고(순위는 서버 `Instant` 권위 —
  Q1) 화면 카운트다운만 망치지만, 그 카운트다운이 이 게임의 긴장 그 자체다.
- **stand 페이로드는 rank를 싣지 않는다**: 라이브 일어서기는 "일어섰다"는 사실(누가)만 통보하고
  순번 숫자·등수를 넣지 않는다. 라이브 도착 순서조차 서버 권위 순서(Q1)와 다를 수 있고 충돌 통보로
  강등될 수 있어("먼저 눌렀는데 꼴등" 절벽), 잠정 등수를 박으면 오해를 키운다. 최종 순위는 결과
  단계에서만 확정 노출한다.
- **재접속 스냅샷** (원안 → **2026-06-24 개정으로 철회**, 결정 9 참조): 원안은 구독 직후 BE가 현재
  `state`를 1회 푸시(`NunchiStateSubscriptionHandler`)해 start 레이스(시작 broadcast 유실)를 막는
  것이었다. 그러나 결정 9의 DESCRIPTION 단계 도입으로 `onDescriptionEnd`가 입력과 무관하게
  description 종료 시 PLAYING을 **자동 재발행**하므로, 시작 broadcast를 놓쳐도 그 전에 구독만 하면
  복구된다. 이로써 구독 스냅샷 핸들러는 제거됐다. PLAYING 페이로드의 `currentNumber`·`stood`는 그대로
  유지되며, 게임 중 새로고침 재접속은 `WsRecoveryService`가 커버한다. 데드라인은 절대 epoch ms라 늦게
  받아도 정확하다.

토픽·커맨드는 ADR-0012의 `@WsTopic`/`@WsReceive`로 등록해 `/dev/ws-catalog` 디스커버리에
노출한다.

### 9. 시작 규칙 설명(DESCRIPTION) 단계 + 종료 후 결과 대기 — 2026-06-24 개정

다른 미니게임(Ladder/Racing/SpeedTouch/BlindTimer의 `DESCRIPTION`, BlockStacking/CardGame의
`READY`)과 동일하게 **시작 직후 짧은 규칙 설명 시간**을 둔다. 상태 머신은
`DESCRIPTION → PLAYING ↔ COLLISION_COOLDOWN → DONE`이 된다.

- `setUp` 직후 상태는 `DESCRIPTION`이며 이 구간엔 idle·하드캡·윈도우 타이머를 걸지 않는다.
  DESCRIPTION 중 들어온 `press`는 도메인이 `IGNORED`로 흡수한다(결정 1 일관 — warn 로그만, 에러
  응답 없음).
- `description`(`nunchi.timing.description`, 예 4초) 경과 시 `onDescriptionEnd`가 `PLAYING`으로
  전이하며 그때 idle·하드캡 타이머를 시작한다. **하드캡은 PLAYING 시작 시점부터** 잰다(설명 시간은
  라운드 상한에 포함하지 않음 — 결정 8 고정 상한과 일관).
- DESCRIPTION 메시지도 epoch 컨트랙트(결정 8)를 따른다: `playStartEpochMs`로 PLAYING 시작 절대
  시각을 실어 FE가 정확한 카운트다운을 그린다.

```json
// /topic/room/{joinCode}/nunchi/state — 시작 규칙 설명
{ "state": "DESCRIPTION", "serverNowEpochMs": 1712140000000, "playStartEpochMs": 1712140004000 }
```

**부수 효과 — 결정 8 구독 스냅샷 철회**: `onDescriptionEnd`의 자동 PLAYING 재발행이 입력과 무관하게
description 종료 시점에 일어나므로, 시작 broadcast(DESCRIPTION) 유실(start 레이스)을 그 자체로
흡수한다. 따라서 원안의 구독 스냅샷 핸들러(`NunchiStateSubscriptionHandler`)는 불필요해져 제거했다.
잔여 빈틈은 시작 +`description` **이후**에야 구독하는 극단 케이스뿐이며(초기 로딩 4초는 넉넉),
게임 중 새로고침 재접속은 `WsRecoveryService`가 커버한다.

**종료 후 결과 대기(`result-delay`)**: 게임이 끝나면 `DONE`은 즉시 브로드캐스트하되, 다음 단계로의
전이(`MiniGameFinishedEvent` 발행 → 확률 조정·SCORE_BOARD 전이)는 곧바로 가지 않고
`result-delay`(`nunchi.timing.result-delay`, 예 1초)만큼 결과를 보여준 뒤 넘어간다. 기존엔 `finish()`가
DONE 알림과 이벤트 발행을 한 번에 동기로 처리해 종료 즉시 화면이 다음 단계로 튀었다 — Ladder의 RESULT
지연·BlindTimer `result-delay`와 같은 "결과를 잠깐 보여주고 전이" 패턴으로 맞춘다. 순서 불변식(결정 5·
ADR-0025 결정 5)은 그대로다: 지연 후 `finishGame()`으로 roundCount를 확정한 뒤 이벤트를 발행한다.

## 핵심 설계 긴장 — 이 ADR의 심장

### "동시(ε)"의 물리적 의미

ε(동시로 간주하는 시간 윈도우)는 임의값이 아니다. 두 입력이 **서로의 일어서기 브로드캐스트를
보고 반응했다고 보기 불가능할 만큼 가까운가**를 뜻한다. 브로드캐스트 RTT + 인간 반응시간
(~200–300ms)이 그 하한을 정한다. 사람은 보통 초 단위로 망설이며 누르므로 **충돌은 엣지
케이스**이고, 수십 ms의 네트워크 지터는 ε 안에서 흡수될 여지가 있다. 이 관점이 ε를 과도하게
두렵게 보지 않게 한다.

### Q1과 Q3는 결합되어 있다 — ε가 트레이드오프 크기를 정한다

**하나의 press를 즉시 확정하면서 동시에 충돌을 존중하는 것은 불가능하다.** 둘 중 하나다.

- **(a) ε만큼 보류 후 확정**: 각 press를 ε 동안 붙들었다가 충돌 여부를 확정 → 일어서기
  피드백에 ε만큼 지연이 붙는다.
- **(b) raw 입력을 즉시 브로드캐스트하고 나중에 권위 시각으로 보정**: 일어서기 애니메이션은
  즉각적이지만, 보정 전까지 **라이브 순번과 최종 순위가 달라질 수 있다**("먼저 눌렀는데
  꼴등"이라는 UX 절벽). 보정 시점을 라운드 종료까지 미루면 절벽이 크고, 충돌 윈도우 종료
  즉시로 당기면 작아진다.

이 트레이드오프가 곧 Q3이며, **(b) 즉시 브로드캐스트 + 충돌 즉시 통보**로 결정됐다(조율 결과
Q3). 충돌 확정은 윈도우(300ms) 경과 후라 통보가 약간 늦지만, 라운드 끝까지 미루지 않고 충돌
즉시 화면을 보정한다.

## FE/BE 조율 결과 (전부 확정)

본래 열린 질문이던 항목이 모두 확정되었다. 아래는 최종 결정과 근거다.

| #  | 항목 | 결정 | 근거 |
|----|------|------|------|
| Q1 | 권위 타임스탬프 신호 | **서버 WS 수신 `Instant`** | 컨트롤러에서 스트림 발행 직전 즉시 찍혀(BlockStacking 현행 패턴) 스트림·컨슈머 지연에 무관. 클라 RTT 지터는 윈도우(300ms)가 흡수 |
| Q2 | 동시 판정 윈도우 기준점 | **충돌 첫 입력 anchor + 300ms 고정** | 그룹은 첫 입력부터 300ms(연쇄로 무한 확장 방지). 윈도우 밖 단독 입력은 충돌 아님 → 자기 순서대로 정상 |
| Q3 | 라이브 표시·충돌 통보 | **(b) 즉시 브로드캐스트 + 충돌 즉시 통보** | 누른 즉시 일어서기 표시 → 그룹 충돌 확정 시 바로 통보(재정립) → 종료 시 최종 순위. 충돌 확정은 윈도우 경과 후라 통보에 약간 지연 내재 |
| Q4 | 다중 충돌 그룹 | **그룹별 분리·발생 순서 스택**(결정 2) | 먼저 충돌이 더 나쁨 |
| Q5 | 미입력(timeout) 순위 | **제일 나쁨(제일 꼴등)**(결정 2·5) | timeout이 가장 나쁜 결과 |
| Q6 | 클라이언트 `pressedAt` | **안티치트·보조용만**, 권위는 서버(결정 4) | 향후 이상 탐지·관전 보정 여지. 권위로 승격 금지 |
| Q7 | 충돌 후 공유 카운터 | **reset(그 숫자부터) + 쿨다운 재개**(결정 6) | 화면 숫자 ≈ 등수 유지, 루프 self-limiting |

## 구현 노트 (메커니즘 확정 — 2026-06-21)

조율로 정한 정책(Q1~Q7)을 실제 코드로 옮길 때의 메커니즘을 확정한다. 정책은 "무엇을", 이
절은 "어떻게"를 정의한다.

### N1. 입력 전송 — 단일 Redis Stream 순차처리 + 서버 `Instant` 페이로드

방 입장과 동일하게 **스트림 하나를 컨슈머 하나가 도착 순서대로** 처리한다. 스트림을 다시
정렬하지 않는다. 대신 각 `press` 이벤트 페이로드에 **컨트롤러가 스트림 발행 직전 찍은 서버
수신 `Instant`**(결정 4·Q1)를 실어, 충돌 윈도우 판정에만 이 `Instant`를 쓴다(도착 시각이
아니라).

도착 순서가 `Instant` 순서와 갈리는 경우는 **두 입력이 수십 ms 안쪽일 때뿐**이고, 그 구간은
어차피 300ms 윈도우 안이라 **충돌(동점)**로 귀결된다 — 순서가 뒤집혀도 결과가 같으므로
재정렬이 불필요하다. 초 단위로 벌어진 정상 입력끼리는 도착 순서 = 누른 순서다.

### N2. 충돌 판정 — 번호별 300ms 윈도우 타이머(디바운스)

`Instant` 정렬을 위한 버퍼링이 아니라, **번호마다 윈도우 타이머**를 둔다(`FlowScheduler` 재사용).

1. 번호 N의 **첫 press**가 들어오면 → 즉시 `stand` 브로드캐스트(Q3-b), N의 윈도우를
   `anchor.Instant + 동시윈도우(300ms)`까지 연다.
2. 윈도우 안에 또 누가 누르면(=`anchor.Instant`와의 차가 윈도우 이내) → **충돌**: 그 그룹 전원
   탈락, N reset, `COLLISION_COOLDOWN` 진입.
3. 윈도우가 입력 없이 닫히면 → N을 anchor가 **solo 확정**, 다음 press부터 N+1.

`currentNumber`는 N의 윈도우가 **클린하게 닫힐 때까지 N+1로 전진하지 않는다**(윈도우 안에 들어온
입력을 N+1로 오분류하지 않기 위함).

### N3. 충돌자 = 탈락(재시도 없음)

충돌한 사람은 **충돌 계층에 고정되고 다시 누를 수 없다**(1인 1press). reset된 번호는 **아직
누르지 않은 다른 사람**이 이어서 차지한다("재개"의 주체는 충돌자가 아니라 남은 사람들). 종료
조건 "전원 입력 완료"(결정 5)는 `(solo) + (충돌) == 전체 인원`으로 판정한다. 2명이 서로 충돌해
남는 사람이 없어도 이 조건으로 즉시 종료되어 무한 쿨다운이 없다.

### N4. `NunchiScore` — 전용 Comparator 폐기, 단일 `long` 밴드 인코딩

`MiniGameScore.equals()`/`compareTo()`가 둘 다 `getValue()` 단일 `long` 기반이고
`MiniGameResult.calculateRank`가 동점 판정에 `equals()`를 쓰므로, **별도 `Comparator`를
넘기면 정렬과 동점판정이 어긋난다**(결정 3 주의문). `SpeedTouchScore`(완주/DNF를 `DNF_BASE`
오프셋으로 단일 `long`에 packing)를 선례로, 3계층을 한 `long`에 밴드 인코딩하고
`fromAscending`(작을수록 좋음)으로 정렬한다.

| 계층 | `getValue()` | 효과 |
|------|--------------|------|
| 정상(solo) | `pressInstantMs` | 빠를수록 작음 = 좋음, 서로 다른 값 |
| 충돌 | `COLLISION_BASE + (MAX - collisionInstantMs)` | 늦게 충돌 = 작음 = 덜 나쁨, **같은 그룹 = 동값(동점)** |
| 미입력 | `MISS_VALUE`(상수) | 전부 동값 = 동점, 최악 |

밴드는 `pressInstantMs < COLLISION_BASE < MISS_VALUE`로 벌려 계층을 자동 분리한다. 단일
`long`이라 consistent-with-equals가 자동 보장된다. **함정**: 정상은 빠를수록 좋지만 충돌은 늦게
충돌할수록 덜 나쁘므로, 충돌 계층만 시각을 역전(`MAX - instant`)한다.

검증(결정 3 예시): 충돌그룹1(A·B, t1 먼저) < 충돌그룹2(C·D, t2 나중)에서
`value_CD = BASE+(MAX-t2) < value_AB = BASE+(MAX-t1)`(t2>t1) → C·D가 상위. 정상·미입력 포함
`{C:1, D:2, E:2, A:4, B:4, F:최하}`가 그대로 재현된다.

### N5. 늦은 도착 입력 — 과거 번호 불변(비목표)

RTT가 윈도우를 넘겨 늦게 도착한 입력은 **도착 시점의 현재 번호에 적용될 뿐, 이미 확정된 과거
번호를 재정렬하지 않는다**(단일 스트림 순차처리라 구조적으로 그렇게 된다). 이 RTT 왜곡은
트레이드오프 절에서 감수하기로 한 항목이다 — **과거 슬롯 재배치 로직을 구현하지 않는다.**

### N6. "이미 누름" 상태 — 재입력 차단 + FE 비활성화 데이터

- BE는 **`acted` 집합**(solo·충돌 합집합)을 들고, 여기 속한 사람의 재입력은 **warn 로그만 남기고
  무시**한다(에러 응답·재전송 없음 — 결정 1 일관). "이미 클릭하였습니다"는 BE 에러가 아니라 FE
  자체 비활성화로 처리한다.
- FE 버튼 비활성화를 위해 브로드캐스트에 **누가 입력했는지**를 싣는다: `stand`에 누른 사람 이름,
  `COLLISION_COOLDOWN`에 `collided` 리스트(이미 결정 8에 포함).
- 시작 시 `currentNumber = 1`. **idle 데드라인은 게임 시작 시각부터** 돈다(아무도 누르지 않아도
  종료되도록). **유효 입력**(idle 리셋 대상) = `acted`에 새로 추가되는 press(solo·충돌 모두).
  쿨다운 동안 idle 일시정지, `resumeAtEpochMs` 이전 무시된 press는 idle을 리셋하지 않는다.
- **식별자는 닉네임(방내 유니크 불변식 전제)**: `stand`·`collided` 등 모든 브로드캐스트는 닉네임
  문자열로 사람을 식별한다. 방 입장 시 같은 닉네임을 막아 **방내 닉네임이 유니크**하고, `Gamer`
  식별자도 `(name, userId)`이며 6개 게임 결과가 전부 name-keyed(`MiniGameResult.toRankMap()` →
  `Map<String, Integer>`)다. 따라서 nunchi 전용 식별자를 새로 싣지 않는다 — FE의 `name === myName`
  매칭은 이 불변식 위에서 안전하다. (이 전제가 깨지면 nunchi만이 아니라 전체 결과 시스템이 깨진다.)

### N7. 결과 응답 DTO에 계층(tier) 노출 — rank만으론 충돌/미입력 구분 불가

`MiniGameResult`는 standard-competition rank(예: `1,2,2,4,6`)만 준다. **rank 숫자만으로는 어떤
동점 그룹이 "충돌"인지 "미입력"인지 FE가 알 수 없다.** 따라서 nunchi **결과 응답 DTO에 각 사람의
계층(`SOLO`/`COLLISION`/`MISS`)을 함께 노출**해, FE가 같은 rank를 묶고 충돌·미입력 배지를 그리게
한다. `NunchiScore`가 밴드로 이미 계층을 알고 있어 도출은 간단하다.

이는 **결정 3 위반이 아니다** — 결정 3이 금지한 건 *도메인* `MiniGameResult`를 대체하는 새 결과
타입이지, *UI 응답 DTO*는 게임마다 다르다(`MiniGameScoresResponse` 등 선례). 3계층 결과 화면은
기존 `MiniGameResultPage`(단순 rank 나열)를 재사용할 수 없는 신규 전용 뷰다.

**전송 방식(확정)**: 공유 `MiniGameRanksResponse`/`MiniGameScoresResponse`(6게임 공용, `tier`
없음)를 건드리지 않고 **nunchi 전용 REST 엔드포인트**를 둔다. 기존 결과 조회가 REST
(`/minigames/ranks`·`/minigames/scores`)인 선례를 따른다.

```json
// GET /minigames/nunchi/result?joinCode={joinCode}
// 필드명은 기존 결과 DTO와 동일하게 playerName 사용(name 아님). rank는 MiniGameResult
// standard-competition(1,2,2,4,6), tier는 NunchiScore 밴드에서 도출.
{
  "results": [
    { "playerName": "민수", "rank": 1, "tier": "SOLO" },
    { "playerName": "철수", "rank": 2, "tier": "COLLISION" },
    { "playerName": "영희", "rank": 2, "tier": "COLLISION" },
    { "playerName": "지훈", "rank": 4, "tier": "MISS" }
  ]
}
```

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|------|------|
| Consumer 처리 순서를 곧 순위로 사용 | 구현 단순, 추가 타임스탬프 불필요 | 비동기 스트림이라 처리 순서 ≠ 누른 순서, 동시 판정 불가 → 게임 규칙 위반 |
| 클라이언트 탭 타임스탬프를 권위로 채택 | 유저 의도를 가장 정확히 반영 | 시계 스큐·조작 가능, 서버 확정 선례(0002/0003) 위반 |
| 신규 `NunchiResult` 결과 타입 도입 | 충돌·꼴등 규칙을 전용 타입에 명시 | `MiniGameResult` 동점 메커니즘과 중복, 6개 게임의 단일 변환 지점(`toRankMap`) 깨짐 |
| 충돌 개념 제거 — 처리 순서대로 단순 순위 | 가장 단순, 타이밍 고민 불필요 | "동시 = 꼴등"이 게임 규칙의 정체성이라 제거 시 다른 게임이 됨 |

## 트레이드오프

**감수한 것들**

- 권위 타임스탬프가 서버 WS 수신 시각이라(Q1) 클라이언트별 RTT 차이만큼 순위에 왜곡이
  남는다. 윈도우(300ms)가 이를 일부 흡수하지만 완전히 제거하진 못한다.
- 라이브 즉시 브로드캐스트(Q3)라 충돌 확정(윈도우 경과) 전까지 라이브 순번과 최종 순위가
  갈릴 수 있다. 충돌 즉시 통보로 보정하되 결과 화면 UX를 별도 설계한다.
- idle 타임아웃 종료라 라운드 길이가 가변이다. 하드 안전 캡으로 상한만 보장한다.

**얻은 것들**

- 게임 통합·순위 표현이 기존 자산(SPI, `MiniGameResult`, Flow)을 그대로 재사용하므로
  구조 리스크가 없다.
- 타이밍 판정이라는 어려운 문제를 권위 신호·윈도우·라이브 정책으로 FE/BE가 합의 완료했다.

## 결과

조율이 완료되어 아래 항목으로 구현한다.

- `MiniGameType`에 `NUNCHI_GAME` 추가
- `nunchi/` 패키지 신설: `NunchiGame`(`Playable`), `NunchiScore` + 전용 `Comparator`,
  `NunchiFlowOrchestrator`, `NunchiNotifier`, `NunchiCommandService`,
  `NunchiGameFactory`(`MiniGameFactory`)
- 입력 엔드포인트: `/app/room/{joinCode}/nunchi/press` (전용, `CommandType` 미사용)
- 일어서기 브로드캐스트 토픽: `/topic/room/{joinCode}/nunchi/stand`
- 상태 브로드캐스트 토픽: `/topic/room/{joinCode}/nunchi/state` (PLAYING/COLLISION_COOLDOWN/DONE;
  `idleDeadlineEpochMs`·`hardCapEpochMs`·`resumeAtEpochMs` 등 모든 타이밍을 epoch ms로 — 결정 8).
  모든 state/stand에 `serverNowEpochMs`(시계 스큐 보정), PLAYING에 `stood`+`currentNumber`(재접속
  스냅샷), stand에는 rank 미포함 — 구현 노트 참조
- 순위 표현: `MiniGameResult` 재사용(새 도메인 타입 없음) — 정상 단독(누른 순) > 충돌(그룹별 스택) >
  미입력(제일 꼴등). `NunchiScore`는 단일 `long` 밴드 인코딩(구현 노트 N4). 결과 응답 DTO에는
  계층(`SOLO`/`COLLISION`/`MISS`)을 노출(N7)
- 타이밍 설정(외부화): `nunchi.timing.*` — 동시 윈도우(300ms)·충돌 쿨다운·무입력 idle 타임아웃
  (예 10초)·하드 안전 캡(예 30초), 전원 입력 시 `raceTimeout` 조기 종료
- 입력 처리 메커니즘: 단일 Redis Stream 순차처리 + 번호별 300ms 윈도우 타이머, 충돌자 탈락,
  `acted` 집합으로 재입력 차단·FE 비활성화 데이터 동봉 — 상세는 구현 노트 N1~N6
- 권위 타임스탬프·클러스터링·라이브 브로드캐스트 정책: 미결 Q1·Q2·Q3·Q6 합의 결과 반영
