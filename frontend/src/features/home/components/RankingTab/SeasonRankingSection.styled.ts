import styled from '@emotion/styled';

/** 이달의 통계 카드들(DashBoard.Container: padding 0 16px)과 좌우 정렬을 맞춘다 */
export const CardWrapper = styled.div`
  padding: 0 16px;
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

export const TitleRow = styled.div`
  display: flex;
  align-items: baseline;
  justify-content: space-between;
`;

export const CardTitle = styled.h3`
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[800]};
  letter-spacing: -0.01em;
`;

export const SeasonLabel = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
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
