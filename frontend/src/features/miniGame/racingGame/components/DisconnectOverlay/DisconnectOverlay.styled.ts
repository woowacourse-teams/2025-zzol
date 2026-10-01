import { keyframes } from '@emotion/react';
import styled from '@emotion/styled';
import { RACING_Z_INDEX } from '../../constants/zIndex';

export const Scrim = styled.div`
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: ${({ theme }) => theme.color.gray[950]}8C;
  z-index: ${RACING_Z_INDEX.BANNER};
`;

export const Banner = styled.div`
  position: absolute;
  top: calc(12px + env(safe-area-inset-top));
  left: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 11px 14px;
  border-radius: 6px;
  background: ${({ theme }) => theme.color.point[500]}EB;
`;

export const BannerText = styled.span`
  ${({ theme }) => theme.typography.small}
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  color: ${({ theme }) => theme.color.white};
`;

export const Notice = styled.div`
  position: absolute;
  top: 40%;
  left: 0;
  right: 0;
  padding: 0 34px;
  text-align: center;
`;

export const NoticeTitle = styled.p`
  ${({ theme }) => theme.typography.h4}
  margin-bottom: 8px;
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  color: ${({ theme }) => theme.color.white};
`;

export const NoticeBody = styled.p`
  ${({ theme }) => theme.typography.caption}
  line-height: 1.75;
  color: ${({ theme }) => theme.color.white}B8;
`;

export const Countdown = styled.div`
  position: absolute;
  left: 24px;
  right: 24px;
  bottom: calc(52px + env(safe-area-inset-bottom));
`;

export const CountdownLabel = styled.p`
  ${({ theme }) => theme.typography.caption}
  margin-bottom: 7px;
  color: ${({ theme }) => theme.color.white}A8;
`;

export const CountdownTrack = styled.div`
  height: 4px;
  border-radius: 2px;
  background: ${({ theme }) => theme.color.white}33;
  overflow: hidden;
`;

const drain = keyframes`
  from { transform: scaleX(1); }
  to   { transform: scaleX(0); }
`;

type FillProps = {
  $durationMs: number;
};

export const CountdownFill = styled.div<FillProps>`
  height: 100%;
  border-radius: 2px;
  transform-origin: left center;
  background: ${({ theme }) => theme.color.yellow};
  animation: ${drain} ${({ $durationMs }) => $durationMs}ms linear forwards;
`;
