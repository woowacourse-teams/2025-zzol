import styled from '@emotion/styled';

/** 내 줄임을 알리는 틴트. 목록을 훑을 때 덩어리로 잡힌다 */
export const MyRow = styled.div`
  background-color: ${({ theme }) => theme.color.point[50]};
  border-radius: 12px;
  padding: 0 12px;
  margin: 0 -12px;
`;

export const ScrollableWrapper = styled.div`
  overflow-y: auto;
  margin-bottom: 1.6rem;
  height: 100%;
`;

export const BottomGap = styled.div`
  height: 3rem;
`;

export const Empty = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  ${({ theme }) => theme.typography.paragraph};
  color: ${({ theme }) => theme.color.gray[400]};
`;

/** 확률은 세로로 쌓이므로 자릿수 폭을 고정해 소수점을 맞춘다 */
export const Percent = styled.span`
  font-variant-numeric: tabular-nums;
`;
