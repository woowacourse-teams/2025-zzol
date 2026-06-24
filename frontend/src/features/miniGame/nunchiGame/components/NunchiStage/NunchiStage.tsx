import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import { useNunchiCountdown } from '../../hooks/useNunchiCountdown';
import NunchiCrowd from '../NunchiCrowd/NunchiCrowd';
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
    lastCollided,
    collisionSeq,
    serverOffsetMs,
    idleDeadlineEpochMs,
    idleWindowMs,
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

  // 긴박감 단계(요구사항 4): 남은 시간이 줄수록 calm→warn→danger 로 박동·색을 고조한다.
  // 데드라인 존재 여부로만 게이팅한다 — remaining===0(만료) 을 calm 으로 되돌리면
  // 아무도 안 누른 클라이맥스 순간에 게이지가 꽉 차고 색이 풀려버린다(DONE 도착 전까지 유지돼야 함).
  // 시작 직후 1프레임 false-danger 는 rAF 첫 틱(~16ms) 전에 사라져 체감되지 않는다.
  const hasDeadline = idleDeadlineEpochMs != null;
  const level = !hasDeadline
    ? 'calm'
    : idleRemainingMs <= 1200
      ? 'danger'
      : idleRemainingMs <= 3000
        ? 'warn'
        : 'calm';

  // 도화선 게이지: 남은시간 / 전체창(idleWindowMs) → 매 갱신마다 가득 찬 상태에서 0 으로 비례해 줄어든다.
  const fuse =
    idleWindowMs && idleWindowMs > 0 ? Math.max(0, Math.min(1, idleRemainingMs / idleWindowMs)) : 1;
  // 게이지 색: 가득(1)=초록 → 빈(0)=빨강. hue 140→0 으로 선형 보간(요구사항 3).
  const fuseColor = `hsl(${Math.round(140 * fuse)}, 75%, 45%)`;

  return (
    <S.Stage>
      {isCooldown ? (
        <S.Cooldown key={collisionSeq}>
          <S.CooldownTitle>충돌!</S.CooldownTitle>
          <S.CollidedList>
            {lastCollided.map((name) => (
              <S.CollidedName key={name} $isMe={name === myName}>
                {name}
              </S.CollidedName>
            ))}
          </S.CollidedList>
          <S.CooldownTimer>{(resumeRemainingMs / 1000).toFixed(1)}초 후 재개</S.CooldownTimer>
        </S.Cooldown>
      ) : (
        <>
          <S.Vignette $active={level === 'danger'} aria-hidden />
          {/* key={currentNumber}: 카운터 전진마다 tick 펀치를 1회 재생(요구사항 4). */}
          <S.Number key={currentNumber} $level={level}>
            {currentNumber}
          </S.Number>
          <S.TensionMeter>
            <S.FuseTrack>
              <S.FuseFill style={{ transform: `scaleX(${fuse})`, backgroundColor: fuseColor }} />
            </S.FuseTrack>
            <S.Seconds $level={level}>{(idleRemainingMs / 1000).toFixed(1)}초</S.Seconds>
          </S.TensionMeter>
          <NunchiCrowd />
        </>
      )}
    </S.Stage>
  );
};

export default NunchiStage;
