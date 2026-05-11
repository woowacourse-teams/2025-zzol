import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

export const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const CarouselArea = styled.div`
  overflow: hidden;
  width: 100%;
  cursor: grab;
  user-select: none;

  &:active {
    cursor: grabbing;
  }
`;

export const Controls = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
`;

export const NavChevron = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  background: ${({ theme }) => theme.color.white};
  color: ${({ theme }) => theme.color.gray[400]};
  font-size: 14px;
  line-height: 1;
  cursor: pointer;
  flex-shrink: 0;
  transition:
    background 0.15s ease,
    color 0.15s ease,
    border-color 0.15s ease;

  @media (hover: hover) {
    &:hover:not(:disabled) {
      background: ${({ theme }) => theme.color.gray[100]};
      color: ${({ theme }) => theme.color.gray[700]};
      border-color: ${({ theme }) => theme.color.gray[300]};
    }
  }

  &:disabled {
    opacity: 0.25;
    cursor: default;
  }
`;

export const Track = styled.div`
  display: flex;
  transition: transform 0.3s ease;
  will-change: transform;
`;

export const Card = styled.div`
  flex: 0 0 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: background 0.15s ease;
  user-select: none;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const Tag = styled.span`
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 20px;
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
  font-weight: 700;
  letter-spacing: 0.03em;
  width: fit-content;
  background: ${({ theme }) => theme.color.point[50]};
  color: ${({ theme }) => theme.color.point[500]};
`;

export const CardTop = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
`;

export const Title = styled.span`
  font-size: 15px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[900]};
  line-height: 1.4;
  letter-spacing: -0.01em;
`;

export const Date = styled.span`
  font-size: 12px;
  font-weight: 400;
  color: ${({ theme }) => theme.color.gray[400]};
  flex-shrink: 0;
  padding-top: 2px;
`;

export const Body = styled.p`
  font-size: 13px;
  font-weight: 400;
  color: ${({ theme }) => theme.color.gray[500]};
  line-height: 1.7;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
`;

export const ReadMore = styled.span`
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.point[400]};
  margin-top: 2px;
  align-self: flex-end;
`;

export const Dots = styled.div`
  display: flex;
  align-items: center;
  gap: 5px;
`;

export const Dot = styled.span<{ $active: boolean }>`
  width: ${({ $active }) => ($active ? '16px' : '5px')};
  height: 5px;
  border-radius: 3px;
  background: ${({ theme, $active }) => ($active ? theme.color.point[400] : theme.color.gray[200])};
  transition:
    width 0.2s ease,
    background 0.2s ease;
`;

export const MoreCard = styled.div`
  flex: 0 0 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 140px;
  padding: 20px 18px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  user-select: none;
  transition:
    background-color 0.15s ease,
    transform 0.12s ease;

  &:active {
    background-color: ${({ theme }) => theme.color.gray[50]};
    transform: scale(0.985);
  }
`;

export const MoreTop = styled.div`
  display: flex;
  flex-direction: column;
  gap: 6px;
`;

export const MoreIconCircle = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: ${({ theme }) => theme.color.point[50]};
  color: ${({ theme }) => theme.color.point[500]};
  font-size: 18px;
  margin-bottom: 6px;
  flex-shrink: 0;
`;

export const MoreTitle = styled.span`
  font-size: 17px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.03em;
  line-height: 1.2;
`;

export const MoreSub = styled.span`
  font-size: 13px;
  font-weight: 400;
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const MoreArrow = styled.span`
  align-self: flex-end;
  font-size: 20px;
  color: ${({ theme }) => theme.color.gray[300]};
`;

export const Detail = styled.div`
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 0 8px;
`;

export const DetailMeta = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const DetailDate = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const DetailBody = styled.p`
  font-size: 15px;
  font-weight: 400;
  color: ${({ theme }) => theme.color.gray[700]};
  line-height: 1.8;
  white-space: pre-wrap;
`;

const shimmer = keyframes`
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
`;

export const SkeletonCard = styled.div`
  width: 100%;
  height: 110px;
  border-radius: 16px;
  background: linear-gradient(
    90deg,
    ${({ theme }) => theme.color.gray[100]} 25%,
    ${({ theme }) => theme.color.gray[50]} 50%,
    ${({ theme }) => theme.color.gray[100]} 75%
  );
  background-size: 200% 100%;
  animation: ${shimmer} 1.4s infinite linear;
`;
