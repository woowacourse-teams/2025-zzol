import styled from '@emotion/styled';

export const Card = styled.button`
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 100%;
  min-height: 140px;
  padding: 20px 18px;
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 20px;
  background: ${({ theme }) => theme.color.white};
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  text-align: left;
  transition:
    background-color 0.15s ease,
    transform 0.12s ease;

  &:active {
    background-color: ${({ theme }) => theme.color.gray[50]};
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
  background: ${({ theme }) => theme.color.point[50]};
  color: ${({ theme }) => theme.color.point[500]};
  font-size: 16px;
  font-weight: 800;
  margin-bottom: 6px;
  flex-shrink: 0;
`;

export const Title = styled.span`
  font-size: 17px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.03em;
  line-height: 1.2;
`;

export const Sub = styled.span`
  font-size: 13px;
  font-weight: 400;
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const Arrow = styled.span`
  align-self: flex-end;
  font-size: 20px;
  color: ${({ theme }) => theme.color.gray[300]};
`;
