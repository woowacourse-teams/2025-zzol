import styled from '@emotion/styled';

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

export const CardTitle = styled.h3`
  font-size: 15px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[800]};
  letter-spacing: -0.01em;
`;

export const List = styled.ul`
  display: flex;
  flex-direction: column;
  gap: 10px;
  list-style: none;
  padding: 0;
  margin: 0;
`;

export const Item = styled.li`
  display: flex;
  align-items: center;
  gap: 10px;
`;

export const GameRank = styled.span`
  width: 18px;
  font-size: 12px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[400]};
  flex-shrink: 0;
  text-align: center;
`;

export const GameInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
  min-width: 0;
`;

export const GameName = styled.span`
  font-size: 13px;
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[800]};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const BarTrack = styled.div`
  width: 100%;
  height: 6px;
  background: ${({ theme }) => theme.color.gray[100]};
  border-radius: 3px;
  overflow: hidden;
`;

export const BarFill = styled.div<{ $ratio: number }>`
  height: 100%;
  width: ${({ $ratio }) => Math.round($ratio * 100)}%;
  background: linear-gradient(
    90deg,
    ${({ theme }) => theme.color.point[400]},
    ${({ theme }) => theme.color.point[300]}
  );
  border-radius: 3px;
  transition: width 0.6s ease;
`;

export const PlayCount = styled.span`
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
  flex-shrink: 0;
  min-width: 32px;
  text-align: right;
`;

export const Empty = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  padding: 16px 0;
`;
