import styled from '@emotion/styled';
import { css } from '@emotion/react';
import type { Theme } from '@emotion/react';

const card = ({ theme }: { theme: Theme }) => css`
  background: ${theme.color.white};
  border: 1px solid ${theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
`;

/** 보조 설명 글자. 프로필 메타·링 라벨·타일 라벨 등에 같이 쓴다. */
export const Caption = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px 16px 32px;
  gap: 12px;
`;

/* ── 계정 + 당첨 카드 ── */

export const ProfileCard = styled.div`
  display: flex;
  flex-direction: column;
  gap: 18px;
  padding: 20px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
`;

export const ProfileRow = styled.div`
  display: flex;
  align-items: center;
  gap: 14px;
`;

export const Avatar = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(
    135deg,
    ${({ theme }) => theme.color.point[500]},
    ${({ theme }) => theme.color.point[300]}
  );
  color: ${({ theme }) => theme.color.white};
  font-size: ${({ theme }) => theme.typography.h3.fontSize};
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  flex-shrink: 0;
  letter-spacing: -0.02em;
`;

export const ProfileInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
`;

export const Nickname = styled.span`
  ${({ theme }) => theme.typography.h2}
  color: ${({ theme }) => theme.color.gray[900]};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const SeasonPill = styled.span`
  ${({ theme }) => theme.typography.caption}
  font-weight: ${({ theme }) => theme.typography.h4.fontWeight};
  color: ${({ theme }) => theme.color.point[500]};
  padding: 3px 10px;
  border: 1px solid ${({ theme }) => theme.color.point[100]};
  border-radius: 20px;
  background: ${({ theme }) => theme.color.point[50]};
  white-space: nowrap;
  flex-shrink: 0;
`;

export const WinRow = styled.div`
  display: flex;
  align-items: center;
  gap: 20px;
`;

export const Ring = styled.div`
  position: relative;
  width: 112px;
  height: 112px;
  flex-shrink: 0;
`;

export const RingSvg = styled.svg`
  width: 100%;
  height: 100%;
`;

export const RingTrack = styled.circle`
  fill: none;
  stroke: ${({ theme }) => theme.color.gray[100]};
  stroke-width: 12;
`;

export const RingArc = styled.circle`
  fill: none;
  stroke: ${({ theme }) => theme.color.point[500]};
  stroke-width: 12;
  stroke-linecap: round;
  transform: rotate(-90deg);
  transform-origin: center;
`;

export const RingCenter = styled.div`
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
`;

export const WinStats = styled.div`
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
`;

export const WinStatRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;

  &:not(:last-of-type) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[100]};
  }
`;

export const WinStatLabel = styled.span`
  font-size: ${({ theme }) => theme.typography.small.fontSize};
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const WinStatValue = styled.span<{ $accent?: boolean }>`
  ${({ theme }) => theme.typography.h4}
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  color: ${({ theme, $accent }) => ($accent ? theme.color.status.online : theme.color.gray[900])};
`;

/* ── 미니게임 요약 ── */

export const SummaryCard = styled.div`
  ${card}
  display: grid;
  grid-template-columns: 1fr 1fr;
`;

export const SummaryCell = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 18px 20px 20px;
  min-width: 0;

  &:first-of-type {
    border-right: 1px solid ${({ theme }) => theme.color.gray[100]};
  }
`;

export const StatLabel = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
  letter-spacing: -0.01em;
`;

export const StatValueRow = styled.div`
  display: flex;
  align-items: baseline;
  gap: 2px;
`;

export const StatNumber = styled.span`
  ${({ theme }) => theme.typography.h1}
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.04em;
  line-height: 1;
`;

export const RingPercent = StatNumber;

export const StatUnit = styled.span`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.gray[500]};
  letter-spacing: -0.02em;
`;

export const MostPlayed = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
`;

export const MostPlayedText = styled.div`
  display: flex;
  flex-direction: column;
  min-width: 0;
`;

export const MostPlayedName = styled.span`
  ${({ theme }) => theme.typography.h4}
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  color: ${({ theme }) => theme.color.gray[900]};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const EmptyText = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
`;

/** 게임 아이콘 타일. 요약은 28px, 게임 카드는 44px. */
export const IconTile = styled.div<{ $size: 28 | 44; $muted?: boolean }>`
  display: flex;
  align-items: center;
  justify-content: center;
  width: ${({ $size }) => $size}px;
  height: ${({ $size }) => $size}px;
  border-radius: ${({ $size }) => ($size === 44 ? 12 : 8)}px;
  background: ${({ theme, $muted }) => ($muted ? theme.color.gray[100] : theme.color.point[50])};
  flex-shrink: 0;

  img {
    width: ${({ $size }) => ($size === 44 ? 28 : 18)}px;
    height: ${({ $size }) => ($size === 44 ? 28 : 18)}px;
    object-fit: contain;
    opacity: ${({ $muted }) => ($muted ? 0.55 : 1)};
  }
`;

/* ── 게임 기록 ── */

export const SectionHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 4px 0;
`;

export const SectionTitle = styled.h4`
  ${({ theme }) => theme.typography.h4}
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  color: ${({ theme }) => theme.color.gray[900]};
  margin: 0;
`;

export const Chip = styled.span<{ $muted?: boolean }>`
  ${({ theme }) => theme.typography.caption}
  font-weight: ${({ theme }) => theme.typography.h4.fontWeight};
  color: ${({ theme, $muted }) => ($muted ? theme.color.gray[400] : theme.color.gray[500])};
  padding: 3px 10px;
  border-radius: 20px;
  background: ${({ theme }) => theme.color.gray[100]};
  white-space: nowrap;
  flex-shrink: 0;
`;

export const GameCard = styled.div<{ $empty?: boolean }>`
  ${card}
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 20px 20px;

  ${({ $empty, theme }) =>
    $empty &&
    css`
      border: 1px dashed ${theme.color.gray[200]};
      box-shadow: none;
    `}
`;

export const GameHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
`;

export const GameInfo = ProfileInfo;

export const GameName = styled.span`
  font-size: ${({ theme }) => theme.typography.h4.fontSize};
  font-weight: ${({ theme }) => theme.typography.h4.fontWeight};
  color: ${({ theme }) => theme.color.gray[900]};
`;

export const TileGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
`;

export const Tile = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 12px 14px;
  border-radius: 12px;
  background: ${({ theme }) => theme.color.gray[50]};
  min-width: 0;
`;

export const TileValue = styled.span<{ $muted?: boolean }>`
  ${({ theme }) => theme.typography.h3}
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  color: ${({ theme, $muted }) => ($muted ? theme.color.gray[600] : theme.color.gray[900])};
  letter-spacing: -0.02em;
`;

/* 배경은 두지 않는다. globalBarRatio 가 한쪽을 항상 1 로 주어 막대가 꽉 찬다. */
export const DiffBar = styled.div`
  position: relative;
  height: 6px;
  border-radius: 999px;
  overflow: hidden;
`;

export const DiffFill = styled.div<{ $ratio: number; $tone: 'mine' | 'global' }>`
  position: absolute;
  inset: 0 auto 0 0;
  width: ${({ $ratio }) => Math.min(100, Math.max(0, $ratio * 100))}%;
  border-radius: 999px;
  background: ${({ theme, $tone }) =>
    $tone === 'mine' ? theme.color.point[500] : theme.color.point[200]};
`;

export const DiffRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
`;

export const DiffCaption = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const PercentilePill = styled.span`
  ${({ theme }) => theme.typography.caption}
  font-weight: ${({ theme }) => theme.typography.h4.fontWeight};
  color: ${({ theme }) => theme.color.point[500]};
  padding: 3px 10px;
  border-radius: 20px;
  background: ${({ theme }) => theme.color.point[50]};
  white-space: nowrap;
  flex-shrink: 0;
`;

export const ErrorText = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  padding: 24px 0;
  margin: 0;
`;

/* ── 통계 안내 · 탈퇴 ── */

export const InfoSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
`;

export const InfoTitle = styled.h4`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
  padding-left: 4px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  margin: 0;
`;

export const TooltipCard = styled.div`
  padding: 16px;
  background: ${({ theme }) => theme.color.gray[50]};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 14px;
`;

export const TooltipList = styled.ul`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[500]};
  line-height: 1.7;
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const DangerCard = styled.div`
  ${card}
  display: flex;
  flex-direction: column;
  overflow: hidden;
  margin-top: 12px;
`;

export const DangerRow = styled.button`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 14px 18px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease;

  &:active {
    background: ${({ theme }) => theme.color.point[50]};
  }
`;

export const DangerLabel = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.point[500]};
`;

export const DangerIcon = styled.span`
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.point[200]};
`;
