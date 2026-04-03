# 블록 쌓기 (Block Stacking) 미니게임

## 게임 개요

블록이 좌우로 왕복 이동하며, 플레이어가 탭/클릭하는 순간 블록이 고정된다.
고정된 블록과 아래 블록의 겹치는 부분만 남아 다음 층의 기준이 되며,
겹치지 않는 부분은 떨어진다. 완전히 벗어나면 게임 오버.

**핵심 규칙**
- 층수가 높을수록 룰렛 당첨 확률 증가
- 점수 = 성공적으로 쌓은 층수
- 블록 너비는 층이 올라갈수록 점점 좁아짐 (겹친 부분만 유지)
- **20초 시간 제한**: 제한 시간이 다 되면 게임이 종료됨
- **복리 속도 증가**: 매 층마다 속도가 **5%씩(x1.05)** 증가하여 긴장감 유발

---

## 게임 상태 머신

```
DESCRIPTION → PREPARE → PLAYING → DONE
```

- **DESCRIPTION**: 게임 설명 슬라이드 (기존 GameIntroSlides 재사용)
- **PREPARE**: 카운트다운 오버레이 (기존 PrepareOverlay 재사용)
- **PLAYING**: 게임 진행 중
- **DONE**: 게임 오버 → result 페이지로 이동

> 현재 구현에서는 Provider가 WebSocket을 통해 게임 상태를 구독한다.
> `state`, `progress`, `complete` 이벤트를 수신하며, 상태 머신 전환과 진행 상태 반영에 사용된다.

---

## 렌더링 방식

**HTML5 Canvas + `requestAnimationFrame`**

- 왕복하는 블록: `requestAnimationFrame` 루프에서 x좌표 업데이트 후 `clearRect` → 재드로우
- 쌓인 블록: 매 프레임 전체 스택을 canvas에 다시 그림
- 잘린 부분 낙하 애니메이션: 별도 `fallingPieces` 배열 관리, y좌표 증가 + opacity 감소
- **반응형 컨테이너**: 기기 높이에 맞춰 캔버스 크기 자동 조정 (Full-Height Immersion)
- **외부 타이머**: 캔버스 우측에 세로형 에너지 바 형태로 배치
- 탭 이벤트: `pointerdown` (모바일/데스크톱 통합)

---

## 파일 구조

```
src/
├── features/miniGame/blockStackingGame/
│   ├── components/
│   │   ├── BlockStackingCanvas/
│   │   │   ├── BlockStackingCanvas.tsx      # canvas 엘리먼트 + 이벤트 바인딩
│   │   │   └── BlockStackingCanvas.styled.ts
│   │   ├── EliminatedOverlay/
│   │   │   ├── EliminatedOverlay.tsx        # 탈락/게임 종료 오버레이
│   │   │   └── EliminatedOverlay.styled.ts
│   │   ├── BlockStackingRanks/
│   │   │   ├── BlockStackingRanks.tsx       # 랭킹 섹션 컨테이너
│   │   │   └── BlockStackingRanks.styled.ts
│   │   └── BlockStackingRankList/
│   │       ├── BlockStackingRankList.tsx    # 랭킹 목록 아이템 렌더링
│   │       └── BlockStackingRankList.styled.ts
│   ├── hooks/
│   │   ├── useBlockStackingGame.ts          # 핵심 게임 루프 (canvas 드로우 + 상태)
│   │   └── useBlockStackingActions.ts       # 백엔드 연동 시 WebSocket publish 담당
│   ├── pages/
│   │   ├── BlockStackingGameReadyPage.tsx
│   │   └── BlockStackingGamePlayPage.tsx
│   └── constants/
│       └── blockStackingConstants.ts        # 블록 속도, 크기, 색상 등
│
└── contexts/BlockStackingGame/
    ├── BlockStackingGameContext.ts           # Context 정의 + useBlockStackingGameContext hook
    └── BlockStackingGameProvider.tsx         # 게임 상태 관리 (로컬 → 추후 WebSocket)
```

---

## 핵심 데이터 구조

```typescript
// 쌓인 블록 한 층
type StackedBlock = {
  x: number;      // canvas 내 좌측 시작 x
  width: number;  // 현재 층 너비
  // y 위치는 별도 필드로 저장하지 않고 카메라/스택 인덱스로 계산
};

// 낙하하는 잘린 조각
type FallingPiece = {
  x: number;
  y: number;
  width: number;
  vy: number;     // 낙하 속도
  opacity: number;
};

// Context가 제공하는 상태
type BlockStackingGameState = {
  phase: 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'DONE';
  score: number;             // 현재 층수
  timeLeft: number;          // 남은 시간 (초)
  stack: StackedBlock[];     // 쌓인 블록 목록
  currentBlock: {
    x: number;
    width: number;
    direction: 1 | -1;
    speed: number;
  };
  fallingPieces: FallingPiece[];
};
```

---

## `useBlockStackingGame` 훅 역할

canvas ref와 게임 상태를 받아 `requestAnimationFrame` 루프를 관리.

```
useBlockStackingGame(canvasRef, { phase, stack, currentBlock, fallingPieces, onTap, onGameOver })
  └── useEffect → rAF 루프 시작
        ├── 현재 블록 x 업데이트 (좌우 반전 처리)
        ├── 쌓인 블록 전체 드로우
        ├── 현재 블록 드로우
        ├── 낙하 조각 드로우 + vy 증가 + opacity 감소
        └── phase !== 'PLAYING' → rAF 중단
```

탭 발생 시:
1. 현재 블록 x와 스택 최상단 블록의 overlap 계산
2. overlap === 0 → `onGameOver()` 호출
3. overlap > 0 → 새 `StackedBlock` 추가, 잘린 부분 `FallingPiece` 추가, 속도 증가

---

## 속도 곡선

층수가 올라갈수록 블록 이동 속도가 점진적으로 증가. 체감 난이도 곡선을 위해 선형이 아닌 계단식 증가 사용.

```typescript
// blockStackingConstants.ts
export const INITIAL_SPEED = 2.2;
export const SPEED_INCREMENT = 1.05; // 5% 증가

export const getBlockSpeed = (floor: number): number => {
  const speed = INITIAL_SPEED * Math.pow(SPEED_INCREMENT, floor);
  return Math.min(speed, 12.0); // 최고 속도 제한
};
```

> **백엔드 연동 시 설계**: 속도 계산 로직을 서버와 동기화하거나, 서버에서 직접 속도 값을 내려주어 모든 플레이어가 동일한 난이도를 경험하도록 설계 권장.
```

---

## 동적 시점 (Dynamic View)

초기에는 블록을 하단에서 쌓기 시작하고, 층수가 높아짐에 따라 시점이 위로 이동하여 블록이 화면 중앙에 머물도록 한다.

### 계산 방식
- `movingBlockY`: 현재 움직이는 블록의 Y 좌표
- `targetY = H / 2` (화면 중앙)
- `initialY = H - 2 * BLOCK_HEIGHT` (화면 하단)
- **부드러운 추적**: `cameraY += (targetY - cameraY) * 0.1` (Lerp 적용)
- 쌓인 블록 `i`의 Y 좌표: `movingBlockY + (stack.length - i) * BLOCK_HEIGHT`

---

## 물리 효과 — 화면 흔들림 (Screen Shake)

블록이 정확히 안착할 때마다 canvas에 진동 효과를 주어 타격감 강화.

### 흔들림 발생 조건

| 이벤트 | 강도 | 지속 |
|--------|------|------|
| 블록 안착 (일반) | 약 (3px) | 200ms |
| 블록 안착 (퍼펙트) | 중 (6px) | 300ms |
| 게임 오버 | 강 (12px) | 500ms |

> **퍼펙트**: 현재 블록이 아래 블록과 완벽하게 정렬된 경우 (오차 ±2px 이내)

### 구현 방식

Canvas Context의 `save()` / `translate()` / `restore()`로 처리. CSS `transform`이 아닌 canvas 내부 변환을 사용해 게임 렌더링과 분리.

```
useScreenShake(intensity, duration)
  └── shakeOffset: { x, y } 반환
        └── rAF 루프에서 ctx.translate(shakeOffset.x, shakeOffset.y) 적용
              └── 매 프레임 랜덤 offset, duration 경과 후 0으로 수렴
```

```typescript
type ShakeConfig = {
  intensity: number;  // 최대 흔들림 px
  duration: number;   // ms
};

// rAF 루프 내에서
ctx.save();
ctx.translate(shakeX, shakeY); // shakeX = (Math.random() * 2 - 1) * intensity * progress
// ... 전체 드로우
ctx.restore();
```

---

## 사운드 시스템

**Web Audio API** 기반. 외부 라이브러리 없이 구현.

### 사운드 종류

| 이벤트 | 효과음 |
|--------|--------|
| 블록 안착 | 짧고 높은 타격음 (440Hz → 880Hz, 100ms) |
| 퍼펙트 안착 | 밝은 상승 효과음 (C5 → E5 → G5 아르페지오) |
| 게임 오버 | 낮게 떨어지는 하강음 (300Hz → 100Hz, 400ms) |
| 속도 구간 진입 | 짧은 알림음 (새 구간 도달 시 1회) |

### 구조

```
useBlockStackingSounds()
  ├── audioCtx: AudioContext (첫 탭 시 생성 — 브라우저 자동재생 정책 대응)
  ├── playLand()       — 안착
  ├── playPerfect()    — 퍼펙트
  ├── playGameOver()   — 게임 오버
  └── playSpeedUp()    — 속도 구간 진입
```

**브라우저 자동재생 정책 대응**: `AudioContext`는 첫 번째 `pointerdown` 이벤트 핸들러 내부에서 생성 또는 `resume()`.

```typescript
// 첫 탭 시 AudioContext 초기화
const initAudio = () => {
  if (!audioCtxRef.current) {
    audioCtxRef.current = new AudioContext();
  } else if (audioCtxRef.current.state === 'suspended') {
    audioCtxRef.current.resume();
  }
};
```

### 음소거 지원

`BlockStackingGamePlayPage`에 토글 버튼 배치. `muted` 상태는 컴포넌트 로컬 state로 관리 (전역 불필요).

---

## gameConfigs.tsx 등록

```typescript
BLOCK_STACKING: {
  Provider: BlockStackingGameProvider,
  ReadyPage: BlockStackingGameReadyPage,
  slides: [
    {
      title: '블록을 쌓아라!',
      description: '화면을 탭해서 블록을 정확히 쌓으세요.\n많이 쌓을수록 당첨 확률이 올라갑니다!',
      icon: <BlockIcon />,
    },
  ],
  PlayPage: BlockStackingGamePlayPage,
},
```

`MiniGameType` 유니온에도 `'BLOCK_STACKING'` 추가 필요.

---

## 실시간 랭킹 (Real-time Ranking)

게임 화면 **우측 상단**에 수직 리스트 형태로 각 플레이어의 현재 층수를 실시간 표시합니다. 
`RacingRanks`와 유사한 스타일로 구현되었으며, 본인의 경우 하이라이트 처리가 됩니다.

### UI 구조

```
┌──────────────────────────┐
│      Canvas (게임)       │
│  ┌────────────────────┐  │
│  │ 1. 철수 12층        │  │ ← BlockStackingRanks (수직 리스트)
│  │ 2. 영희 9층         │  │
│  └────────────────────┘  │
└──────────────────────────┘
```

### 구현 방식

`BlockStackingRanks` 컴포넌트가 Context의 `rankings`를 구독하여 실시간 정렬 후 렌더링합니다.
캔버스 위의 오버레이 형태로 배치되어 게임 몰입감을 해치지 않으면서 정보를 제공합니다.

---

## 백엔드 API 설계

### 전체 통신 흐름

```
[호스트] 게임 시작 요청 (REST)
    ↓
[서버] 모든 클라이언트에 게임 시작 브로드캐스트 (WebSocket)
    ↓
[클라이언트] 게임 진행 중 — 층수 변화 시 progress 발행 (WebSocket)
    ↓
[서버] 전체 플레이어 랭킹 브로드캐스트 (WebSocket)
    ↓
[클라이언트] 게임 오버 시 최종 점수 제출 (WebSocket)
    ↓
[서버] 모든 플레이어 완료 시 결과 브로드캐스트 (WebSocket)
    ↓
[클라이언트] REST로 최종 랭킹 조회 (기존 MiniGameResultPage 패턴)
```

---

### WebSocket — STOMP Destinations

#### 1. 게임 상태 브로드캐스트

**Subscribe** `/room/{joinCode}/block-stacking/state`

서버 → 모든 클라이언트. 게임 시작/종료 신호.

```json
{
  "state": "PREPARE" | "PLAYING" | "DONE",
  "countdown": 3
}
```

#### 2. 게임 진행 중 탭 이벤트 발행

**Publish** `/room/{joinCode}/block-stacking/progress`

클라이언트 → 서버. 블록이 성공적으로 안착할 때마다(층이 올라갈 때마다) 발행.

클라이언트는 탭 결과를 **로컬에서 즉시 처리(낙관적 업데이트)** 하여 렌더링 딜레이 없이 게임을 진행.
서버는 수신한 좌표로 overlap을 재계산하여 이상 탐지(점수 조작 등) 및 공식 점수 기록에 활용.

```json
{
  "playerName": "string",
  "floor": 7,
  "tapX": 142.5,
  "movingBlockX": 140.0,
  "stackTopX": 85.0,
  "stackTopWidth": 150.0
}
```

> **좌표 기준**: canvas 좌측 상단 원점(0, 0) 기준 픽셀 값. 클라이언트가 계산한 슬라이싱 직전 상태를 전송.
> 서버는 이 값으로 `overlap = min(tapX + movingBlockWidth, stackTopX + stackTopWidth) - max(tapX, stackTopX)` 를 재현 가능.

**Subscribe** `/room/{joinCode}/block-stacking/progress`

서버 → 모든 클라이언트. 랭킹 바 업데이트용.

```json
{
  "players": [
    { "name": "철수", "floor": 12 },
    { "name": "영희", "floor": 9 }
  ]
}
```

#### 3. 게임 오버 — 최종 점수 제출 (Optional)

클라이언트는 게임 오버(블록 이탈 또는 타이머 만료) 시 최종 상태를 기록합니다. 
이미 매 탭마다 `progress` 토픽을 통해 실시간 검증 및 점수 기록이 이루어지고 있으므로, 별도의 누적 로그 제출은 생략합니다.

#### 4. 전체 완료 브로드캐스트

**Subscribe** `/room/{joinCode}/block-stacking/complete`

서버 → 모든 클라이언트. 제한 시간 경과 or 모든 플레이어 제출 완료 시.

```json
{
  "state": "DONE"
}
```

---

### REST API

기존 `MiniGameResultPage`가 사용하는 공통 엔드포인트 재사용.

#### 최종 랭킹 조회

**GET** `/minigames/ranks?joinCode={joinCode}`

```json
{
  "ranks": [
    { "rank": 1, "name": "철수", "score": 15 },
    { "rank": 2, "name": "영희", "score": 12 }
  ]
}
```

#### 내 점수 조회

**GET** `/minigames/scores?joinCode={joinCode}&playerName={name}`

```json
{
  "score": 15,
  "rank": 1,
  "totalPlayers": 4
}
```

> 두 엔드포인트 모두 기존 미니게임과 동일한 스펙. 백엔드에서 `gameType: "BLOCK_STACKING"` 필터링만 추가하면 됨.

---

### 호스트 전용 — 게임 시작 REST

기존 미니게임과 동일한 패턴으로 추정. 백엔드 확인 필요.

**POST** `/rooms/{joinCode}/minigame/start`

```json
{
  "gameType": "BLOCK_STACKING"
}
```

---

## 구현 단계

### Phase 1 — 게임 코어 (프론트 전용)
- [x] `blockStackingConstants.ts` (속도 구간 테이블 포함)
- [x] `BlockStackingGameContext.ts` + `BlockStackingGameProvider.tsx` (로컬 상태 → WebSocket 연동 완료)
- [x] `useBlockStackingGame.ts` (canvas rAF 루프 + 슬라이싱 로직, 흔들림 효과 내장)
- [x] `useBlockStackingSounds.ts` (Web Audio API 사운드)
- [x] `BlockStackingCanvas.tsx`

### Phase 2 — UI 조립 + 프레임워크 편입
- [x] `BlockStackingGamePlayPage.tsx` (음소거 토글 포함)
- [x] `BlockStackingGameReadyPage.tsx`
- [x] `EliminatedOverlay.tsx` (탈락 시 전용 오버레이)
- [x] `BlockStackingRanks.tsx` (캔버스 우측 상단 수직형 랭킹)
- [x] `gameConfigs.tsx` 등록 + 슬라이드 이미지 연결
- [x] `MiniGameType` 확장 + `MiniGameSection` 통합

### Phase 3 — 백엔드 연동
- [x] Provider에 WebSocket 구독 추가 (`state`, `progress`, `complete`) 및 타임스탬프 동기화
- [x] `BlockStackingGameContext.ts` 및 `BlockStackingGameProvider.tsx` 연동 완료
- [x] `useBlockStackingActions.ts` 구현 (progress publish)
- [x] `useBlockStackingGame.ts`에서 탭 성공 시 `publishProgress()` 호출 연결
- [x] `MiniGameSection` 연동 완료
- [x] `BlockStackingRanks` WebSocket 데이터 연동
- [x] REST 최종 랭킹 연동 (기존 `MiniGameResultPage` 재사용)

#### 백엔드 작업 (프론트 연동 전 완료 필요)
→ 아래 **백엔드 구현 요구사항** 섹션 참고

---

## 미결 사항

- [x] 블록 색상 팔레트 — 10색 순환 적용
- [x] 배경색 — 층수에 따라 낮→밤 그라디언트 전환
- [x] 게임 오버 후 결과 화면 — 공유 `MiniGameResultPage` 사용 완료
- [x] 멀티 플레이 실시간 비교 — `BlockStackingRanks` 오버레이 적용 완료

---

## 백엔드 구현 요구사항

> 프론트가 Phase 3 연동을 시작하기 전에 아래 항목이 백엔드에서 완료되어야 한다.

### 1. MiniGameType 등록

`/rooms/minigames` 등 게임 목록 API 응답에 `BLOCK_STACKING` 타입 추가.
현재 프론트는 `FRONTEND_ONLY_GAMES` 임시 배열로 이를 보완 중이며, 백엔드 지원 후 제거 예정.

---

### 2. WebSocket STOMP 엔드포인트 구현

#### 게임 상태 브로드캐스트 (`/room/{joinCode}/block-stacking/state`)

호스트가 게임을 시작하면 서버가 모든 클라이언트에 순서대로 브로드캐스트:
1. `PREPARE` (카운트다운 시작) + `"countdown": 3`
2. `PLAYING` (게임 시작)
3. `DONE` (타이머 만료 또는 전원 제출 완료 시)

```json
{ "state": "PREPARE" | "PLAYING" | "DONE", "countdown": 3 }
```

#### 진행 중 탭 수신 및 랭킹 브로드캐스트

- **수신** `/room/{joinCode}/block-stacking/progress`: 클라이언트로부터 층 정보 + 탭 좌표 수신
- 수신 후 전체 플레이어 랭킹을 동일 토픽으로 브로드캐스트

수신 페이로드:
```json
{
  "playerName": "string",
  "floor": 7,
  "tapX": 142.5,
  "movingBlockX": 140.0,
  "stackTopX": 85.0,
  "stackTopWidth": 150.0
}
```

브로드캐스트 페이로드:
```json
{
  "players": [
    { "name": "철수", "floor": 12 },
    { "name": "영희", "floor": 9 }
  ]
}
```

> **좌표 검증 로직** (서버 측): `overlap = min(tapX + movingBlockWidth, stackTopX + stackTopWidth) - max(tapX, stackTopX)`
> overlap ≤ 0이면 해당 탭은 게임 오버 조건 — 이상 플래그 처리 가능.
> 단, 클라이언트가 낙관적으로 처리 중이므로 서버가 결과를 되돌리는 방식은 사용하지 않음.

#### 최종 점수 제출 수신

이미 매 탭마다 `progress` 토픽을 통해 실시간 검증 및 점수 기록이 이루어지고 있으므로, 별도의 누적 로그 제출은 생략 가능합니다. 
모든 플레이어 제출 완료(또는 20초 타이머 만료) 시 complete 브로드캐스트를 수행합니다.

**Publish** `/room/{joinCode}/block-stacking/complete`
```json
{ "state": "DONE" }
```

---

### 3. REST API

기존 미니게임 공통 엔드포인트를 재사용하며, `gameType: "BLOCK_STACKING"` 필터링만 추가하면 됨.

| 엔드포인트 | 용도 |
|-----------|------|
| `GET /minigames/ranks?joinCode={joinCode}` | 최종 랭킹 조회 |
| `GET /minigames/scores?joinCode={joinCode}&playerName={name}` | 내 점수 조회 |
| `POST /rooms/{joinCode}/minigame/start` (body: `{ "gameType": "BLOCK_STACKING" }`) | 호스트 게임 시작 |

---

### 4. 게임 규칙 파라미터 (서버 동기화 권장)

현재 프론트에서 상수로 관리 중인 값들. 멀티플레이어 공정성을 위해 서버에서 게임 시작 시 내려주는 방식으로 확장 가능 (현재는 프론트 고정값 사용).

| 파라미터 | 현재 프론트 값 | 설명 |
|---------|--------------|------|
| `gameDuration` | 20초 | 총 제한 시간 |
| `initialSpeed` | 2.2 | 초기 블록 이동 속도 (px/frame) |
| `speedIncrement` | 1.05 | 층당 속도 배율 (5% 증가) |
| `maxSpeed` | 12.0 | 최고 속도 상한 |
| `initialBlockWidth` | 150px | 첫 번째 층 블록 너비 |
