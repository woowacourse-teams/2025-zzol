/**
 * 지렁이 게임(WormGame) 메시지 타입.
 *
 * 컨트랙트 SSOT: backend `coffeeshout/wormgame` (WormGameNotifier·WormsStateResponse·WormSnapshotResponse).
 * 필드명·옵셔널 여부를 임의로 바꾸지 않는다.
 *
 * 토픽(FE 는 `/topic` 을 빼고 구독):
 *  - `/room/{joinCode}/worm/state`     → WormGameStateMessage (복구 저장 O)
 *  - `/room/{joinCode}/worm`           → WormDeltaMessage 20Hz (복구 저장 X, tick 단조증가 아니면 폐기)
 *  - `/room/{joinCode}/worm/snapshot`  → WormSnapshotMessage 10s 주기
 *  - `/user/queue/worm/snapshot`       → WormSnapshotMessage 구독 시점 유니캐스트.
 *    **델타 토픽보다 먼저 구독해야** 유니캐스트를 놓치지 않는다.
 *  - 발행 `/room/{joinCode}/worm/steer` → SteerCommand (10Hz, 변화 시만)
 */

export type WormGameState = 'DESCRIPTION' | 'PREPARE' | 'PLAYING' | 'FINISH' | 'DONE';

export type WormGameStateMessage = { state: WormGameState };

export type WormPosition = {
  playerName: string;
  x: number;
  y: number;
  /** 진행 방향(라디안, 서버 정규화 (-π, π]) */
  angle: number;
  alive: boolean;
  /** 서버가 마지막으로 적용한 이 플레이어의 조향 seq */
  lastSeq: number;
};

export type WormDeltaMessage = {
  tick: number;
  radius: number;
  worms: WormPosition[];
};

export type WormPoint = { x: number; y: number };

export type WormTrailSnapshot = {
  playerName: string;
  alive: boolean;
  trail: WormPoint[];
};

export type WormSnapshotMessage = {
  tick: number;
  tickMillis: number;
  /** ISO-8601 — tick↔절대시각 매핑 전용 */
  serverNow: string;
  radius: number;
  worms: WormTrailSnapshot[];
};

/** 조향 대상은 서버가 STOMP principal 로 정한다 — playerName 을 실으면 남의 지렁이를 조종할 수 있다. */
export type SteerCommand = {
  /** 목표각(라디안) */
  angle: number;
  /** 단조증가 입력 일련번호 */
  seq: number;
};
