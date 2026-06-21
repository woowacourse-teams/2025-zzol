import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import { useNunchiCountdown } from '../../hooks/useNunchiCountdown';
import * as S from './NunchiStage.styled';

/**
 * 눈치게임 상단 무대(요구사항 9).
 *
 *  - 현재 숫자(currentNumber)를 크게 표시.
 *  - 일어선 사람(stood) — "일어섰다"는 사실만(등수 미포함 — 요구사항 H/6).
 *    막 도착한 stand(lastStand.seq)만 애니메이션하고 재접속 스냅샷은 정적으로 반영(요구사항 7).
 *  - COLLISION_COOLDOWN 동안 충돌자(collided) 흔들림/빨강 표시 + 재개 카운트다운(resumeAtEpochMs).
 *  - PLAYING 동안 무입력 데드라인(idleDeadlineEpochMs) 카운트다운 — 긴장감(요구사항 1/2/3).
 */
const NunchiStage = () => {
  const { myName } = useIdentifier();
  const {
    gameState,
    currentNumber,
    stood,
    collided,
    lastStand,
    collisionSeq,
    serverOffsetMs,
    idleDeadlineEpochMs,
    resumeAtEpochMs,
  } = useNunchiGameContext();

  const isCooldown = gameState === 'COLLISION_COOLDOWN';

  // 단일 rAF 카운트다운(요구사항 G): 상태에 따라 idle 또는 resumeAt 하나만 활성.
  const idleRemainingMs = useNunchiCountdown(
    idleDeadlineEpochMs,
    serverOffsetMs,
    gameState === 'PLAYING'
  );
  const resumeRemainingMs = useNunchiCountdown(resumeAtEpochMs, serverOffsetMs, isCooldown);

  return (
    <S.Stage>
      {isCooldown ? (
        <S.Cooldown key={collisionSeq}>
          <S.CooldownTitle>충돌!</S.CooldownTitle>
          <S.CollidedList>
            {collided.map((name) => (
              <S.CollidedName key={name} $isMe={name === myName}>
                {name}
              </S.CollidedName>
            ))}
          </S.CollidedList>
          <S.CooldownTimer>{(resumeRemainingMs / 1000).toFixed(1)}초 후 재개</S.CooldownTimer>
        </S.Cooldown>
      ) : (
        <>
          <S.Number>{currentNumber}</S.Number>
          <S.IdleTimer>{(idleRemainingMs / 1000).toFixed(1)}초</S.IdleTimer>
          <S.StoodList>
            {stood.map((name) => (
              <S.StoodName
                key={name}
                // 막 도착한 단일 presser 만 등장 애니메이션(스냅샷 replay 방지 — 요구사항 7).
                $justStood={lastStand?.name === name}
                $isMe={name === myName}
              >
                {name}
              </S.StoodName>
            ))}
          </S.StoodList>
          {collided.length > 0 && (
            // 충돌자는 결과 전까지 탈락 상태로 계속 표시한다(요구사항 H — collided 의도).
            <S.OutList>
              {collided.map((name) => (
                <S.OutName key={name} $isMe={name === myName}>
                  {name} 탈락
                </S.OutName>
              ))}
            </S.OutList>
          )}
        </>
      )}
    </S.Stage>
  );
};

export default NunchiStage;
