# 0002. 넘버포커 게임 설계

- 날짜: 2026-03-29
- 상태: 승인 (실시간 확률 조정 방식 보류)

## 컨텍스트

기존 미니게임(카드게임, 레이싱, 스피드터치 등)은 모두 플레이어 간 점수 경쟁 방식이다.
새로 추가할 넘버포커는 **플레이어 vs 딜러** 구조로, 기존 미니게임과 성격이 다르다.

서비스 특성상 최대 9명이 동시에 플레이하고 모바일 환경이 주 플랫폼이다.
미니게임 결과는 룰렛 확률에 반영되는 구조이므로, 게임 결과를 기존 확률 조정 파이프라인에 연결해야 한다.
3라운드 단위로 구성하여 단순 1회 게임보다 긴장감 있는 흐름을 만든다.

## 게임 흐름 (PM / 기획 관점)

### 전체 흐름 요약

```text
[카드 배분] → [1단계: 내 패 공개] → [2단계: 딜러 패 1장 공개] → [결과: 딜러 패 전체 공개]
→ 라운드 종료 → [라운드 준비 확인] → (다음 라운드 반복, 총 3라운드) → 최종 결과
```

### 상세 흐름

**1. 카드 배분 (LOADING)**

- 플레이어 전원에게 각 2장, 딜러에게 2장을 나눠준다.
- 딜러 패는 전부 뒤집혀 있다 (블라인드).

**2. 1단계 결정 (STAGE_1) — 4초**

- 플레이어는 자신의 패 2장만 볼 수 있다. 딜러 패는 보이지 않는다.
- 정보 없는 상태에서 **폴드(포기) or 계속 진행**을 선택한다.
- 4초 안에 행동하지 않으면 자동으로 계속 진행 처리된다.

**3. 2단계 결정 (STAGE_2) — 8초**

- 딜러 패 1장이 공개된다.
- 부분 정보를 보고 **폴드 or 계속 진행**을 다시 선택한다.
- 1단계에서 폴드한 플레이어는 이 단계를 건너뛰고 대기 화면으로 전환된다.

**4. 결과 공개 (SHOWDOWN)**

- 딜러 패 2장이 모두 공개된다.
- 2단계까지 남은 플레이어의 패와 딜러 패를 자동으로 비교한다.
- 폴드한 플레이어는 자동으로 FOLD 결과를 받는다.

**5. 라운드 결과 (SCORE_BOARD)**

- 각 플레이어의 결과(WIN / FOLD / LOSE)와 확률 변동량을 표시한다.
- 마지막 라운드(3라운드)이면 게임 종료 후 룰렛 확률에 반영된다.
- 마지막 라운드가 아니면 라운드 준비 단계(ROUND_READY)로 넘어간다.

**6. 라운드 준비 확인 (ROUND_READY)**

- 다음 라운드 시작 전 전원이 준비 완료를 확인하는 단계다.
- 각 플레이어가 **준비 완료** 버튼을 누르면 레디 상태로 전환된다.
- 전원 레디 완료 시 타이머와 무관하게 즉시 다음 라운드 LOADING으로 진행된다.
- 타이머(5초) 만료 시 레디 여부와 무관하게 다음 라운드로 자동 진행된다.

### 엣지 케이스

| 상황 | 처리 방식 |
|------|----------|
| STAGE_1에서 전원 폴드 | STAGE_2, SHOWDOWN 건너뛰고 바로 SCORE_BOARD |
| 흡수자(WIN·TIE)가 없거나 전원 동일 결과 | 해당 라운드 전원 확률 변동 없음 |
| 딜러와 동점 (TIE) | WIN이 있으면 TIE는 변동 없음, WIN이 없으면 TIE가 증가분 흡수 |
| STAGE_1_FOLD와 STAGE_2_FOLD만 존재 | STAGE_1_FOLD가 흡수자 → 확률 감소, STAGE_2_FOLD는 +step×0.6 |
| STAGE_2_FOLD만 존재 (LOSE·WIN·TIE·STAGE_1_FOLD 없음) | 흡수자 없음 → 전원 변동 없음 |
| ROUND_READY 타이머 만료 시 미레디 플레이어 존재 | 레디 여부 무관하게 다음 라운드 자동 진행 |
| 마지막 라운드(3라운드) SCORE_BOARD | ROUND_READY 없이 게임 종료 |

## 결정

### 1. 게임 방식: 플레이어 vs 딜러 (개인전)

9명이 딜러와 각자 1:1로 배틀한다. 플레이어끼리 직접 경쟁하지 않는다.
9명이 동시에 독립적으로 행동할 수 있어 턴제 대기 없이 모바일에서 빠르게 진행된다.

### 2. 카드 구성

- **카드 범위**: 숫자 1~10 (A는 1로 표시, J·Q·K 없음)
- **문양 없음**: 숫자만으로 패를 구성한다
- **덱**: 각 숫자 4장 × 10종 = 40장
- **핸드**: 플레이어 2장, 딜러 2장

### 3. 족보 (단순 2장 비교)

```text
1순위: 페어 (같은 숫자 2장) — 페어끼리는 숫자 높은 쪽이 이김
2순위: 하이카드 (다른 숫자 2장) — 높은 숫자 비교, 같으면 낮은 숫자 비교
TIE: 두 패가 완전히 동일한 경우에만 발생
```

문양이 없으므로 TIE는 드물지만 완전히 제거할 수 없다.
(예: 플레이어 [5,5] vs 딜러 [5,5], 플레이어 [8,3] vs 딜러 [8,3])

### 4. 단계 기반 폴드 구조

시간 기반 대신 **정보량 기반** 구조를 선택했다.

| 단계 | 딜러 공개 수 | 타이머 | 폴드 의미 |
|------|------------|--------|---------|
| STAGE_1 | 0장 | 4초 | 정보 없이 포기 |
| STAGE_2 | 1장 | 8초 | 부분 정보 보고 포기 |
| SHOWDOWN | 2장 | 없음 | 자동 승패 결정 |

두 단계 모두 폴드 결과는 동일 등급(FOLD)으로 처리한다.

### 5. 확률 조정 공식

기존 `ProbabilityCalculator`를 거치지 않고 `NumberPokerProbabilityAdjuster`가 변동량을 계산하고,
Application Layer에서 `player.updateProbability()`를 호출한다.
기존 확률 시스템에 영향을 주지 않으며, 넘버포커 전용 4단계 결과를 그대로 표현할 수 있다.

```text
라운드당 step = adjustmentStep / roundCount  (= adjustmentStep / 3)

결과별 변동량:
  STAGE_1 FOLD → +(step × 0.3)
  STAGE_2 FOLD → +(step × 0.6)
  LOSE         → +(step × 1.0)

흡수자 결정 (우선순위):
  1순위: WIN
  2순위: TIE (WIN 없을 때)
  3순위: STAGE_1_FOLD (WIN·TIE·LOSE 없고 STAGE_2_FOLD가 있을 때) → STAGE_1_FOLD는 증가 대신 흡수
  4순위: FOLD 전체 (WIN·TIE 없고 LOSE가 있을 때) → FOLD는 증가 대신 흡수
  흡수자 없거나 전원 동일 결과 → 해당 라운드 전원 변동 없음

흡수자 변동량 = -(증가분 총합 ÷ 흡수자 인원수)
TIE (비흡수자, WIN 존재 시) → 변동 없음
```

케이스별 예시: 4명, adjustmentStep = 300, 라운드당 step = 100

```text
케이스 1 (WIN 존재): A=WIN, B=TIE, C=STAGE_1_FOLD, D=LOSE
  증가분 = 30 + 100 = 130
  A: -130   B: 0   C: +30   D: +100

케이스 2 (WIN 없음, TIE 흡수): A=TIE, B=TIE, C=STAGE_2_FOLD, D=LOSE
  증가분 = 60 + 100 = 160
  TIE 2명 흡수: 각 -80
  A: -80   B: -80   C: +60   D: +100   합계 = 0 ✓

케이스 3 (WIN·TIE 없음, FOLD 흡수): A=STAGE_1_FOLD, B=STAGE_2_FOLD, C=LOSE, D=LOSE
  LOSE 증가분 = 100 + 100 = 200
  FOLD 2명 흡수: 각 -100
  A: -100   B: -100   C: +100   D: +100   합계 = 0 ✓

케이스 5 (STAGE_1_FOLD 흡수): A=STAGE_1_FOLD, B=STAGE_1_FOLD, C=STAGE_2_FOLD, D=STAGE_2_FOLD
  WIN·TIE·LOSE 없고 STAGE_2_FOLD 있음 → STAGE_1_FOLD가 흡수자
  STAGE_2_FOLD 증가분 = 60 + 60 = 120
  STAGE_1_FOLD 2명 흡수: 각 -60
  A: -60   B: -60   C: +60   D: +60   합계 = 0 ✓

케이스 6: 전원 LOSE, 전원 STAGE_2_FOLD, 또는 흡수자 없음 → 전원 변동 없음
```

**1% 하한선 보장 (Application Layer)**

`NumberPokerProbabilityAdjuster`는 순수 변동량만 계산한다. 실제 적용 시 플레이어 확률이
최솟값(100 = 1%) 미만으로 내려가지 않도록 Application Layer에서 보정한다.

```text
적용값 = max(MIN_PROBABILITY, currentProbability + delta)
MIN_PROBABILITY = 100  (Probability 단위 기준 1%)
```

룰렛이 상대값(전체 합 기준 비율) 방식으로 동작하므로,
하한선 보정으로 총합이 10000에서 소폭 벗어나도 룰렛 결과에 영향 없다.
`Probability` 도메인 객체의 상한(10000) 제약은 그대로 유지한다.

### 6. 설정 구조

타이밍과 확률 배수는 yml로 관리하고, **라운드 수는 호스트가 게임 시작 전 직접 설정**한다.
`adjustmentStep / roundCount` 공식으로 라운드 수에 관계없이 총 확률 조정량이 일정하게 유지된다.

**application.yml** (서버 고정값)

```yaml
number-poker:
  timing:
    stage1: 4000ms
    stage2: 8000ms
    round-ready: 5000ms
  probability:
    stage1-fold-multiplier: 0.3
    stage2-fold-multiplier: 0.6
```

**클라이언트 라운드 수 설정 (호스트 전용)**

```text
발행: /app/room/{joinCode}/poker/settings

{ "roundCount": 3 }
```

- 호스트만 전송 가능하며, 게임 시작 전에만 유효하다.
- 허용 범위: 1~5. 서버는 범위 초과 시 요청을 무시한다.
- 수신 후 설정값을 전체 브로드캐스트한다.
- 라운드 수가 클수록 라운드당 확률 변동 폭이 줄고 게임이 길어진다.

**Java Properties 클래스** (yml 바인딩)

```java
@Validated
@ConfigurationProperties(prefix = "number-poker")
public record NumberPokerProperties(
    @Valid Timing timing,
    @Valid Probability probability
) {
    public record Timing(
        @NotNull @DurationMin(nanos = 1) Duration stage1,
        @NotNull @DurationMin(nanos = 1) Duration stage2,
        @NotNull @DurationMin(nanos = 1) Duration roundReady
    ) {}

    public record Probability(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double stage1FoldMultiplier,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double stage2FoldMultiplier
    ) {}
}
```

`stage1FoldMultiplier`, `stage2FoldMultiplier`는 각 폴드 결과의 확률 증가 배수다.
두 값은 `stage1FoldMultiplier < stage2FoldMultiplier < 1.0` 관계가 유지되어야 한다.

### 8. 백엔드 도메인 구조

기존 미니게임(`CardGame` 등)은 `Playable` 인터페이스를 구현해 `MiniGameResult` 파이프라인을 거친다.
넘버포커도 `Playable`을 구현하되, **라운드마다 실시간으로 확률이 조정**되므로 게임 종료 후 추가 확률 조정은 없다.
이를 위해 `Playable`의 `shouldAdjustProbabilities()`를 `false`로 오버라이드한다.
`Room.applyMiniGameResult(Playable)`은 이 플래그를 확인해 확률 조정 단계를 건너뛴다.

라운드별 확률 조정은 `NumberPokerProbabilityAdjuster`가 전담하며 기존 `ProbabilityCalculator`를 거치지 않는다.
`getResult()`는 전원 1위(공동 순위)를 반환하고, `getScores()`는 0점을 반환한다 — DB 기록 목적으로만 사용된다.

`PokerPhase`(현재 게임 페이즈)는 `NumberPokerGame`(Aggregate Root)이 보유한다.
`PokerRound`는 라운드 내 데이터(패, 폴드 여부, 딜러)만 책임지며 페이즈 상태를 갖지 않는다.

```text
numberpoker/
├── domain/
│   ├── NumberPokerGame        ← Aggregate Root (Playable 구현, 라운드 관리, PokerPhase 보유)
│   ├── NumberPokerScore       ← MiniGameScore 구현체 (getValue()=0, DB 기록 전용)
│   ├── PokerRound             ← 라운드 1개의 데이터 (패, 폴드, 딜러, 준비 상태)
│   ├── Dealer                 ← 딜러 패 + 단계별 공개 관리
│   ├── PlayerPokerHand        ← 플레이어 패 + ACTIVE/FOLDED 상태
│   ├── PokerCard              ← 카드 단위 (숫자 1~10)
│   ├── HandRanking            ← 패 강도 비교 (페어 > 하이카드)
│   ├── PokerRoundResult       ← WIN / STAGE_1_FOLD / STAGE_2_FOLD / LOSE / TIE
│   ├── PokerPhase             ← LOADING/STAGE_1/STAGE_2/SHOWDOWN/SCORE_BOARD/ROUND_READY
│   └── NumberPokerProbabilityAdjuster ← 라운드 결과로 변동량 계산 (적용은 Application Layer)
├── application/
├── infra/
└── ui/
```

### 7. WebSocket 이벤트 흐름

```text
구독 (서버 → 클라이언트)
  /topic/room/{joinCode}/poker/state  ← 통합 게임 상태 (페이즈 변경 + 라운드 결과 포함)
  /user/queue/poker/hand              ← 내 카드 2장 (나만 받는 개인 메시지, LOADING 시 1회)

발행 (클라이언트 → 서버)
  /app/room/{joinCode}/poker/settings ← 라운드 수 설정 (호스트 전용, 게임 시작 전)
  /app/room/{joinCode}/poker/fold     ← 폴드 액션 (STAGE_1·STAGE_2에서만 유효)
  /app/room/{joinCode}/poker/ready    ← 라운드 준비 완료 (ROUND_READY에서만 유효)

페이즈별 서버 전송 내용
  LOADING      : /user/queue/poker/hand → 내 카드 2장
                 /topic/.../poker/state → phase=LOADING, 딜러 뒷면 2장, 플레이어 목록
  STAGE_1      : /topic/.../poker/state → phase=STAGE_1, timerSeconds=4
  STAGE_1 중   : 플레이어 폴드 발생 시 → /topic/.../poker/state 즉시 재전송
                 (phase=STAGE_1 유지, playerStatuses만 갱신)
  STAGE_2      : /topic/.../poker/state → phase=STAGE_2, timerSeconds=8, 딜러 1장 공개
  STAGE_2 중   : 플레이어 폴드 발생 시 → /topic/.../poker/state 즉시 재전송
                 (phase=STAGE_2 유지, playerStatuses만 갱신)
  SHOWDOWN     : /topic/.../poker/state → phase=SHOWDOWN, 딜러 2장 전체 공개
  SCORE_BOARD  : /topic/.../poker/state → phase=SCORE_BOARD, roundResult 포함
  ROUND_READY  : /topic/.../poker/state → phase=ROUND_READY, timerSeconds=5, readyStatuses 포함
                 (레디 발생 시마다 readyStatuses 갱신 메시지 재전송)

타이머 만료 전 행동 없으면 자동으로 계속 진행 처리 (서버 주도)
전원 레디 완료 시 타이머와 무관하게 즉시 다음 라운드로 진행 (조기 진행)
```

### 8. 모바일 UI 방향

9명을 동등하게 표시하는 원형 테이블 대신 **자신 중심 레이아웃**을 채택한다.

- **내 영역 (화면 하단 60%)**: 내 패 2장 크게 표시, 폴드/계속 버튼
- **딜러 영역 (화면 상단 중앙)**: 딜러 패 2장 (뒤집힌 상태 → 단계별 공개)
- **다른 플레이어 (상단 좌우)**: 아이콘 + ACTIVE/FOLDED 상태 compact 표시
  - 폴드 발생 시 해당 플레이어 아이콘에 즉시 FOLDED 표시 (서버 재전송 수신 시점)
  - STAGE_1·STAGE_2 진행 중 남은 ACTIVE 인원 수를 확인할 수 있음
- **폴드한 플레이어**: 다음 라운드 대기 화면으로 전환 (차후 관전 기능 검토)

딜러 패가 한 장씩 뒤집히는 순간이 핵심 긴장감 포인트이므로,
다른 플레이어 UI는 최소화하여 딜러 공개 애니메이션에 집중한다.

## 프론트엔드 연동 가이드

### 연결 구조 한눈에 보기

```text
구독 (서버 → 클라이언트)
  /topic/room/{joinCode}/poker/state    ← 통합 게임 상태 (페이즈 변경 + 라운드 결과)
  /user/queue/poker/hand                ← 내 카드 2장 (나만 받는 개인 메시지, LOADING 1회)

발행 (클라이언트 → 서버)
  /app/room/{joinCode}/poker/settings   ← 라운드 수 설정 (호스트 전용, 게임 시작 전)
  /app/room/{joinCode}/poker/fold       ← 폴드 액션 (STAGE_1·STAGE_2, body 없음)
  /app/room/{joinCode}/poker/ready      ← 라운드 준비 완료 (ROUND_READY, body 없음)
```

> 기존 CardGame의 `/topic/.../gameState` 단일 토픽 패턴과 동일한 구조다.
> `/user/queue/poker/hand`는 STOMP 세션 기준으로 본인에게만 전송되는 개인 메시지다.

---

### 페이즈 흐름

```text
              [라운드 시작]
                   │
              LOADING ───────────── /user/queue/poker/hand 수신 (내 카드 2장)
                   │
            STAGE_1 (4초) ───────── 폴드 버튼 활성화
                   │
           폴드 or 대기
          ┌────────┴────────┐
        FOLDED           ACTIVE
          │                 │
     대기 화면          STAGE_2 (8초) ── 딜러 카드 1장 공개, 폴드 버튼 활성화
                             │
                        폴드 or 대기
                       ┌─────┴──────┐
                     FOLDED       ACTIVE
                       │             │
                  대기 화면        SHOWDOWN ── 딜러 카드 전체 공개, 승패 결정
                                      │
                                 SCORE_BOARD ── 결과 및 확률 변동 표시
                                      │
                          ┌───────────┴───────────┐
                     isFinalRound=true        isFinalRound=false
                          │                        │
                     [게임 종료]             ROUND_READY (5초) ── 준비 완료 버튼
                                                   │
                                        전원 레디 or 타이머 만료
                                                   │
                                            [다음 라운드]
```

---

### TypeScript 타입 정의

```typescript
// 카드
interface PokerCard {
  value: number; // 1~10
}

// 플레이어 상태
interface PlayerStatus {
  playerName: string;
  status: 'ACTIVE' | 'FOLDED';
}

// 라운드 결과 (SCORE_BOARD 단계에서만 채워짐)
interface PlayerRoundResult {
  playerName: string;
  result: 'WIN' | 'LOSE' | 'STAGE_1_FOLD' | 'STAGE_2_FOLD' | 'TIE';
  cards: PokerCard[]; // 라운드 종료 시 폴드 포함 전원 공개 (항상 2장)
  probabilityChange: number; // 양수=증가(불리), 음수=감소(유리), 0=변동없음
}

interface RoundResult {
  isFinalRound: boolean; // true면 게임 종료 후 룰렛 확률 반영
  dealerCards: PokerCard[]; // 딜러 최종 패 2장
  playerResults: PlayerRoundResult[];
}

// ① /user/queue/poker/hand — 내 카드 (LOADING 시 1회, 개인 메시지)
interface PokerHandMessage {
  roundNumber: number; // 1~3
  cards: PokerCard[];  // 항상 2장
}

// 라운드 준비 상태 (ROUND_READY 단계에서만 사용)
interface PlayerReadyStatus {
  playerName: string;
  isReady: boolean;
}

// ② /topic/.../poker/state — 통합 게임 상태 (모든 페이즈에서 전송)
interface PokerGameState {
  phase: 'LOADING' | 'STAGE_1' | 'STAGE_2' | 'SHOWDOWN' | 'SCORE_BOARD' | 'ROUND_READY';
  roundNumber: number;                        // 1~3
  timerSeconds: number | null;                // SHOWDOWN·SCORE_BOARD는 null
  dealerRevealedCards: PokerCard[];           // LOADING·STAGE_1=[], STAGE_2=[1장], SHOWDOWN 이후=[2장]
  playerStatuses: PlayerStatus[];             // 전원의 ACTIVE/FOLDED 상태 (라운드 진행 중)
  readyStatuses: PlayerReadyStatus[] | null;  // ROUND_READY일 때만 채워짐, 나머지는 null
  roundResult: RoundResult | null;            // SCORE_BOARD일 때만 채워짐, 나머지는 null
}
```

---

### 페이즈별 렌더링 가이드

**LOADING**

- `/user/queue/poker/hand` 수신 후 내 카드 2장을 화면에 표시한다.
- 딜러 영역은 뒤집힌 카드 2장으로 표시한다.
- 폴드/계속 버튼은 비활성화 상태로 둔다.

**STAGE_1**

- `timerSeconds: 4` 기준으로 타이머 UI를 시작한다.
- 폴드 버튼을 활성화한다. 폴드 시 `/app/room/{joinCode}/poker/fold` 발행 후 버튼을 비활성화한다.
- 타이머 만료 전 아무 행동도 없으면 자동으로 STAGE_2로 넘어간다 (별도 처리 불필요).
- **같은 `phase: 'STAGE_1'`으로 state 메시지가 재수신되면 `playerStatuses`만 갱신한다.**
  누군가 폴드했을 때 서버가 즉시 재전송하므로, 해당 플레이어 아이콘을 FOLDED로 업데이트한다.

**STAGE_2**

- `dealerRevealedCards[0]`으로 딜러 카드 1장을 뒤집는 애니메이션을 재생한다.
- `timerSeconds: 8` 기준으로 타이머를 시작한다.
- 이미 FOLDED 상태인 플레이어는 대기 화면을 유지한다 (phase 메시지를 수신하더라도 무시).
- **STAGE_1과 동일하게, 같은 `phase: 'STAGE_2'`로 재수신 시 `playerStatuses`만 갱신한다.**

**SHOWDOWN**

- `dealerRevealedCards[1]`로 딜러 카드 2번째 장을 뒤집는 애니메이션을 재생한다.
- 타이머 없음, 폴드 버튼 없음.
- 카드 오픈 애니메이션이 끝난 후 SCORE_BOARD result 메시지가 도착한다.

**SCORE_BOARD**

- `roundResult.dealerCards`로 딜러 패 2장을 표시한다.
- `playerResults`의 `cards`로 모든 플레이어의 패를 공개한다. 폴드한 플레이어도 이 시점에 패가 공개된다.
- 내 `playerName`의 결과를 강조 표시한다.
- `probabilityChange`가 음수면 초록(유리), 양수면 빨강(불리), 0이면 회색으로 표시한다.
- `isFinalRound: true`이면 "게임 종료" 화면으로 전환하고 룰렛 진행을 기다린다.
- `isFinalRound: false`이면 ROUND_READY 메시지를 기다린다.

**ROUND_READY**

- `timerSeconds: 5` 기준으로 카운트다운을 시작한다.
- **준비 완료** 버튼을 표시한다. 클릭 시 `/app/room/{joinCode}/poker/ready`를 발행하고 버튼을 비활성화한다.
- `readyStatuses`로 각 플레이어의 레디 여부를 실시간으로 표시한다 (레디 발생 시마다 서버가 메시지를 재전송).
- 전원 레디 or 타이머 만료 시 서버가 다음 라운드 LOADING을 전송한다. 클라이언트는 별도 처리 불필요.

---

### 구독 및 발행 예시

```typescript
// 라운드 수 설정 (호스트 전용, 게임 시작 전)
stompClient.publish({
  destination: `/app/room/${joinCode}/poker/settings`,
  body: JSON.stringify({ roundCount: 3 }), // 1~5
});

// 구독 설정 (게임 시작 시 1회)
stompClient.subscribe(`/topic/room/${joinCode}/poker/state`, (message) => {
  const state: PokerGameState = JSON.parse(message.body);
  renderByPhase(state); // phase 값 하나로 렌더링 분기
});

stompClient.subscribe(`/user/queue/poker/hand`, (message) => {
  const hand: PokerHandMessage = JSON.parse(message.body);
  setMyCards(hand.cards); // 내 카드 저장
});

// 폴드 발행 (STAGE_1·STAGE_2에서 버튼 클릭 시)
stompClient.publish({
  destination: `/app/room/${joinCode}/poker/fold`,
  // body 없음
});

// 준비 완료 발행 (ROUND_READY에서 버튼 클릭 시)
stompClient.publish({
  destination: `/app/room/${joinCode}/poker/ready`,
  // body 없음
});
```

---

### 페이즈별 state 메시지 예시

**LOADING**

```json
{
  "phase": "LOADING",
  "roundNumber": 1,
  "timerSeconds": null,
  "dealerRevealedCards": [],
  "playerStatuses": [
    { "playerName": "홍길동", "status": "ACTIVE" },
    { "playerName": "김철수", "status": "ACTIVE" }
  ],
  "readyStatuses": null,
  "roundResult": null
}
```

**STAGE_1**

```json
{
  "phase": "STAGE_1",
  "roundNumber": 1,
  "timerSeconds": 4,
  "dealerRevealedCards": [],
  "playerStatuses": [
    { "playerName": "홍길동", "status": "ACTIVE" },
    { "playerName": "김철수", "status": "ACTIVE" }
  ],
  "readyStatuses": null,
  "roundResult": null
}
```

**STAGE_2** (김철수 STAGE_1에서 폴드)

```json
{
  "phase": "STAGE_2",
  "roundNumber": 1,
  "timerSeconds": 8,
  "dealerRevealedCards": [{ "value": 7 }],
  "playerStatuses": [
    { "playerName": "홍길동", "status": "ACTIVE" },
    { "playerName": "김철수", "status": "FOLDED" }
  ],
  "readyStatuses": null,
  "roundResult": null
}
```

**SHOWDOWN**

```json
{
  "phase": "SHOWDOWN",
  "roundNumber": 1,
  "timerSeconds": null,
  "dealerRevealedCards": [{ "value": 7 }, { "value": 3 }],
  "playerStatuses": [
    { "playerName": "홍길동", "status": "ACTIVE" },
    { "playerName": "김철수", "status": "FOLDED" }
  ],
  "readyStatuses": null,
  "roundResult": null
}
```

**SCORE_BOARD**

```json
{
  "phase": "SCORE_BOARD",
  "roundNumber": 1,
  "timerSeconds": null,
  "dealerRevealedCards": [{ "value": 7 }, { "value": 3 }],
  "playerStatuses": [
    { "playerName": "홍길동", "status": "ACTIVE" },
    { "playerName": "김철수", "status": "FOLDED" }
  ],
  "readyStatuses": null,
  "roundResult": {
    "isFinalRound": false,
    "dealerCards": [{ "value": 7 }, { "value": 3 }],
    "playerResults": [
      {
        "playerName": "홍길동",
        "result": "WIN",
        "cards": [{ "value": 5 }, { "value": 5 }],
        "probabilityChange": -100
      },
      {
        "playerName": "김철수",
        "result": "STAGE_1_FOLD",
        "cards": [{ "value": 1 }, { "value": 3 }],
        "probabilityChange": 30
      },
      {
        "playerName": "이영희",
        "result": "LOSE",
        "cards": [{ "value": 2 }, { "value": 3 }],
        "probabilityChange": 100
      }
    ]
  }
}
```

**ROUND_READY** (초기 진입)

```json
{
  "phase": "ROUND_READY",
  "roundNumber": 1,
  "timerSeconds": 5,
  "dealerRevealedCards": [],
  "playerStatuses": [
    { "playerName": "홍길동", "status": "ACTIVE" },
    { "playerName": "김철수", "status": "FOLDED" }
  ],
  "readyStatuses": [
    { "playerName": "홍길동", "isReady": false },
    { "playerName": "김철수", "isReady": false }
  ],
  "roundResult": null
}
```

**ROUND_READY** (홍길동 레디 후 갱신)

```json
{
  "phase": "ROUND_READY",
  "roundNumber": 1,
  "timerSeconds": 5,
  "dealerRevealedCards": [],
  "playerStatuses": [
    { "playerName": "홍길동", "status": "ACTIVE" },
    { "playerName": "김철수", "status": "FOLDED" }
  ],
  "readyStatuses": [
    { "playerName": "홍길동", "isReady": true },
    { "playerName": "김철수", "isReady": false }
  ],
  "roundResult": null
}
```

---

### 주의 사항

- **같은 phase로 state 메시지가 재수신되면 페이즈 전환이 아닌 `playerStatuses` 갱신으로 처리한다.**
  타이머를 리셋하거나 애니메이션을 다시 재생하지 않는다.
- 내 `playerStatuses` 상태가 이미 `FOLDED`이면 `phase: 'STAGE_2'`가 와도 폴드 버튼을 표시하지 않는다.
- STAGE_1에서 전원 폴드 시 서버가 STAGE_2를 건너뛰고 바로 `phase: 'SCORE_BOARD'`를 보낸다.
- `phase: 'ROUND_READY'`는 `isFinalRound: false`인 SCORE_BOARD 이후에만 온다. 마지막 라운드에는 오지 않는다.
- ROUND_READY에서 레디 버튼을 이미 눌렀다면 서버에서 갱신 메시지가 와도 버튼을 다시 활성화하지 않는다.
- `probabilityChange: 0`은 TIE이거나 전원 동일 결과 두 가지 모두 해당한다. 표시는 동일하게 회색 처리한다.
- 타이머는 서버 기준으로 동작한다. 클라이언트 카운트다운은 UX 전용이며,
  실제 단계 전환은 서버의 state 메시지 수신 시점을 기준으로 처리한다.
- 재연결 시 `/topic/.../poker/state`의 마지막 메시지로 현재 상태를 복구할 수 있다.

---

## 고려한 대안

| 대안 | 장점 | 단점 |
|------|-----|------|
| 플레이어 간 배틀 (턴제) | 전통 포커 느낌 | 9명 턴 대기로 모바일 UX 최악 |
| 시간 기반 폴드 | 빠른 포기에 인센티브 부여 | 기준이 불명확, 구현 복잡 |
| 카드 3장 + 정통 족보 | 포커 다양성 | 모바일에서 족보 파악 어려움, 구현 복잡 |
| 단일 라운드 | 구현 단순 | 게임 시간이 너무 짧고 확률 변동 폭 제한 |
| 딜러 자격 미달 규칙 (퀸 하이 미만) | 정통 포커 룰 | 전원 WIN 라운드 빈도 높아 게임 무의미해짐 |
| 원형 테이블 9인 UI | 전통 포커 느낌 | 모바일 화면에서 슬롯이 너무 작아짐 |

## 트레이드오프

**감수한 것들**

- step 상한 클램핑으로 인해 극단적인 케이스(WIN 1명, LOSE 8명)에서 총합이 정확히 0이 되지 않을 수 있다.
- 문양이 없어 TIE가 발생할 수 있다. 단, TIE는 완전히 동일한 패일 때만 발생하므로 빈도는 낮다.
- 흡수자가 없는 라운드(전원 LOSE 등)는 확률 변동이 없어 무의미한 라운드가 된다.
- 3라운드 합산 후 1회 반영 방식이므로 게임 중 실시간 확률 변동을 보여줄 수 없다 (보류).

**얻은 것들**

- 플레이어가 독립적으로 행동하므로 턴 대기 없이 9명이 동시 진행 가능하다.
- 숫자 1~10 + 2장 + 페어/하이카드 족보로 누구나 즉시 이해 가능한 단순한 게임이 된다.
- `adjustmentStep / roundCount` 공식으로 기존 확률 조정 총량을 유지하면서 라운드별 분산 처리가 가능하다.
- FOLD도 확률이 오르도록 설계하여 폴드가 항상 안전한 선택이 되는 문제를 방지한다.
- 1차 폴드가 흡수자가 될 수 있어 1차 폴드 > 2차 폴드 > LOSE 순서의 유불리가 모든 케이스에서 일관되게 유지된다.

## 보류 사항

**라운드마다 실시간 확률 조정**

라운드 종료 시마다 룰렛 확률을 즉시 반영하면 플레이어가 확률 변동을 라운드마다 체감할 수 있다.
이를 위해서는 `Room`의 `effectiveRoundCount` 계산 방식 변경과 미니게임 인터페이스에 `roundWeight` 개념 추가가 필요하며,
기존 게임 전체에 영향이 미친다. 현재는 3라운드 합산 후 1회 반영으로 진행하고, 향후 별도 ADR로 결정한다.
