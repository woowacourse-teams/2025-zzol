import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

const float = keyframes`
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-4px); }
`;

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
`;

export const WordCard = styled.div`
  background: white;
  border-radius: 24px;
  padding: 24px 48px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
`;

export const Word = styled.span`
  font-size: 2.5rem;
  font-weight: 800;
  color: #1a1a1a;
  letter-spacing: 2px;
`;

export const Label = styled.span`
  font-size: 0.75rem;
  color: #bbb;
  font-weight: 500;
`;

export const NextCharContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  animation: ${float} 2s ease-in-out infinite;
`;

export const NextCharLabel = styled.span`
  font-size: 0.7rem;
  color: #999;
  font-weight: 600;
`;

export const NextChar = styled.span`
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ff6b6b, #ff8e53);
  color: white;
  font-size: 1.5rem;
  font-weight: 800;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(255, 107, 107, 0.35);
`;
