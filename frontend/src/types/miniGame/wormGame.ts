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

import type {
  Point,
  WormGameStateResponse,
  WormSnapshotResponse,
  WormsStateResponse,
} from '@/apis/websocket/generated/wsContract';

export type {
  SteerCommand,
  WormGameState,
  WormPosition,
  WormTrailSnapshot,
} from '@/apis/websocket/generated/wsContract';

export type WormGameStateMessage = WormGameStateResponse;

export type WormDeltaMessage = WormsStateResponse;

export type WormPoint = Point;

export type WormSnapshotMessage = WormSnapshotResponse;
