import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
import { rankColorMap } from '@/constants/color';
import { RACING_Z_INDEX } from '../../constants/zIndex';

const riseIn = keyframes`
  from {
    transform: translateY(12px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
`;

export const Container = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background: ${({ theme }) => theme.color.gray[950]}C7;
  z-index: ${RACING_Z_INDEX.FINISH};
`;

export const Title = styled.p`
  ${({ theme }) => theme.typography.caption}
  letter-spacing: 0.22em;
  color: ${({ theme }) => theme.color.white}9E;
`;

/** 시안의 체커 플래그 띠. 결승 이미지와 같은 무늬를 CSS 로 낸다. */
export const CheckerRule = styled.div`
  width: 60px;
  height: 3px;
  margin: 16px 0 0;
  background-image:
    linear-gradient(
      45deg,
      ${({ theme }) => theme.color.gray[100]} 25%,
      transparent 25%,
      transparent 75%,
      ${({ theme }) => theme.color.gray[100]} 75%
    ),
    linear-gradient(
      45deg,
      ${({ theme }) => theme.color.gray[100]} 25%,
      transparent 25%,
      transparent 75%,
      ${({ theme }) => theme.color.gray[100]} 75%
    );
  background-size: 12px 12px;
  background-position:
    0 0,
    6px 6px;
`;

export const Podium = styled.ol`
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  max-width: 320px;
  margin-top: 34px;
  padding: 0 32px;
  list-style: none;
`;

type PodiumItemProps = {
  $rank: number;
  $isMe: boolean;
};

export const PodiumItem = styled.li<PodiumItemProps>`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: ${({ $isMe }) => ($isMe ? '14px' : '12px 14px')};
  border-radius: 8px;
  animation: ${riseIn} 320ms ease-out backwards;
  animation-delay: ${({ $rank }) => 120 + ($rank - 1) * 140}ms;

  ${({ theme, $rank, $isMe }) => {
    if ($isMe) {
      return `
        background: ${theme.color.point[500]}33;
        border: 1px solid ${theme.color.white}99;
        box-shadow: 0 0 22px ${theme.color.point[500]}47;
      `;
    }
    if ($rank === 1) {
      return `
        background: ${theme.color.yellow}24;
        border: 1px solid ${theme.color.yellow}80;
      `;
    }
    return `
      background: ${theme.color.white}0F;
      border: 1px solid ${theme.color.white}29;
    `;
  }}

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

type RankProps = {
  $rank: number;
  $isMe: boolean;
};

export const Rank = styled.span<RankProps>`
  ${({ theme }) => theme.typography.h1}
  width: 26px;
  line-height: 1;
  color: ${({ theme, $rank, $isMe }) => ($isMe ? theme.color.white : rankColorMap[$rank])};
`;

type AvatarProps = {
  $color: string;
  $isMe: boolean;
};

export const Avatar = styled.span<AvatarProps>`
  flex-shrink: 0;
  width: ${({ $isMe }) => ($isMe ? '38px' : '34px')};
  height: ${({ $isMe }) => ($isMe ? '38px' : '34px')};
  border: ${({ theme, $isMe }) => `${$isMe ? 3 : 2}px solid ${theme.color.white}`};
  border-radius: 50%;
  background: ${({ $color }) => $color};
`;

export const Name = styled.span`
  ${({ theme }) => theme.typography.h4}
  flex-grow: 1;
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  color: ${({ theme }) => theme.color.white};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const MyResult = styled.p`
  ${({ theme }) => theme.typography.small}
  margin-top: 8px;
  color: ${({ theme }) => theme.color.white}B3;
`;
