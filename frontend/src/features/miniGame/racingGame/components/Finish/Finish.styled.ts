import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
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
  gap: 16px;
  padding: 0 24px;
  background: rgba(0, 0, 0, 0.62);
  z-index: ${RACING_Z_INDEX.FINISH};
`;

export const Podium = styled.ol`
  display: flex;
  flex-direction: column;
  gap: 6px;
  width: 100%;
  max-width: 320px;
  list-style: none;
`;

type PodiumItemProps = {
  $order: number;
  $isMe: boolean;
};

export const PodiumItem = styled.li<PodiumItemProps>`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border: 1px solid ${({ theme, $isMe }) => ($isMe ? `${theme.color.white}8C` : 'transparent')};
  border-radius: 8px;
  background: ${({ theme }) => theme.color.white}1F;
  animation: ${riseIn} 320ms ease-out backwards;
  animation-delay: ${({ $order }) => 120 + $order * 140}ms;

  @media (prefers-reduced-motion: reduce) {
    animation: none;
  }
`;

export const Rank = styled.span`
  ${({ theme }) => theme.typography.h3}
  width: 1.5rem;
  color: ${({ theme }) => theme.color.yellow};
`;

export const Name = styled.span`
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.white};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const MyResult = styled.p`
  ${({ theme }) => theme.typography.h3}
  color: ${({ theme }) => theme.color.white};
`;
