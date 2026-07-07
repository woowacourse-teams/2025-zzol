import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import {
  NunchiGameState,
  NunchiLocalInputState,
  NunchiStandMessage,
  NunchiStateMessage,
} from '@/types/miniGame/nunchiGame';
import { PropsWithChildren, useCallback, useMemo, useState } from 'react';
import { NunchiGameContext } from './NunchiGameContext';

/**
 * 눈치게임 Provider — 구독·상태·낙관적 입력의 단일 오케스트레이션 지점.
 *
 * 책임 매핑(ADR-0031 요구사항):
 *  - (D) 낙관적 일어서기 + 충돌 되돌림: press() 즉시 myInputState='PRESSED'(ghost),
 *        stand 로 'STOOD' 확정 / collided 에 내가 있으면 'COLLIDED' 전환.
 *  - (E) 키패드 락아웃: 첫 press 즉시 비활성, 충돌자 영구 OUT, 쿨다운 동안 비활성(canPress 파생).
 *  - (F) deadline 단일 소스: idleDeadlineEpochMs 를 state·stand 두 핸들러가 모두 여기로 write.
 *  - (D 스큐) serverOffsetMs = serverNowEpochMs - Date.now() 를 매 메시지 갱신.
 *  - (재접속 스냅샷) 구독 직후 PLAYING 스냅샷(currentNumber+stood)을 애니메이션 없이 상태만 반영.
 *  - (G 단일 rAF) 카운트다운 루프는 useNunchiCountdown 훅이 담당(여기선 deadline 값만 보관).
 */
const NunchiGameProvider = ({ children }: PropsWithChildren) => {
  const { joinCode, myName } = useIdentifier();
  const { send, isConnected } = useWebSocket();

  // 초기값은 'DESCRIPTION'(인트로). 'PLAYING' 으로 두면 ReadyPage 가 즉시 play 로 이동해 슬라이드가 안 보인다.
  const [gameState, setGameState] = useState<NunchiGameState>('DESCRIPTION');
  const [currentNumber, setCurrentNumber] = useState(1);
  const [stood, setStood] = useState<string[]>([]);
  // 닉네임 → 누른 숫자(무대 번호 뱃지용). stand 이벤트로만 채워지며 리셋하지 않는다
  // (쿨다운 후 재개 PLAYING 에서 기존 수집값을 잃지 않도록 — 재접속 시엔 비어 시작).
  const [standNumbers, setStandNumbers] = useState<Record<string, number>>({});
  const [collided, setCollided] = useState<string[]>([]);
  // 이번 충돌 그룹(가장 최근 COLLISION_COOLDOWN 메시지의 collided). 쿨다운 무대 "충돌!" 표시용 —
  // 누적 union(collided)이 아니라 "방금 충돌한 사람"만 보여줘야 한다.
  const [lastCollided, setLastCollided] = useState<string[]>([]);
  const [serverOffsetMs, setServerOffsetMs] = useState(0);
  const [playStartEpochMs, setPlayStartEpochMs] = useState<number | null>(null);
  const [idleDeadlineEpochMs, setIdleDeadlineEpochMs] = useState<number | null>(null);
  const [idleWindowMs, setIdleWindowMs] = useState<number | null>(null);
  const [hardCapEpochMs, setHardCapEpochMs] = useState<number | null>(null);
  const [resumeAtEpochMs, setResumeAtEpochMs] = useState<number | null>(null);
  const [myInputState, setMyInputState] = useState<NunchiLocalInputState>('IDLE');

  // 이벤트 신호(요구사항 7): 배열 diff 가 아니라 핸들러에서 직접 갱신 → 스냅샷 replay 방지.
  const [lastStand, setLastStand] = useState<{ name: string; seq: number } | null>(null);
  const [collisionSeq, setCollisionSeq] = useState(0);

  // --- state 토픽 구독 -------------------------------------------------------
  useWebSocketSubscription<NunchiStateMessage>(
    `/room/${joinCode}/nunchi/state`,
    useCallback(
      (msg: NunchiStateMessage) => {
        // 1. 스큐 보정(D) — 모든 분기 공통. DONE 은 serverNowEpochMs 가 없으므로 분기 안에서 처리.
        setGameState(msg.state);

        if (msg.state === 'DESCRIPTION') {
          // 규칙 설명 단계. serverNowEpochMs 만 온다(playStartEpochMs 는 READY 로 이동 — 결정 9).
          setServerOffsetMs(msg.serverNowEpochMs - Date.now());
          return;
        }

        if (msg.state === 'READY') {
          // 곧 시작 카운트다운 단계. playStartEpochMs(=PLAYING 시작 시각)를 보관 →
          // ReadyPage 가 READY→START! 를 띄우고 그 시각에 play 로 전환한다(결정 9).
          setServerOffsetMs(msg.serverNowEpochMs - Date.now());
          setPlayStartEpochMs(msg.playStartEpochMs);
          return;
        }

        if (msg.state === 'PLAYING') {
          setServerOffsetMs(msg.serverNowEpochMs - Date.now());
          setPlayStartEpochMs(null); // READY 종료 → 계약("READY 동안만 non-null") 복원.
          // 재접속 스냅샷 포함: currentNumber + stood 를 그대로 반영(애니메이션 없이 상태만 — 요구사항 7).
          setCurrentNumber(msg.currentNumber);
          setStood(msg.stood);
          // deadline 단일 소스(F): state·stand 가 같은 필드에 write.
          setIdleDeadlineEpochMs(msg.idleDeadlineEpochMs);
          // 게이지 전체 길이(메시지 시점 기준). 게이지가 가득→0 으로 비례해 줄도록.
          setIdleWindowMs(msg.idleDeadlineEpochMs - msg.serverNowEpochMs);
          setHardCapEpochMs(msg.hardCapEpochMs);
          setResumeAtEpochMs(null);
          // 쿨다운→PLAYING 재개. collided 는 결과 전까지 유지(요구사항 H)하되,
          // 내 로컬 상태를 스냅샷 권위로 재정렬한다.
          setMyInputState((prev) => {
            if (prev === 'COLLIDED') return prev; // 충돌자 영구 OUT(1인 1press)
            if (msg.stood.includes(myName)) return 'STOOD'; // 스냅샷에 내가 일어서 있음
            // PRESSED 였는데 stand 도 collided 도 못 받은 경우(쿨다운 중 무시된 press 등)
            // → IDLE 로 되돌려 재개 후 다시 누를 수 있게 한다(stuck PRESSED 방지).
            return 'IDLE';
          });
        } else if (msg.state === 'COLLISION_COOLDOWN') {
          setServerOffsetMs(msg.serverNowEpochMs - Date.now());
          // BE 의 collided 는 "이번 충돌 그룹"만(누적 아님 — NunchiFlowOrchestrator#119).
          // 한 라운드에 충돌이 2번 이상이면 이전 그룹을 덮어쓰면 안 된다(영구 OUT — 요구사항 H).
          // 닉네임 기준 union 으로 누적한다(방내 닉네임 유니크 — ADR N6).
          setCollided((prev) => {
            const merged = [...prev];
            msg.collided.forEach((name) => {
              if (!merged.includes(name)) merged.push(name);
            });
            return merged;
          });
          // 무대 "충돌!" 표시는 이번 그룹만(누적 union 아님 — 이전 라운드 탈락자가 섞이면 안 됨).
          setLastCollided(msg.collided);
          setCurrentNumber(msg.number); // 필드명 number 주의(currentNumber 아님)
          setResumeAtEpochMs(msg.resumeAtEpochMs);
          setIdleDeadlineEpochMs(null); // 쿨다운 동안 idle 일시정지(데드라인 무의미)
          setIdleWindowMs(null);
          // 충돌 애니메이션 트리거(이벤트 신호 — 요구사항 7). 배열 diff 아님.
          setCollisionSeq((seq) => seq + 1);
          if (msg.collided.includes(myName)) {
            setMyInputState('COLLIDED'); // 충돌 전환(D/H) — 영구 OUT
          }
        }
        // DONE 분기: 페이지(NunchiGamePlayPage)의 useEffect 가 결과로 navigate.
      },
      [myName]
    )
  );

  // --- stand 토픽 구독 -------------------------------------------------------
  useWebSocketSubscription<NunchiStandMessage>(
    `/room/${joinCode}/nunchi/stand`,
    useCallback(
      (msg: NunchiStandMessage) => {
        setServerOffsetMs(msg.serverNowEpochMs - Date.now()); // 스큐 보정(D)
        setIdleDeadlineEpochMs(msg.idleDeadlineEpochMs); // 단일 소스(F) — state 와 같은 필드
        setIdleWindowMs(msg.idleDeadlineEpochMs - msg.serverNowEpochMs); // 게이지 전체 길이 갱신
        // press 가 들어와 데드라인이 리셋됐으므로 게이지도 가득 찬 상태에서 다시 줄기 시작한다.
        // 카운터 전진: 서버는 클린 solo press 마다 PLAYING 을 보내지 않으므로(시작·재개·스냅샷만),
        // stand 의 number(=currentNumber 와 동일 의미)로 무대 숫자를 갱신해야 1→2→3 이 보인다.
        setCurrentNumber(msg.number);
        // 라이브 일어섬(H): "일어섰다"는 사실만(등수 미포함). number 는 카운터 값(등수 아님).
        setStood((prev) => (prev.includes(msg.name) ? prev : [...prev, msg.name]));
        // 무대 번호 뱃지용 매핑(라이브 수집). 첫 stand 만 기록(중복 press 무시).
        setStandNumbers((prev) =>
          prev[msg.name] != null ? prev : { ...prev, [msg.name]: msg.number }
        );
        // 일어서기 애니메이션 트리거(이벤트 신호 — 요구사항 7). 매 stand 마다 seq 증가.
        setLastStand((prev) => ({ name: msg.name, seq: (prev?.seq ?? 0) + 1 }));
        if (msg.name === myName) {
          setMyInputState('STOOD'); // 내 ghost(PRESSED) → 확정(D)
        }
      },
      [myName]
    )
  );

  // --- 낙관적 입력(D/E/J) ----------------------------------------------------
  // canPress 파생: IDLE 이고 PLAYING 이며 연결됨일 때만 true (E/J).
  const canPress = useMemo(
    () => myInputState === 'IDLE' && gameState === 'PLAYING' && isConnected,
    [myInputState, gameState, isConnected]
  );

  const press = useCallback(() => {
    // 1. 누를 수 없는 상태면 무시(이미 눌렀거나 충돌/쿨다운/끊김 — 잘못된 입력 FE 방어).
    //    BE 는 잘못된 press 를 warn 만 하고 무시하므로(에러 응답 없음) FE 가 1차 방어한다.
    if (!canPress) return;
    // 2. 즉시 락아웃(E) + ghost 낙관적 피드백(D). 서버 stand 도착 시 'STOOD' 로 확정된다.
    setMyInputState('PRESSED');
    // 3. body 없는 send — 서버가 수신 Instant 로 판정한다.
    send(`/room/${joinCode}/nunchi/press`);
  }, [canPress, joinCode, send]);

  const value = useMemo(
    () => ({
      gameState,
      currentNumber,
      stood,
      standNumbers,
      collided,
      lastCollided,
      lastStand,
      collisionSeq,
      serverOffsetMs,
      playStartEpochMs,
      idleDeadlineEpochMs,
      idleWindowMs,
      hardCapEpochMs,
      resumeAtEpochMs,
      myInputState,
      canPress,
      isConnected,
      press,
    }),
    [
      gameState,
      currentNumber,
      stood,
      standNumbers,
      collided,
      lastCollided,
      lastStand,
      collisionSeq,
      serverOffsetMs,
      playStartEpochMs,
      idleDeadlineEpochMs,
      idleWindowMs,
      hardCapEpochMs,
      resumeAtEpochMs,
      myInputState,
      canPress,
      isConnected,
      press,
    ]
  );

  return <NunchiGameContext.Provider value={value}>{children}</NunchiGameContext.Provider>;
};

export default NunchiGameProvider;
