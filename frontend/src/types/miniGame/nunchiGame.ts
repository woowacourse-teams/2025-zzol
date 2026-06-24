/**
 * 눈치게임(Nunchi) 메시지/상태 타입.
 *
 * 컨트랙트 SSOT(권위): backend ADR-0031 "결정 8(WebSocket 상태 컨트랙트)" + 구현 노트 N1~N7.
 * 아래 타입은 ADR의 JSON 컨트랙트를 그대로 옮긴 것이다. 필드명·옵셔널 여부를 임의로 바꾸지 않는다.
 *
 * 주의(필드명 함정 — ADR JSON 그대로):
 *  - PLAYING 은 `currentNumber` 를 쓴다.
 *  - COLLISION_COOLDOWN 은 `number` 를 쓴다 (currentNumber 아님). 둘을 통일하지 않는다.
 */

/**
 * 서버 state 토픽이 보내는 상태(컨트랙트): DESCRIPTION → PLAYING ↔ COLLISION_COOLDOWN → DONE (ADR 결정 8).
 * 와이어 메시지 타입(NunchiStateMessage)은 이 4개만 쓴다 — 임의 확장 금지.
 */
export type NunchiServerState = 'DESCRIPTION' | 'PLAYING' | 'COLLISION_COOLDOWN' | 'DONE';

/**
 * Provider 의 라우팅 상태 = 서버 상태 머신과 동일(4-state). 초기값 'DESCRIPTION' 으로 첫 메시지 전까지
 * 인트로(ReadyPage)를 보여준다. 서버가 DESCRIPTION 을 playStartEpochMs(PLAYING 시작 시각)와 함께
 * 보내므로, ReadyPage 는 그 시각까지 인트로 후 play 로 전환한다(PLAYING 메시지 도착 또는 타임아웃 폴백).
 */
export type NunchiGameState = NunchiServerState;

/**
 * DESCRIPTION 메시지 — 게임 진입(규칙 설명 단계) / 설명 구간 재접속 스냅샷.
 * playStartEpochMs 로 PLAYING 시작 시각을 알린다. FE 는 이 시각까지 인트로를 보여주고 play 로 전환한다.
 *
 * 예: { state:'DESCRIPTION', serverNowEpochMs:..., playStartEpochMs:... }
 */
export type NunchiDescriptionMessage = {
  state: 'DESCRIPTION';
  /** 서버 현재 시각(시계 스큐 보정 기준). 모든 메시지에 포함. */
  serverNowEpochMs: number;
  /** PLAYING 이 시작될 서버 시각(epoch ms). ReadyPage 가 이 시각에 play 로 전환한다. */
  playStartEpochMs: number;
};

/**
 * PLAYING 메시지 — 게임 시작 / 충돌 후 재개 / 재접속 스냅샷에 공통으로 쓰인다.
 * `stood` 와 `currentNumber` 가 함께 와서 새로고침 복구가 한 번에 된다(재접속 스냅샷, ADR 결정 8).
 *
 * 예: { state:'PLAYING', currentNumber:1, stood:['민수'], serverNowEpochMs:..., idleDeadlineEpochMs:..., hardCapEpochMs:... }
 */
export type NunchiPlayingMessage = {
  state: 'PLAYING';
  /** 현재 눌러야 할 공유 카운터 숫자. */
  currentNumber: number;
  /** 이미 일어선(=누른) 사람들의 닉네임. 재접속 스냅샷 복구용. */
  stood: string[];
  /** 서버 현재 시각(시계 스큐 보정 기준). 모든 메시지에 포함. */
  serverNowEpochMs: number;
  /** 무입력 자동 종료 예정 시각. 유효 입력마다 갱신된다(stand 메시지에도 동일 필드가 옴 — 단일 소스). */
  idleDeadlineEpochMs: number;
  /** 라운드 하드 안전 캡(고정 상한). */
  hardCapEpochMs: number;
};

/**
 * COLLISION_COOLDOWN 메시지 — 충돌 발생(ADR 결정 6).
 * 누가 충돌했고(collided) 언제 재개되는지(resumeAtEpochMs)를 알린다.
 *
 * 예: { state:'COLLISION_COOLDOWN', number:1, collided:['철수','영희'], serverNowEpochMs:..., resumeAtEpochMs:... }
 */
export type NunchiCollisionCooldownMessage = {
  state: 'COLLISION_COOLDOWN';
  /** 충돌이 난 숫자(reset 대상). PLAYING 의 currentNumber 와 필드명이 다름에 주의. */
  number: number;
  /** 충돌한 사람들의 닉네임. 이들은 영구 OUT 처리(1인 1press). */
  collided: string[];
  /** 서버 현재 시각(시계 스큐 보정 기준). */
  serverNowEpochMs: number;
  /** 재개 예정 시각. FE 는 이 시각까지 쿨다운 카운트다운을 그린다. */
  resumeAtEpochMs: number;
};

/** DONE 메시지 — 게임 종료. 결과는 결과 페이지에서 별도 조회한다. */
export type NunchiDoneMessage = {
  state: 'DONE';
};

/** `/topic/room/{joinCode}/nunchi/state` 메시지(state 필드 기준 discriminated union). */
export type NunchiStateMessage =
  | NunchiDescriptionMessage
  | NunchiPlayingMessage
  | NunchiCollisionCooldownMessage
  | NunchiDoneMessage;

/**
 * `/topic/room/{joinCode}/nunchi/stand` 메시지 — 한 명이 번호를 차지(첫 press 즉시·낙관적, N2).
 *
 * 핵심(ADR 결정 8 — BE 확정):
 *  - **단일 presser** 다(배열 아님). 번호별 첫 press 가 즉시 stand 로 브로드캐스트된다(N2).
 *  - `number` 는 **카운터 값이지 등수가 아니다**. rank/등수는 stand 에 절대 오지 않는다
 *    (라이브엔 잠정 등수 표시 금지). 충돌은 stand 가 아니라 COLLISION_COOLDOWN state 로만 통보된다.
 *  - serverNowEpochMs(스큐 보정) + idleDeadlineEpochMs(데드라인 갱신, 요구사항 F)가 함께 온다.
 *    → state 핸들러와 stand 핸들러가 같은 Provider 필드(idleDeadlineEpochMs)에 write 한다(단일 소스).
 *
 * 예: { name:'민수', number:1, serverNowEpochMs:1712140000000, idleDeadlineEpochMs:1712140010000 }
 */
export type NunchiStandMessage = {
  /** 방금 일어선 사람의 닉네임(단일 presser). */
  name: string;
  /** 이 사람이 차지한 카운터 값(등수 아님 — PLAYING 의 currentNumber 와 동일 의미). */
  number: number;
  /** 서버 현재 시각(시계 스큐 보정 기준). */
  serverNowEpochMs: number;
  /** 갱신된 무입력 데드라인. state 의 idleDeadlineEpochMs 와 동일 필드로 취급(단일 소스). */
  idleDeadlineEpochMs: number;
};

/**
 * 결과 응답 DTO — 3계층 결과 화면용(ADR 구현 노트 N7 — BE 확정).
 *
 * rank 숫자만으로는 어떤 동점 그룹이 충돌인지 미입력인지 FE 가 구분할 수 없으므로,
 * BE 가 각 사람의 계층(tier)을 함께 노출한다. FE 는 같은 rank 를 묶고 계층 배지를 렌더한다.
 *
 * 전송(확정): 공유 결과 DTO(`/minigames/ranks`·`/minigames/scores`, tier 없음)를 건드리지 않고
 *   **nunchi 전용 REST** 를 둔다 — `GET /minigames/nunchi/result?joinCode={joinCode}`.
 * 필드명(확정): `playerName`(name 아님), `rank`(standard-competition 1,2,2,4,6), `tier` 3값.
 *
 * 예: { results: [ { playerName:'민수', rank:1, tier:'SOLO' },
 *                   { playerName:'철수', rank:2, tier:'COLLISION' }, ... ] }
 */
export type NunchiResultTier = 'SOLO' | 'COLLISION' | 'MISS';

export type NunchiResultEntry = {
  /** 플레이어 닉네임(기존 결과 DTO 와 동일하게 playerName — name 아님). */
  playerName: string;
  /** standard-competition rank (동점 그룹은 동일 rank). 예: 1,2,2,4,6 */
  rank: number;
  /** 계층 배지용. SOLO=정상, COLLISION=충돌, MISS=미입력. */
  tier: NunchiResultTier;
};

export type NunchiResultResponse = {
  results: NunchiResultEntry[];
};

/**
 * 로컬(클라이언트 전용) 키패드 상태 — 서버 브로드캐스트가 아니라 FE 내부 상태(요구사항 E).
 *  - IDLE:      아직 안 누름. 누를 수 있음.
 *  - PRESSED:   첫 press 후 락아웃(더블탭 방지). 서버 stand 확정 대기.
 *  - STOOD:     서버 stand 로 확정됨(정상 일어섬).
 *  - COLLIDED:  COLLISION_COOLDOWN.collided 에 내가 포함 → 영구 OUT.
 */
export type NunchiLocalInputState = 'IDLE' | 'PRESSED' | 'STOOD' | 'COLLIDED';
