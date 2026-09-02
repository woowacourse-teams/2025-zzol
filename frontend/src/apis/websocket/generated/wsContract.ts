// 자동 생성 파일이라 손으로 고치지 않는다. 원천은 backend 의 @WsTopic/@WsQueue/@WsReceive 다.
// 갱신: backend/gradlew -p backend :app:test --tests '*WsCatalogContractTest*'
// destination 은 방 코드를 변수로 보간해서 넘긴다. `/room/ABCD/winner` 처럼 통째로 고정한
// 문자열은 정상 경로여도 아래 동치 검사에 걸려 컴파일 오류가 난다.

/** 구독 destination. useWebSocketSubscription 이 /topic 을 붙이므로 topic 은 prefix 없이 쓴다. */
export type WsTopicPath =
  | `/room/${string}`
  | `/room/${string}/blind-timer/progress`
  | `/room/${string}/blind-timer/state`
  | `/room/${string}/block-stacking/progress`
  | `/room/${string}/block-stacking/state`
  | `/room/${string}/gameState`
  | `/room/${string}/ladder/line`
  | `/room/${string}/ladder/state`
  | `/room/${string}/minigame`
  | `/room/${string}/nunchi/stand`
  | `/room/${string}/nunchi/state`
  | `/room/${string}/qr-code`
  | `/room/${string}/racing-game`
  | `/room/${string}/racing-game/state`
  | `/room/${string}/roulette`
  | `/room/${string}/round`
  | `/room/${string}/settlement`
  | `/room/${string}/speed-touch/progress`
  | `/room/${string}/speed-touch/state`
  | `/room/${string}/winner`
  | `/room/${string}/worm`
  | `/room/${string}/worm/snapshot`
  | `/room/${string}/worm/state`;

/** 개인 큐. broker 가 직접 라우팅하므로 /user/queue 를 그대로 쓴다. */
export type WsQueuePath =
  | '/user/queue/friends/presence'
  | '/user/queue/friends/removed'
  | '/user/queue/friends/requests'
  | '/user/queue/friends/responses'
  | '/user/queue/rooms/invitations'
  | '/user/queue/worm/snapshot';

export type WsSubscribePath =
  | WsTopicPath
  | WsQueuePath
  | '/user/queue/errors';

/** 송신 destination. send 가 /app 을 붙이므로 prefix 없이 쓴다. */
export type WsSendPath =
  | `/room/${string}/blind-timer/stop`
  | `/room/${string}/block-stacking/fail`
  | `/room/${string}/block-stacking/progress`
  | `/room/${string}/ladder/draw`
  | `/room/${string}/minigame/command`
  | `/room/${string}/nunchi/press`
  | `/room/${string}/racing-game/tap`
  | `/room/${string}/show-roulette`
  | `/room/${string}/speed-touch/touch`
  | `/room/${string}/spin-roulette`
  | `/room/${string}/update-minigames`
  | `/room/${string}/update-players`
  | `/room/${string}/update-ready`
  | `/room/${string}/worm/steer`;

// `${string}` 은 '/' 도 삼키므로 `/room/${string}` 이 모든 room 경로에 맞아 버린다.
// 호출부 리터럴이 정확히 한 패턴과 같을 때(상호 대입)만 통과시킨다. 아니면 오류 메시지에 경로를 찍는다.
type Same<A, B> = [A] extends [B] ? ([B] extends [A] ? true : false) : false;
type MatchesOne<D, P> = P extends unknown ? Same<D, P> : never;
type Exact<D extends string, P extends string> = true extends MatchesOne<D, P>
  ? D
  : `ws 카탈로그에 없는 destination: ${D}`;

export type WsSubscribeDestination<D extends WsSubscribePath> = Exact<D, WsSubscribePath>;
export type WsSendDestination<D extends WsSendPath> = Exact<D, WsSendPath>;

// BE record 를 그대로 옮긴 payload 타입. BE 에서 @Nullable 을 단 필드만 `field?: T | null` 이다.
// @JsonInclude(NON_NULL) 이면 필드가 빠지고, 아니면 null 이 오므로 둘 다 허용한다.
export type BlindTimerGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'DONE';
export type BlindTimerPlayerProgress = {
  playerName: string;
  stopped: boolean;
  timedOut: boolean;
};
export type BlindTimerProgressResponse = {
  players: BlindTimerPlayerProgress[];
};
export type BlindTimerStateResponse = {
  state: BlindTimerGameState;
  targetTimeMillis: number;
  blindDelayMillis: number;
};
export type BlockStackingGameState = 'READY' | 'PREPARE' | 'PLAYING' | 'DONE';
export type BlockStackingPlayerRankInfo = {
  name: string;
  floor: number;
};
export type BlockStackingProgressRequest = {
  floor: number;
  movingBlockX: number;
  stackTopX: number;
  stackTopWidth: number;
};
export type BlockStackingProgressResponse = {
  players: BlockStackingPlayerRankInfo[];
};
export type BlockStackingStateResponse = {
  state: BlockStackingGameState;
  endTimeEpochMs?: number | null;
};
export type CardGameState = 'READY' | 'FIRST_LOADING' | 'LOADING' | 'PREPARE' | 'PLAYING' | 'SCORE_BOARD' | 'DONE';
export type CardInfoMessage = {
  cardType: CardType;
  value: number;
  selected: boolean;
  playerName?: string | null;
  colorIndex?: number | null;
};
export type CardType = 'ADDITION' | 'MULTIPLIER';
export type CommandType = 'START_MINI_GAME' | 'SELECT_CARD';
export type Entry = {
  playerName: string;
  totalPoints: number;
  tier: string;
  seasonRank: number;
};
export type FriendRemovedPayload = {
  removedByUserId: number;
};
export type FriendRequestPayload = {
  requestId: number;
  fromUserId: number;
  fromUserCode: string;
  fromNickname: string;
  createdAt: string;
};
export type FriendResponsePayload = {
  requestId: number;
  accepted: boolean;
  counterpartUserId: number;
  counterpartUserCode: string;
  counterpartNickname: string;
};
export type LadderDrawRequest = {
  segmentIndex: number;
};
export type LadderGameState = 'DESCRIPTION' | 'PREPARE' | 'DRAWING' | 'RESULT' | 'DONE';
export type LadderLineResponse = {
  playerName: string;
  segmentIndex: number;
  row: number;
  colorIndex: number;
};
export type LadderStateResponse = {
  state: LadderGameState;
  poles?: PoleInfo[] | null;
  bottomRanks?: Record<string, number> | null;
  endTimeEpochMs?: number | null;
  rankings?: Record<string, number> | null;
  animationDurationMs?: number | null;
};
export type MiniGameMessage = {
  commandType: CommandType;
  commandRequest: unknown;
};
export type MiniGameSelectMessage = {
  hostName: string;
  miniGameTypes: MiniGameType[];
};
export type MiniGameStartMessage = {
  miniGameType: MiniGameType;
};
export type MiniGameStateMessage = {
  cardGameState: CardGameState;
  currentRound: RoundLabel;
  cardInfoMessages: CardInfoMessage[];
  allSelected: boolean;
};
export type MiniGameType = 'CARD_GAME' | 'RACING_GAME' | 'SPEED_TOUCH' | 'BLIND_TIMER' | 'BLOCK_STACKING' | 'LADDER_GAME' | 'NUNCHI_GAME' | 'WORM_GAME';
export type NunchiStandResponse = {
  name: string;
  number: number;
  serverNowEpochMs: number;
  idleDeadlineEpochMs: number;
};
export type NunchiState = 'DESCRIPTION' | 'READY' | 'PLAYING' | 'COLLISION_COOLDOWN' | 'DONE';
export type NunchiStateResponse = {
  state: NunchiState;
  currentNumber?: number | null;
  stood?: string[] | null;
  number?: number | null;
  collided?: string[] | null;
  serverNowEpochMs?: number | null;
  idleDeadlineEpochMs?: number | null;
  hardCapEpochMs?: number | null;
  resumeAtEpochMs?: number | null;
  playStartEpochMs?: number | null;
};
export type PlayerResponse = {
  userId?: number | null;
  playerName: string;
  playerType: PlayerType;
  isReady: boolean;
  colorIndex: number;
  probability: number;
};
export type PlayerType = 'HOST' | 'GUEST';
export type Point = {
  x: number;
  y: number;
};
export type PoleInfo = {
  index: number;
  playerName: string;
  colorIndex: number;
};
export type PresencePayload = {
  userId: number;
  online: boolean;
  joinCode?: string | null;
  joinable: boolean;
};
export type QrCodeStatus = 'PENDING' | 'SUCCESS' | 'ERROR';
export type QrCodeStatusResponse = {
  status: QrCodeStatus;
  qrCodeUrl?: string | null;
};
export type RacingGameRunnersStateResponse = {
  distance: RacingRange;
  players: RunnerPosition[];
};
export type RacingGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'DONE';
export type RacingGameStateResponse = {
  state: RacingGameState;
};
export type RacingRange = {
  start: number;
  end: number;
};
export type ReadyChangeMessage = {
  joinCode: string;
  playerName: string;
  isReady: boolean;
};
export type RoomInvitationPayload = {
  inviterUserId: number;
  inviterNickname: string;
  joinCode: string;
};
export type RoomState = 'READY' | 'PLAYING' | 'SCORE_BOARD' | 'ROULETTE' | 'DONE';
export type RoomStatusResponse = {
  joinCode: string;
  roomState: RoomState;
};
export type RouletteSpinMessage = {
  hostName: string;
};
export type RoundLabel = 'READY' | 'FIRST' | 'SECOND';
export type RunnerPosition = {
  playerName: string;
  position: number;
  speed: number;
};
export type SeasonRankMessage = {
  seasonKey: string;
  entries: Entry[];
};
export type SpeedTouchGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'DONE';
export type SpeedTouchPlayerProgress = {
  playerName: string;
  currentNumber: number;
  finished: boolean;
};
export type SpeedTouchProgressResponse = {
  players: SpeedTouchPlayerProgress[];
};
export type SpeedTouchStateResponse = {
  state: SpeedTouchGameState;
};
export type SteerCommand = {
  angle: number;
  seq: number;
};
export type TapCommand = {
  tapCount: number;
};
export type TouchCommand = {
  touchedNumber: number;
};
export type WinnerResponse = {
  playerName: string;
  colorIndex: number;
  randomAngle: number;
};
export type WormGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'FINISH' | 'DONE';
export type WormGameStateResponse = {
  state: WormGameState;
};
export type WormPosition = {
  playerName: string;
  x: number;
  y: number;
  angle: number;
  alive: boolean;
  lastSeq: number;
};
export type WormSnapshotResponse = {
  tick: number;
  tickMillis: number;
  serverNow: string;
  radius: number;
  worms: WormTrailSnapshot[];
};
export type WormTrailSnapshot = {
  playerName: string;
  alive: boolean;
  trail: Point[];
};
export type WormsStateResponse = {
  tick: number;
  radius: number;
  worms: WormPosition[];
};

// destination 별 payload. 세그먼트가 많은 패턴을 앞에 둬야 `/room/${string}` 이 다른 room 경로를 삼키지 않는다.
export type WsPayloadOf<D extends WsSubscribePath> =
  D extends '/user/queue/friends/presence' ? PresencePayload :
  D extends '/user/queue/friends/removed' ? FriendRemovedPayload :
  D extends '/user/queue/friends/requests' ? FriendRequestPayload :
  D extends '/user/queue/friends/responses' ? FriendResponsePayload :
  D extends '/user/queue/rooms/invitations' ? RoomInvitationPayload :
  D extends '/user/queue/worm/snapshot' ? WormSnapshotResponse :
  D extends `/room/${string}/blind-timer/progress` ? BlindTimerProgressResponse :
  D extends `/room/${string}/blind-timer/state` ? BlindTimerStateResponse :
  D extends `/room/${string}/block-stacking/progress` ? BlockStackingProgressResponse :
  D extends `/room/${string}/block-stacking/state` ? BlockStackingStateResponse :
  D extends `/room/${string}/ladder/line` ? LadderLineResponse :
  D extends `/room/${string}/ladder/state` ? LadderStateResponse :
  D extends `/room/${string}/nunchi/stand` ? NunchiStandResponse :
  D extends `/room/${string}/nunchi/state` ? NunchiStateResponse :
  D extends `/room/${string}/racing-game/state` ? RacingGameStateResponse :
  D extends `/room/${string}/speed-touch/progress` ? SpeedTouchProgressResponse :
  D extends `/room/${string}/speed-touch/state` ? SpeedTouchStateResponse :
  D extends `/room/${string}/worm/snapshot` ? WormSnapshotResponse :
  D extends `/room/${string}/worm/state` ? WormGameStateResponse :
  D extends '/user/queue/errors' ? string :
  D extends `/room/${string}/gameState` ? MiniGameStateMessage :
  D extends `/room/${string}/minigame` ? MiniGameType[] :
  D extends `/room/${string}/qr-code` ? QrCodeStatusResponse :
  D extends `/room/${string}/racing-game` ? RacingGameRunnersStateResponse :
  D extends `/room/${string}/roulette` ? RoomStatusResponse :
  D extends `/room/${string}/round` ? MiniGameStartMessage :
  D extends `/room/${string}/settlement` ? SeasonRankMessage :
  D extends `/room/${string}/winner` ? WinnerResponse :
  D extends `/room/${string}/worm` ? WormsStateResponse :
  D extends `/room/${string}` ? PlayerResponse[] :
  never;
