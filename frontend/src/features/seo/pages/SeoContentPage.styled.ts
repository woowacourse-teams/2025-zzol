import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
import { Link } from 'react-router-dom';

const fadeSlideUp = keyframes`
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
`;

export const Container = styled.article`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

/** CTA 바를 화면 하단에 고정하기 위해 본문만 스크롤한다 */
export const Scroll = styled.div`
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 4px 24px;
  display: flex;
  flex-direction: column;
  gap: 28px;
`;

export const Hero = styled.header`
  display: flex;
  flex-direction: column;
  gap: 14px;
`;

export const HeroIcon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  border-radius: 18px;
  background: ${({ theme }) => theme.color.point[50]};

  img {
    width: 34px;
    height: 34px;
  }
`;

export const Title = styled.h1`
  ${({ theme }) => theme.typography.h2}
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.02em;
`;

/** 본문 첫 문장 — 요약처럼 읽히도록 본문보다 진하게 */
export const Lead = styled.p`
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.gray[800]};
  line-height: 1.75;
`;

export const Body = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[600]};
  line-height: 1.85;
`;

export const Section = styled.section`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

export const SectionTitle = styled.h2`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.01em;
`;

/* 게임 설명 그림 — 인게임 설명 카루셀과 같은 그림을 재사용한다 */

export const Figure = styled.figure`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
`;

export const FigureImage = styled.img`
  width: 100%;
  max-height: 180px;
  object-fit: contain;
  border-radius: 12px;
`;

/* 이용 가이드 스텝 */

export const StepList = styled.ol`
  display: flex;
  flex-direction: column;
  gap: 10px;
  list-style: none;
`;

export const StepItem = styled.li<{ $index: number }>`
  animation: ${fadeSlideUp} 0.35s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: ${({ $index }) => `${Math.min($index, 7) * 0.07}s`};

  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
`;

export const StepBody = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex: 1;
  min-width: 0;
`;

export const StepNumber = styled.span`
  ${({ theme }) => theme.typography.caption}
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
`;

export const StepText = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[700]};
  line-height: 1.7;
`;

/** 인앱 가이드 모달의 실제 화면 캡처 — 세로로 긴 폰 화면이라 폭을 좁게 잡는다 */
export const StepShot = styled.img`
  flex-shrink: 0;
  width: 92px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 10px;
`;

/* 미니게임 목록 — MiniGameCarousel 카드와 같은 시각 언어 */

export const GameGrid = styled.ul`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  list-style: none;
`;

export const GameGridItem = styled.li<{ $index: number }>`
  animation: ${fadeSlideUp} 0.35s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: ${({ $index }) => `${Math.min($index, 7) * 0.07}s`};
  display: flex;
`;

export const GameCard = styled(Link)`
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
  padding: 18px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  text-decoration: none;
  transition:
    background 0.15s ease,
    transform 0.12s ease;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
    transform: scale(0.96);
  }
`;

export const GameIconTile = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: ${({ theme }) => theme.color.point[50]};

  img {
    width: 28px;
    height: 28px;
  }
`;

export const GameName = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[900]};
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  letter-spacing: -0.01em;
`;

export const GameDesc = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
`;

/* 다른 게임 보기 — 6종을 한 화면에 보이는 3열 그리드 */

export const MiniGrid = styled.ul`
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  list-style: none;
`;

export const MiniCard = styled(Link)`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  height: 100%;
  padding: 12px 6px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  text-decoration: none;
  transition:
    background 0.15s ease,
    transform 0.12s ease;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
    transform: scale(0.96);
  }
`;

export const MiniIconTile = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 12px;
  background: ${({ theme }) => theme.color.point[50]};

  img {
    width: 22px;
    height: 22px;
  }
`;

export const MiniName = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[800]};
  font-weight: ${({ theme }) => theme.typography.h4.fontWeight};
  text-align: center;
  line-height: 1.3;
`;

/* 하단 고정 CTA 바 */

export const CtaBar = styled.div`
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0 -4px;
  padding: 12px 4px calc(8px + env(safe-area-inset-bottom));
  background: ${({ theme }) => theme.color.white};
  border-top: 1px solid ${({ theme }) => theme.color.gray[100]};
`;

export const PrimaryCta = styled(Link)`
  ${({ theme }) => theme.typography.h4}
  display: flex;
  align-items: center;
  justify-content: center;
  height: 50px;
  background: ${({ theme }) => theme.color.point[400]};
  border-radius: 12px;
  color: ${({ theme }) => theme.color.white};
  text-decoration: none;
  box-shadow: 0 6px 18px ${({ theme }) => theme.color.point[400]}33;
  transition:
    background 0.15s ease,
    transform 0.12s ease;

  &:active {
    background: ${({ theme }) => theme.color.point[500]};
    transform: scale(0.98);
  }
`;

export const SecondaryCta = styled(Link)`
  ${({ theme }) => theme.typography.h4}
  display: flex;
  align-items: center;
  justify-content: center;
  height: 44px;
  background: ${({ theme }) => theme.color.gray[50]};
  border-radius: 12px;
  color: ${({ theme }) => theme.color.gray[700]};
  text-decoration: none;
  transition: background 0.15s ease;

  &:active {
    background: ${({ theme }) => theme.color.gray[100]};
  }
`;
