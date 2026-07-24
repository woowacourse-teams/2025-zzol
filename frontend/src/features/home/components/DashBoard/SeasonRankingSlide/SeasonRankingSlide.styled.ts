import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';
import { scoreboardReveal } from '@/styles/animations/scoreboardReveal';

const spin = keyframes`
  to { transform: rotate(360deg); }
`;

export const Card = styled.div`
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
`;

export const CardTitle = styled.h3`
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[800]};
  letter-spacing: -0.01em;
`;

export const Empty = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  padding: 16px 0;
`;

export const MyRankRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-radius: 12px;
  background: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[100]};
`;

export const MyRankLabel = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.point[400]};
  font-weight: 700;
`;

export const MyRankValue = styled.span`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.gray[900]};
`;

export const AnimatedItem = styled.div<{ $index: number }>`
  animation: ${scoreboardReveal} 0.38s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: ${({ $index }) => `${$index * 0.07}s`};
`;

export const Spinner = styled.div`
  width: 20px;
  height: 20px;
  border: 2px solid ${({ theme }) => theme.color.gray[200]};
  border-top-color: ${({ theme }) => theme.color.point[400]};
  border-radius: 50%;
  animation: ${spin} 0.7s linear infinite;
  margin: 12px auto;
`;
