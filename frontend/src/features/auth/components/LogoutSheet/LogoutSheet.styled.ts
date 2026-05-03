import styled from '@emotion/styled';

export const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 8px 0 4px;
`;

export const UserInfo = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 0;
  border-bottom: 1px solid ${({ theme }) => theme.color.gray[100]};
  margin-bottom: 4px;
`;

export const Avatar = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
`;

export const NicknameText = styled.span`
  ${({ theme }) => theme.typography.h4};
  color: ${({ theme }) => theme.color.gray[900]};
`;

export const LogoutButton = styled.button`
  width: 100%;
  padding: 14px 0;
  border: none;
  border-radius: 12px;
  background-color: ${({ theme }) => theme.color.gray[100]};
  color: ${({ theme }) => theme.color.gray[700]};
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:active {
    background-color: ${({ theme }) => theme.color.gray[200]};
  }
`;
