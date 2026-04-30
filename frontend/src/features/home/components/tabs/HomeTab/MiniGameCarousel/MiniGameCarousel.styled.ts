import styled from '@emotion/styled';

export const Grid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
`;

export const GameCard = styled.button`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 18px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  cursor: pointer;
  text-align: left;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: background 0.15s ease, transform 0.12s ease;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
    transform: scale(0.96);
  }
`;

export const IconWrapper = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: ${({ theme }) => theme.color.point[50]};
`;

export const GameIcon = styled.img`
  width: 28px;
  height: 28px;
  user-select: none;
  -webkit-user-drag: none;
`;

export const GameMeta = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const GameName = styled.span`
  font-size: 13px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[900]};
  line-height: 1.3;
  letter-spacing: -0.01em;
`;

export const GameDesc = styled.span`
  font-size: 11px;
  font-weight: 400;
  color: ${({ theme }) => theme.color.gray[400]};
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
`;
