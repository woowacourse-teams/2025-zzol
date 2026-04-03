import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

const fadeIn = keyframes`
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
`;

const scaleUp = keyframes`
  from {
    transform: scale(0.8);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
`;

export const Backdrop = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.75);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 100;
  animation: ${fadeIn} 0.3s ease-out forwards;
  backdrop-filter: blur(4px);
`;

export const Content = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  animation: ${scaleUp} 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275) forwards;
`;

export const MessageWrapper = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
`;

export const EliminatedText = styled.h2`
  color: #ff4d4d;
  font-size: 2.5rem;
  font-weight: 800;
  text-shadow: 0 0 20px rgba(255, 77, 77, 0.5);
  margin: 0;
  letter-spacing: -0.05rem;
`;

export const SubText = styled.p`
  color: #ffffff;
  font-size: 1.1rem;
  font-weight: 500;
  opacity: 0.8;
  margin: 0;
`;

export const IconWrapper = styled.div`
  font-size: 4rem;
  filter: drop-shadow(0 0 10px rgba(255, 255, 255, 0.3));
`;

export const RankContainer = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  align-items: center;
  width: 100%;
  margin-top: 1rem;
`;

export const RankTitle = styled.div`
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 0.05rem;
  text-transform: uppercase;
`;
