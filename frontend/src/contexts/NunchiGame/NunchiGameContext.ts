import { NunchiGameState, NunchiLocalInputState } from '@/types/miniGame/nunchiGame';
import { createContext, useContext } from 'react';

/**
 * 눈치게임 Context.
 *
 * deadline 단일 소스(요구사항 F): idleDeadlineEpochMs 는 state·stand 두 핸들러가 모두
 * 이 Provider state 한 곳에만 write 한다(두 값 사이 튐 방지).
 *
 * 스큐 보정(요구사항 D): serverOffsetMs = serverNowEpochMs - Date.now() 를 매 메시지 갱신.
 * 카운트다운은 remaining = deadline - (Date.now() + serverOffsetMs) 로 계산한다.
 */
type NunchiGameContextType = {
  /** 서버 상태 머신. */
  gameState: NunchiGameState;

  /** 현재 눌러야 할 숫자(PLAYING 의 currentNumber). */
  currentNumber: number;

  /** 라이브로 일어선 사람들의 닉네임(누른 순 아님, "일어섰다"는 사실만 — 요구사항 H). */
  stood: string[];

  /** 충돌한 사람들의 닉네임(COLLISION_COOLDOWN 동안). 결과 전까지 충돌 상태 표시용. */
  collided: string[];

  /**
   * 방금 새로 일어선 사람의 닉네임(stand 핸들러가 매 이벤트마다 갱신).
   * 재접속 스냅샷의 stood 배열을 diff 하지 않고 "막 도착한 단일 presser"만 애니메이션하기 위한
   * 이벤트 신호(요구사항 7). seq 가 바뀔 때만 애니메이션을 1회 재생한다(같은 이름 연속 무시 방지).
   */
  lastStand: { name: string; seq: number } | null;

  /**
   * 충돌 발생 이벤트 신호(COLLISION_COOLDOWN 핸들러가 매 이벤트마다 seq 증가).
   * collided 배열 diff 대신 이 seq 변화로 흔들림/빨강 충돌 애니메이션을 1회 재생한다(요구사항 7).
   */
  collisionSeq: number;

  /** 시계 스큐 보정 오프셋. = serverNowEpochMs - Date.now(), 매 메시지 갱신(요구사항 D). */
  serverOffsetMs: number;

  /** PLAYING 이 시작될 서버 시각(epoch ms). DESCRIPTION 동안만 non-null — ReadyPage 전환 타이머용. */
  playStartEpochMs: number | null;

  /** 무입력 자동 종료 데드라인(단일 소스 — 요구사항 F). state·stand 둘 다 여기로 write. */
  idleDeadlineEpochMs: number | null;

  /** 라운드 하드 안전 캡(고정 상한). */
  hardCapEpochMs: number | null;

  /** 충돌 후 재개 예정 시각(COLLISION_COOLDOWN 동안). */
  resumeAtEpochMs: number | null;

  /** 내 로컬 키패드 상태(요구사항 E). 서버가 아니라 FE 가 관리. */
  myInputState: NunchiLocalInputState;

  /** 내가 키패드를 누를 수 있는지(파생값). PRESSED/STOOD/COLLIDED·쿨다운·끊김 시 false. */
  canPress: boolean;

  /** WebSocket 연결 상태(요구사항 J — 끊김 시 키패드 경고/비활성). */
  isConnected: boolean;

  /** 키패드 press 핸들러 — 낙관적 로컬 피드백 후 STOMP send(요구사항 D/E/J). */
  press: () => void;
};

export const NunchiGameContext = createContext<NunchiGameContextType | null>(null);

export const useNunchiGameContext = () => {
  const context = useContext(NunchiGameContext);
  if (!context) {
    throw new Error('useNunchiGameContext 는 NunchiGameProvider 안에서 사용해야 합니다.');
  }
  return context;
};
