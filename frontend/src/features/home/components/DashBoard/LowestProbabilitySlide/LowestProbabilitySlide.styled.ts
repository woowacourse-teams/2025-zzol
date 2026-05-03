import styled from '@emotion/styled';

export const Card = styled.div`
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(
    135deg,
    ${({ theme }) => theme.color.point[500]} 0%,
    ${({ theme }) => theme.color.point[400]} 70%,
    #ff8c7a 100%
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
  color: rgba(255, 255, 255, 0.65);
`;

export const Content = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
`;

export const ProbRow = styled.div`
  display: flex;
  align-items: baseline;
  gap: 8px;
`;

export const BigProb = styled.span`
  font-size: 40px;
  font-weight: 900;
  color: ${({ theme }) => theme.color.white};
  letter-spacing: -0.04em;
  line-height: 1;
`;

export const ProbLabel = styled.span`
  font-size: 13px;
  font-weight: 500;
  color: rgba(255, 255, 255, 0.75);
`;

export const Divider = styled.div`
  height: 1px;
  background: rgba(255, 255, 255, 0.2);
`;

export const Names = styled.p`
  font-size: 15px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.white};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const Empty = styled.p`
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
  text-align: center;
  padding: 12px 0;
`;
