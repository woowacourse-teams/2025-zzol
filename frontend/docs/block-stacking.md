# 빌딩 쌓기 (Block Stacking) 미니게임

화면 이름은 "빌딩 쌓기"고 코드·서버 식별자는 `BLOCK_STACKING`이다.

## 규칙

빌딩 층이 좌우로 왕복하고, 플레이어가 탭한 순간 고정된다. 아래 층과 겹친 부분만 남아 다음 층의 기준이 되고, 벗어난 부분은 떨어진다. 완전히 벗어나면 탈락이다.

- 점수는 쌓은 층수다. 높을수록 룰렛 당첨 확률이 오른다
- 제한 시간은 20초다. 시간이 다 되면 그 자리에서 멈춘다
- 속도는 층마다 5%씩 복리로 오르고 상한이 있다
- 양 끝 오차가 모두 `PERFECT_THRESHOLD`(3px) 안이면 퍼펙트다. 너비가 깎이지 않고 효과가 따로 난다

값은 `features/miniGame/blockStackingGame/constants/blockStackingBalance.ts`(속도·시간·퍼펙트 기준)와 `blockStackingConstants.ts`(캔버스·블록 치수, 낙하 물리, 야경 팔레트)에 있다. 문서에 숫자를 베끼지 않는다.

## 상태 흐름

```text
READY → PREPARE → PLAYING → DONE
```

상태는 서버가 `/room/{joinCode}/block-stacking/state`로 보내고 FE 초기값은 `READY`다. `PREPARE`에서는 공유 `PrepareOverlay`가 뜨고, `DONE`이 오면 `BlockStackingGamePlayPage`가 공유 결과 페이지로 이동한다.

탈락과 시간 만료는 클라이언트가 먼저 판정해 `isLocalGameOver`로 둔다. 서버 `DONE`과는 별개라, 내가 탈락해도 다른 사람이 쌓는 동안 스카이라인을 보며 기다린다.

## 파일 구조

```text
src/features/miniGame/blockStackingGame/
├── components/BlockStackingCanvas/   # canvas 엘리먼트, pointerdown 바인딩, 음소거 토글
├── core/nightTowerDraw.ts            # 하늘·빌딩 층·땅·시계·높이선·스카이라인 그리기 (순수 함수)
├── hooks/
│   ├── useBlockStackingGame.ts       # rAF 루프, 슬라이싱 판정, 카메라, 흔들림, 타이머
│   ├── useBlockStackingActions.ts    # progress·fail 발행
│   └── useBlockStackingSounds.ts     # Web Audio 효과음, muted 상태
├── pages/                            # ReadyPage, PlayPage
└── constants/                        # blockStackingBalance.ts, blockStackingConstants.ts

src/contexts/BlockStackingGame/       # Context + Provider (state·progress 구독)
src/types/miniGame/blockStackingGame.ts  # 생성 타입 alias와 캔버스 전용 타입
```

## 렌더링

HTML5 Canvas와 `requestAnimationFrame`으로 그린다. 매 프레임 하늘, 먼 도시, 땅, 쌓인 층, 움직이는 층, 떨어지는 조각, 시계를 다시 그린다. 그리기 함수는 `core/nightTowerDraw.ts`에 순수 함수로 두고 훅이 상태를 넘긴다.

- **야경 테마(#1831)**: 층은 창문 달린 빌딩이고, 층이 오를수록 하늘이 낮에서 노을, 밤으로 바뀐다
- **카메라**: 움직이는 층이 화면 중앙 근처에 머물도록 프레임 독립 lerp로 따라간다
- **흔들림**: 안착 3px·200ms, 퍼펙트 6px·300ms, 탈락 12px·500ms. `ctx.translate`로 처리한다
- **라이벌 높이선**: 다른 참가자의 현재 층에 점선과 이름표를 긋는다. 색은 서버 `Player.colorIndex`를 따른다
- **탈락 뒤 스카이라인**: "N층에서 멈춤"을 잠시 보여준 뒤, 나와 아직 쌓는 사람의 빌딩을 순위대로 나란히 세운다. 순위는 층수가 같으면 같게 매겨 결과 화면과 맞춘다. 시간이 다 돼 멈추면 전원을 세운다
- **효과음**: 안착·퍼펙트·탈락·속도 구간 진입. `AudioContext`는 첫 탭에서 만들어 자동재생 정책을 피한다. 음소거는 캔버스 위 버튼으로 토글한다

## Context (`BlockStackingGameProvider`)

| 값 | 설명 |
| --- | --- |
| `gameState` | 서버 상태 |
| `rankings` | 최근 progress 브로드캐스트의 참가자 목록 |
| `towers` | 참가자별로 쌓은 블록. 0번은 받침이다. 메시지를 놓쳐 층이 건너뛰면 사이 층을 직전 블록으로 채운다 |
| `isLocalGameOver`, `setLocalGameOver` | 클라이언트 판정 탈락·시간 만료 |
| `endTimeEpochMs`, `totalTimeSeconds` | 서버가 준 종료 시각. 타이머는 이 값과 `Date.now()`로 남은 시간을 센다 |

## WebSocket 계약

destination과 payload는 `src/apis/websocket/generated/wsContract.ts`가 SSOT다. 여기에 필드를 베끼지 않는다.

| 방향 | destination | payload 타입 |
| --- | --- | --- |
| 구독 | `/room/{joinCode}/block-stacking/state` | `BlockStackingStateResponse` |
| 구독 | `/room/{joinCode}/block-stacking/progress` | `BlockStackingProgressResponse` |
| 발행 | `/room/{joinCode}/block-stacking/progress` | `BlockStackingProgressRequest`. 층이 올라갈 때마다 보낸다 |
| 발행 | `/room/{joinCode}/block-stacking/fail` | 없음. 탈락 시 한 번 보낸다 |

탭 결과는 클라이언트가 먼저 반영하고 서버는 받은 좌표로 겹침을 다시 계산해 점수를 기록한다. 서버가 결과를 되돌리지는 않는다.

## 결과

게임 전용 결과 화면은 없다. 공유 `MiniGameResultPage`가 REST로 랭킹을 조회한다.
