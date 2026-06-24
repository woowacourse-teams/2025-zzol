import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';

type UrgencyLevel = 'calm' | 'warn' | 'danger';

const shake = keyframes`
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-6px); }
  40% { transform: translateX(6px); }
  60% { transform: translateX(-4px); }
  80% { transform: translateX(4px); }
`;

// 카운터가 전진할 때마다 숫자가 한 번 "쿵" 찍히는 펀치(요구사항 4 — 단조로움 해소).
const numberTick = keyframes`
  0% { transform: scale(1.5); }
  60% { transform: scale(0.94); }
  100% { transform: scale(1); }
`;

// 남은 시간이 줄수록 박동이 빨라지는 심장박동(긴박감의 핵심 장치).
const heartbeat = keyframes`
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.08); }
`;

// danger 구간에서 화면 가장자리가 붉게 차오르는 비네트.
const vignettePulse = keyframes`
  0%, 100% { opacity: 0.25; }
  50% { opacity: 1; }
`;

const numberColor = (
  theme: { color: { gray: Record<number, string>; point: Record<number, string> } },
  level: UrgencyLevel
) => {
  if (level === 'danger') return theme.color.point[500];
  if (level === 'warn') return theme.color.point[400];
  return theme.color.gray[800];
};

export const Stage = styled.div`
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 100%;
  flex: 1;
  min-height: 0;
`;

// danger 구간 비네트 — 가장자리만 붉게(가운데는 투명). hex+opacity 2자리로 반투명 처리.
export const Vignette = styled.div<{ $active: boolean }>`
  position: absolute;
  inset: 0;
  pointer-events: none;
  border-radius: 24px;
  background: radial-gradient(
    ellipse at center,
    transparent 45%,
    ${({ theme }) => theme.color.point[500]}40 100%
  );
  opacity: ${({ $active }) => ($active ? 1 : 0)};
  transition: opacity 0.3s ease;
  animation: ${({ $active }) => ($active ? vignettePulse : 'none')} 0.7s ease-in-out infinite;

  @media (prefers-reduced-motion: reduce) {
    animation: none;
    opacity: ${({ $active }) => ($active ? 0.7 : 0)};
  }
`;

export const Number = styled.div<{ $level: UrgencyLevel }>`
  font-size: clamp(96px, 30vw, 140px);
  font-weight: 800;
  line-height: 1;
  color: ${({ theme, $level }) => numberColor(theme, $level)};
  transition: color 0.25s ease;
  will-change: transform;

  ${({ $level }) =>
    $level === 'calm' &&
    css`
      animation: ${numberTick} 0.3s ease-out;
    `}
  ${({ $level }) =>
    $level === 'warn' &&
    css`
      animation:
        ${numberTick} 0.3s ease-out,
        ${heartbeat} 0.9s ease-in-out 0.3s infinite;
    `}
  ${({ $level }) =>
    $level === 'danger' &&
    css`
      animation:
        ${numberTick} 0.3s ease-out,
        ${heartbeat} 0.45s ease-in-out 0.3s infinite;
    `}

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

// 줄어드는 도화선 게이지(요구사항 4 — "3.0초" 텍스트 대체).
export const TensionMeter = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 100%;
  max-width: 240px;
`;

export const FuseTrack = styled.div`
  width: 100%;
  height: 8px;
  border-radius: 999px;
  background: ${({ theme }) => theme.color.gray[200]};
  overflow: hidden;
`;

// 색(backgroundColor)은 남은 비율에 따라 초록→빨강으로 매 프레임 동적 계산해 inline 으로 주입한다
// (정적 토큰으로 표현 불가한 데이터 구동 그라디언트 — transform scaleX 와 동일한 예외).
export const FuseFill = styled.div`
  height: 100%;
  border-radius: 999px;
  transform-origin: left center;
  transition: transform 0.1s linear;
`;

export const Seconds = styled.div<{ $level: UrgencyLevel }>`
  font-size: ${({ theme }) => theme.typography.small.fontSize};
  font-weight: ${({ $level }) => ($level === 'danger' ? 800 : 600)};
  font-variant-numeric: tabular-nums;
  color: ${({ theme, $level }) =>
    $level === 'danger'
      ? theme.color.point[500]
      : $level === 'warn'
        ? theme.color.point[400]
        : theme.color.gray[500]};
  transition: color 0.25s ease;
`;

export const Cooldown = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  animation: ${shake} 0.4s ease-in-out;

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

export const CooldownTitle = styled.div`
  font-size: 40px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.red};
`;

export const CollidedList = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
`;

export const CollidedName = styled.span<{ $isMe?: boolean }>`
  padding: 6px 14px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: ${({ $isMe }) => ($isMe ? 800 : 600)};
  background: ${({ theme }) => theme.color.point[100]};
  color: ${({ theme }) => theme.color.point[500]};
  border: 2px solid ${({ theme }) => theme.color.point[400]};
`;

export const CooldownTimer = styled.div`
  font-size: 18px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[600]};
`;
