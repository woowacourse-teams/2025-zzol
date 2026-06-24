import { RankColorKey, rankColorMap } from '@/constants/color';
import styled from '@emotion/styled';

type RankNumberProps = {
  $rank: number;
};

type PlayerCardWrapperProps = {
  $isHighlighted?: boolean;
};

export const Banner = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 10px;
  height: 100%;
  text-align: center;
`;

export const DescriptionWrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
`;

export const ResultList = styled.section`
  display: flex;
  flex-direction: column;
  gap: 8px;
  height: 100%;
  overflow: scroll;
`;

export const PlayerCardWrapper = styled.div<PlayerCardWrapperProps>`
  display: flex;
  align-items: center;
  padding: 0 20px 0 8px;
  gap: 24px;
  border-radius: 12px;
  background-color: ${({ $isHighlighted, theme }) =>
    $isHighlighted ? theme.color.point[100] : 'transparent'};
`;

export const RankNumber = styled.div<RankNumberProps>`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 35px;
  height: 35px;
  border-radius: 12px;
  background-color: ${({ $rank }) => rankColorMap[$rank as RankColorKey] ?? '#none'};
  font-size: 20px;
  font-weight: 600;
  color: ${({ $rank }) => ($rank <= 3 ? '#ffffff' : '#666')};
`;
