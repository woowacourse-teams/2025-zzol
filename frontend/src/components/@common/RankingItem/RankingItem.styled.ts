import { RankColorKey, rankColorMap } from '@/constants/color';
import styled from '@emotion/styled';

type Props = {
  $rank: number;
};

export const Container = styled.div`
  display: flex;
  align-items: center;
  gap: 0.5rem;
  background-color: ${({ theme }) => theme.color.white};
  padding: 0.6rem;
  border-radius: 8px;
`;

export const RankNumber = styled.div<Props>`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 35px;
  height: 35px;
  border-radius: 12px;
  background-color: ${({ $rank, theme }) =>
    rankColorMap[$rank as RankColorKey] ?? theme.color.white};
  flex-shrink: 0;
`;

export const Content = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  gap: 1rem;
`;
