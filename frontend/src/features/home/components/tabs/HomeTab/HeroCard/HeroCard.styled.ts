import styled from '@emotion/styled';

export const Card = styled.button`
  position: relative;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 100%;
  min-height: 140px;
  padding: 20px 18px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(
    140deg,
    ${({ theme }) => theme.color.point[500]} 0%,
    ${({ theme }) => theme.color.point[400]} 60%,
    #ff8c7a 100%
  );
  cursor: pointer;
  text-align: left;
  transition:
    opacity 0.15s ease,
    transform 0.12s ease;

  &::before {
    content: '';
    position: absolute;
    top: -40px;
    right: -30px;
    width: 140px;
    height: 140px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.1);
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    bottom: -50px;
    right: 40px;
    width: 100px;
    height: 100px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.07);
    pointer-events: none;
  }

  &:active {
    opacity: 0.92;
    transform: scale(0.985);
  }
`;

export const Top = styled.div`
  display: flex;
  flex-direction: column;
  gap: 6px;
`;

export const IconCircle = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.95);
  font-size: 20px;
  font-weight: 400;
  margin-bottom: 6px;
  flex-shrink: 0;
`;

export const Title = styled.span`
  font-size: 17px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.white};
  letter-spacing: -0.03em;
  line-height: 1.2;
  white-space: pre-line;
`;

export const Description = styled.span`
  font-size: 13px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.82);
  line-height: 1.6;
  white-space: pre-line;
`;

export const Arrow = styled.span`
  align-self: flex-end;
  font-size: 22px;
  color: rgba(255, 255, 255, 0.95);
`;

export const Badge = styled.span`
  display: inline-flex;
  align-items: center;
  padding: 3px 9px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.2);
  color: rgba(255, 255, 255, 0.95);
  letter-spacing: 0.01em;
  width: fit-content;
  margin-bottom: 2px;
`;
