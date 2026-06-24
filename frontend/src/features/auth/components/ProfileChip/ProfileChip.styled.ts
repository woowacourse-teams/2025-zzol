import styled from '@emotion/styled';

export const Button = styled.button`
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 999px;
  background: ${({ theme }) => theme.color.white};
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:active {
    background-color: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const LoginLabel = styled.span`
  ${({ theme }) => theme.typography.small};
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const Avatar = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background-color: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  font-size: 12px;
  font-weight: 700;
`;

export const PersonIcon = styled.svg`
  width: 16px;
  height: 16px;
  color: ${({ theme }) => theme.color.gray[600]};
`;
