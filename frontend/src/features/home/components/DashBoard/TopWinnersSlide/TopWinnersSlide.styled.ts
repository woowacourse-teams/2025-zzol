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
  gap: 2px;
  list-style: none;
  padding: 0;
  margin: 0;
`;

export const Item = styled.li`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};
  border-radius: 8px;
  transition: background 0.15s ease;

  &:first-of-type {
    background: rgba(245, 166, 35, 0.06);
  }

  &:last-of-type {
    border-bottom: none;
  }
`;

const RANK_COLORS = ['#F5A623', '#9EAAB8', '#BE8C5A'] as const;

export const Rank = styled.span<{ $rank: number }>`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 800;
  flex-shrink: 0;
  background: ${({ $rank }) => ($rank <= 3 ? RANK_COLORS[$rank - 1] : 'transparent')};
  color: ${({ $rank, theme }) => ($rank <= 3 ? '#fff' : theme.color.gray[400])};
  border: ${({ $rank, theme }) => ($rank > 3 ? `1px solid ${theme.color.gray[200]}` : 'none')};
`;

export const Name = styled.span`
  flex: 1;
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[800]};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const Count = styled.span`
  font-size: 13px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
  flex-shrink: 0;
`;

export const Empty = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  padding: 16px 0;
`;
