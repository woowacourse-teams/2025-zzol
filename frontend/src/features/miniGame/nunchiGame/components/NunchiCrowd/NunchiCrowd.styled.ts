import styled from '@emotion/styled';
import { css, keyframes } from '@emotion/react';
import type { NunchiPersonState } from './NunchiCrowd';

// 막 일어선 사람의 번호 뱃지가 톡 튀어나오는 등장(요구사항 7 — lastStand 한 명만).
const badgePop = keyframes`
  0% { transform: translateX(-50%) scale(0); }
  60% { transform: translateX(-50%) scale(1.2); }
  100% { transform: translateX(-50%) scale(1); }
`;

export const Crowd = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  width: 100%;
`;

export const Row = styled.div`
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  justify-content: center;
  gap: 16px 12px;
  width: 100%;
  padding: 0 8px;
`;

export const Person = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  width: 60px;
`;

// 아바타 래퍼 — 앉음은 살짝 흐리게, 탈락은 회색·기울임으로 구분한다. 일어섬은 머리 위 번호 뱃지로 전달한다.
export const IconSlot = styled.div<{
  $state: NunchiPersonState;
  $isMe: boolean;
}>`
  position: relative;
  display: inline-flex;
  border-radius: 50%;
  transition:
    opacity 0.3s ease,
    filter 0.3s ease,
    transform 0.3s ease,
    box-shadow 0.2s ease;

  ${({ $state }) =>
    $state === 'seated' &&
    css`
      opacity: 0.5;
    `}
  ${({ $state }) =>
    $state === 'stood' &&
    css`
      opacity: 1;
    `}
  ${({ $state }) =>
    $state === 'out' &&
    css`
      transform: translateY(4px) rotate(-10deg);
      opacity: 0.4;
      filter: grayscale(1);
    `}

  ${({ $isMe, theme }) =>
    $isMe &&
    css`
      box-shadow: 0 0 0 3px ${theme.color.point[300]};
    `}

  @media (prefers-reduced-motion: reduce) {
    transition: none;
  }
`;

// 머리 위 번호 뱃지 — 배경은 플레이어 고유색(colorList — CircleIcon 과 동일한 예외), 숫자는 흰색.
export const Badge = styled.span<{ $color: string; $justStood: boolean }>`
  position: absolute;
  top: -12px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1;
  min-width: 22px;
  height: 22px;
  padding: 0 5px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px;
  border: 2px solid ${({ theme }) => theme.color.white};
  background: ${({ $color }) => $color};
  color: ${({ theme }) => theme.color.white};
  ${({ theme }) => theme.typography.caption}
  line-height: 1;
  font-variant-numeric: tabular-nums;
  box-shadow: 0 2px 6px ${({ theme }) => theme.color.black}33;

  ${({ $justStood }) =>
    $justStood &&
    css`
      animation: ${badgePop} 0.3s ease-out;
    `}

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

export const Name = styled.div<{ $state: NunchiPersonState }>`
  max-width: 100%;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
  text-decoration: ${({ $state }) => ($state === 'out' ? 'line-through' : 'none')};
`;

export const Remaining = styled.div`
  display: flex;
  align-items: baseline;
  gap: 4px;
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const RemainingCount = styled.span`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.point[500]};
  font-variant-numeric: tabular-nums;
`;
