import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';

const flashBg = keyframes`
  0% { background-color: rgba(255, 60, 60, 0.9); }
  30% { background-color: rgba(255, 120, 50, 0.85); }
  60% { background-color: rgba(255, 60, 60, 0.8); }
  100% { background-color: rgba(0, 0, 0, 0.75); }
`;

const shakeScreen = keyframes`
  0%, 100% { transform: translate(0, 0); }
  10% { transform: translate(-8px, -6px); }
  20% { transform: translate(8px, 4px); }
  30% { transform: translate(-6px, 8px); }
  40% { transform: translate(6px, -4px); }
  50% { transform: translate(-4px, 6px); }
  60% { transform: translate(4px, -8px); }
  70% { transform: translate(-8px, 4px); }
  80% { transform: translate(6px, 6px); }
  90% { transform: translate(-4px, -6px); }
`;

const bombPop = keyframes`
  0% { transform: scale(0); opacity: 0; }
  40% { transform: scale(1.4); opacity: 1; }
  60% { transform: scale(0.9); }
  80% { transform: scale(1.1); }
  100% { transform: scale(1); }
`;

const textSlideUp = keyframes`
  0% { transform: translateY(30px); opacity: 0; }
  100% { transform: translateY(0); opacity: 1; }
`;

const particleExplode = keyframes`
  0% { transform: scale(0); opacity: 1; }
  50% { transform: scale(1.5); opacity: 0.7; }
  100% { transform: scale(2.5); opacity: 0; }
`;

export const Overlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  animation:
    ${flashBg} 0.8s ease forwards,
    ${shakeScreen} 0.5s ease;
`;

export const BombEmoji = styled.div`
  font-size: 5rem;
  animation: ${bombPop} 0.6s cubic-bezier(0.34, 1.56, 0.64, 1) forwards;
`;

export const ExplosionRing = styled.div`
  position: absolute;
  width: 200px;
  height: 200px;
  border-radius: 50%;
  border: 4px solid rgba(255, 200, 50, 0.6);
  animation: ${particleExplode} 1s ease-out forwards;
`;

export const EliminatedText = styled.div`
  font-size: 1.8rem;
  font-weight: 800;
  color: white;
  text-align: center;
  text-shadow: 2px 2px 8px rgba(0, 0, 0, 0.5);
  animation: ${textSlideUp} 0.5s ease 0.4s both;
`;

export const SubText = styled.div`
  font-size: 1.1rem;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.8);
  animation: ${textSlideUp} 0.5s ease 0.6s both;
`;
