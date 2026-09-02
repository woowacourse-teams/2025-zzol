// 자동 생성 — 손으로 고치지 말 것. 원천: backend @WsTopic/@WsQueue/@WsReceive
// 갱신: backend/gradlew -p backend :app:test --tests '*WsCatalogContractTest*'

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
  | '/queue/errors'
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
// 호출부 리터럴이 정확히 한 패턴과 같을 때(상호 대입)만 통과시킨다. 아니면 never 라 컴파일 오류다.
type Same<A, B> = [A] extends [B] ? ([B] extends [A] ? true : false) : false;
type MatchesOne<D, P> = P extends unknown ? Same<D, P> : never;
type Exact<D extends string, P extends string> = true extends MatchesOne<D, P> ? D : never;

export type WsSubscribeDestination<D extends WsSubscribePath> = Exact<D, WsSubscribePath>;
export type WsSendDestination<D extends WsSendPath> = Exact<D, WsSendPath>;
