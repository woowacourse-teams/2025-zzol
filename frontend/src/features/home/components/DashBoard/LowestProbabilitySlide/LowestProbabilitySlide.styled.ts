import styled from '@emotion/styled';

export const Card = styled.div`
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px;
  background: linear-gradient(
    135deg,
    ${({ theme }) => theme.color.point[500]} 0%,
    ${({ theme }) => theme.color.point[400]} 70%,
    ${({ theme }) => theme.color.point[300]} 100%
  );
  border-radius: 16px;
  box-shadow: 0 4px 16px rgba(255, 75, 75, 0.25);

  &::before {
    content: '';
    position: absolute;
    top: -30px;
    right: -20px;
    width: 110px;
    height: 110px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -40px;
    right: 30px;
    width: 80px;
    height: 80px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.07);
    pointer-events: none;
  }
`;

export const Header = styled.div`
  display: flex;
  flex-direction: column;
  gap: 3px;
`;

export const CardTitle = styled.h3`
  font-size: 14px;
  font-weight: 700;
  color: rgba(255, 255, 255, 0.9);
`;

export const Sub = styled.p`
  font-size: 12px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.6);
`;

/* Main content: probability (left) | divider | winner (right) */
export const Content = styled.div`
  display: flex;
  align-items: stretch;
  gap: 20px;
`;

export const ProbSection = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  flex-shrink: 0;
`;

export const BigProb = styled.span`
  font-size: 44px;
  font-weight: 900;
  color: ${({ theme }) => theme.color.white};
  letter-spacing: -0.05em;
  line-height: 1;
`;

export const ProbLabel = styled.span`
  font-size: 11px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.65);
  letter-spacing: 0.02em;
`;

export const VerticalDivider = styled.div`
  width: 1px;
  background: rgba(255, 255, 255, 0.25);
  flex-shrink: 0;
`;

export const WinnerSection = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 6px;
  min-width: 0;
  flex: 1;
`;

export const WinnerLabel = styled.span`
  font-size: 11px;
  font-weight: 600;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 0.01em;
`;

export const WinnerRow = styled.div`
  display: flex;
  align-items: baseline;
  gap: 6px;
  min-width: 0;
`;

export const WinnerName = styled.span`
  font-size: 20px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.white};
  letter-spacing: -0.03em;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  min-width: 0;
`;

export const WinnerCode = styled.span`
  font-size: 11px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.6);
  letter-spacing: 0.01em;
  flex-shrink: 0;
`;

export const MultiWinnerList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

export const Empty = styled.p`
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
  padding: 12px 0;
`;
