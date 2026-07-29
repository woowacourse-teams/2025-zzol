import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';
import { rankColorMap, tierColorMap, type TierColorKey } from '@/constants/color';
import { scoreboardReveal } from '@/styles/animations/scoreboardReveal';

const spin = keyframes`
  to { transform: rotate(360deg); }
`;

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

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 24px 16px 16px;
  animation: ${fadeSlideUp} 0.35s ease both;
`;

export const Title = styled.h2`
  ${({ theme }) => theme.typography.h4}
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.02em;
`;

export const Caption = styled.p`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
`;

/* ─── 내 순위 히어로 ─── */

export const HeroCard = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
  border-radius: 16px;
  background: linear-gradient(
    135deg,
    ${({ theme }) => theme.color.point[400]},
    ${({ theme }) => theme.color.point[500]}
  );
  box-shadow: 0 6px 18px rgba(245, 62, 65, 0.25);
  animation: ${fadeSlideUp} 0.35s ease 0.05s both;
`;

export const HeroTopRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

export const HeroLabel = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.point[100]};
  font-weight: 700;
  letter-spacing: 0.02em;
`;

export const HeroRankRow = styled.div`
  display: flex;
  align-items: baseline;
  gap: 8px;
`;

export const HeroRank = styled.span`
  ${({ theme }) => theme.typography.h1}
  font-weight: 800;
  color: ${({ theme }) => theme.color.white};
  line-height: 1;
`;

export const HeroRankUnit = styled.span`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.point[100]};
`;

export const HeroPoints = styled.span`
  ${({ theme }) => theme.typography.h4}
  font-weight: 700;
  color: ${({ theme }) => theme.color.white};
  margin-left: auto;
`;

export const HeroFooter = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.point[100]};
`;

/* ─── 티어 뱃지 ─── */

export const TierBadge = styled.span<{ $tier: TierColorKey }>`
  ${({ theme }) => theme.typography.caption}
  font-weight: 800;
  padding: 4px 10px;
  border-radius: 999px;
  background: ${({ $tier }) => tierColorMap[$tier].bg};
  color: ${({ $tier }) => tierColorMap[$tier].text};
  letter-spacing: 0.02em;
  white-space: nowrap;
`;

/* ─── 리더보드 ─── */

export const BoardCard = styled.div`
  display: flex;
  flex-direction: column;
  padding: 8px 12px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
`;

export const BoardHeader = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 8px 8px;
`;

export const BoardTitle = styled.h3`
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[800]};
`;

export const BoardCount = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const Row = styled.div<{ $isMe: boolean }>`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  border-radius: 10px;
  background: ${({ $isMe, theme }) => ($isMe ? theme.color.point[50] : 'transparent')};
  border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};
`;

export const Medal = styled.div<{ $rank: number }>`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  flex-shrink: 0;
  border-radius: 50%;
  ${({ theme }) => theme.typography.small}
  font-weight: 800;
  background: ${({ $rank, theme }) => rankColorMap[$rank] ?? theme.color.gray[100]};
  color: ${({ $rank, theme }) => ($rank <= 3 ? theme.color.gray[900] : theme.color.gray[500])};
`;

export const RowName = styled.div`
  display: flex;
  align-items: baseline;
  gap: 2px;
  min-width: 0;
  flex: 1;
`;

export const Nickname = styled.span`
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[900]};
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const UserCode = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
  flex-shrink: 0;
`;

export const RowPoints = styled.span`
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[800]};
  flex-shrink: 0;
`;

export const AnimatedItem = styled.div<{ $index: number }>`
  animation: ${scoreboardReveal} 0.38s cubic-bezier(0.22, 1, 0.36, 1) both;
  animation-delay: ${({ $index }) => `${$index * 0.07}s`};

  /* Row는 각 AnimatedItem의 유일한 자식이라 Row 쪽 last-of-type은 모든 행에 매치된다.
     마지막 행 판단은 형제 관계가 있는 이 래퍼에서 해야 한다 */
  &:last-of-type ${Row} {
    border-bottom: none;
  }
`;

/* ─── 빈 상태 · 로딩 ─── */

export const Empty = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 28px 0 24px;
`;

export const EmptyEmoji = styled.span`
  font-size: 34px;
  line-height: 1;
`;

export const EmptyText = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
`;

export const Spinner = styled.div`
  width: 20px;
  height: 20px;
  border: 2px solid ${({ theme }) => theme.color.gray[200]};
  border-top-color: ${({ theme }) => theme.color.point[400]};
  border-radius: 50%;
  animation: ${spin} 0.7s linear infinite;
  margin: 20px auto;
`;

/* ─── 포인트 안내 ─── */

export const GuideCard = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 16px 20px;
  background: ${({ theme }) => theme.color.gray[50]};
  border-radius: 16px;
`;

export const GuideTitle = styled.h3`
  ${({ theme }) => theme.typography.caption}
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const GuideList = styled.ul`
  display: flex;
  flex-direction: column;
  gap: 4px;
  list-style: none;
  padding: 0;
  margin: 0;

  li {
    ${({ theme }) => theme.typography.caption}
    color: ${({ theme }) => theme.color.gray[400]};
  }
`;
