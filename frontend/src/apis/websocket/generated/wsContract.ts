// 자동 생성 파일이라 손으로 고치지 않는다. 원천은 backend 의 @WsTopic/@WsQueue/@WsReceive 다.
// 갱신: backend/gradlew -p backend :app:test --tests '*WsCatalogContractTest*' && npm run generate:ws
// destination 은 방 코드를 변수로 보간해서 넘긴다. `/room/ABCD/winner` 처럼 통째로 고정한
// 문자열은 정상 경로여도 아래 동치 검사에 걸려 컴파일 오류가 난다.
import type { components } from './wsOpenApi';

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

// payload 타입. 모양은 ws-openapi.json 에서 openapi-typescript 가 만든 wsOpenApi.d.ts 에 있다.
// BE 에서 @Nullable 을 단 필드만 `field?: T | null` 이다.
export type BlindTimerGameState = components['schemas']['BlindTimerGameState'];
export type BlindTimerPlayerProgress = components['schemas']['BlindTimerPlayerProgress'];
export type BlindTimerProgressResponse = components['schemas']['BlindTimerProgressResponse'];
export type BlindTimerStateResponse = components['schemas']['BlindTimerStateResponse'];
export type BlockStackingGameState = components['schemas']['BlockStackingGameState'];
export type BlockStackingPlayerRankInfo = components['schemas']['BlockStackingPlayerRankInfo'];
export type BlockStackingProgressRequest = components['schemas']['BlockStackingProgressRequest'];
export type BlockStackingProgressResponse = components['schemas']['BlockStackingProgressResponse'];
export type BlockStackingStateResponse = components['schemas']['BlockStackingStateResponse'];
export type CardGameState = components['schemas']['CardGameState'];
export type CardInfoMessage = components['schemas']['CardInfoMessage'];
export type CardType = components['schemas']['CardType'];
export type CommandType = components['schemas']['CommandType'];
export type Entry = components['schemas']['Entry'];
export type FriendRemovedPayload = components['schemas']['FriendRemovedPayload'];
export type FriendRequestPayload = components['schemas']['FriendRequestPayload'];
export type FriendResponsePayload = components['schemas']['FriendResponsePayload'];
export type LadderDrawRequest = components['schemas']['LadderDrawRequest'];
export type LadderGameState = components['schemas']['LadderGameState'];
export type LadderLineResponse = components['schemas']['LadderLineResponse'];
export type LadderStateResponse = components['schemas']['LadderStateResponse'];
export type MiniGameMessage = components['schemas']['MiniGameMessage'];
export type MiniGameSelectMessage = components['schemas']['MiniGameSelectMessage'];
export type MiniGameStartMessage = components['schemas']['MiniGameStartMessage'];
export type MiniGameStateMessage = components['schemas']['MiniGameStateMessage'];
export type MiniGameType = components['schemas']['MiniGameType'];
export type NunchiStandResponse = components['schemas']['NunchiStandResponse'];
export type NunchiState = components['schemas']['NunchiState'];
export type NunchiStateResponse = components['schemas']['NunchiStateResponse'];
export type PlayerResponse = components['schemas']['PlayerResponse'];
export type PlayerType = components['schemas']['PlayerType'];
export type Point = components['schemas']['Point'];
export type PoleInfo = components['schemas']['PoleInfo'];
export type PresencePayload = components['schemas']['PresencePayload'];
export type QrCodeStatus = components['schemas']['QrCodeStatus'];
export type QrCodeStatusResponse = components['schemas']['QrCodeStatusResponse'];
export type RacingGameRunnersStateResponse = components['schemas']['RacingGameRunnersStateResponse'];
export type RacingGameState = components['schemas']['RacingGameState'];
export type RacingGameStateResponse = components['schemas']['RacingGameStateResponse'];
export type RacingRange = components['schemas']['RacingRange'];
export type ReadyChangeMessage = components['schemas']['ReadyChangeMessage'];
export type RoomInvitationPayload = components['schemas']['RoomInvitationPayload'];
export type RoomState = components['schemas']['RoomState'];
export type RoomStatusResponse = components['schemas']['RoomStatusResponse'];
export type RouletteSpinMessage = components['schemas']['RouletteSpinMessage'];
export type RoundLabel = components['schemas']['RoundLabel'];
export type RunnerPosition = components['schemas']['RunnerPosition'];
export type SeasonRankMessage = components['schemas']['SeasonRankMessage'];
export type SpeedTouchGameState = components['schemas']['SpeedTouchGameState'];
export type SpeedTouchPlayerProgress = components['schemas']['SpeedTouchPlayerProgress'];
export type SpeedTouchProgressResponse = components['schemas']['SpeedTouchProgressResponse'];
export type SpeedTouchStateResponse = components['schemas']['SpeedTouchStateResponse'];
export type SteerCommand = components['schemas']['SteerCommand'];
export type TapCommand = components['schemas']['TapCommand'];
export type TouchCommand = components['schemas']['TouchCommand'];
export type WinnerResponse = components['schemas']['WinnerResponse'];
export type WormGameState = components['schemas']['WormGameState'];
export type WormGameStateResponse = components['schemas']['WormGameStateResponse'];
export type WormPosition = components['schemas']['WormPosition'];
export type WormSnapshotResponse = components['schemas']['WormSnapshotResponse'];
export type WormTrailSnapshot = components['schemas']['WormTrailSnapshot'];
export type WormsStateResponse = components['schemas']['WormsStateResponse'];

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
