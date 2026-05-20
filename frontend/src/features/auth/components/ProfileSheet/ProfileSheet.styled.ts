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
  ${({ theme }) => theme.typography.h4};
  flex-shrink: 0;
`;

export const NicknameRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
`;

export const NicknameText = styled.span`
  ${({ theme }) => theme.typography.h4};
  color: ${({ theme }) => theme.color.gray[900]};
`;

export const EditButton = styled.button`
  ${({ theme }) => theme.typography.small};
  color: ${({ theme }) => theme.color.point[400]};
  background: none;
  border: 1px solid ${({ theme }) => theme.color.point[400]};
  border-radius: 12px;
  padding: 3px 10px;
  cursor: pointer;
  flex-shrink: 0;

  &:active {
    background-color: ${({ theme }) => theme.color.point[50]};
  }
`;

export const EditRow = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
`;

export const NicknameInput = styled.input`
  ${({ theme }) => theme.typography.h4};
  color: ${({ theme }) => theme.color.gray[900]};
  border: 1px solid ${({ theme }) => theme.color.point[400]};
  border-radius: 8px;
  padding: 6px 10px;
  outline: none;
  width: 100%;
  background: ${({ theme }) => theme.color.white};

  &:disabled {
    opacity: 0.6;
  }
`;

export const EditActions = styled.div`
  display: flex;
  gap: 6px;
`;

export const SaveButton = styled.button`
  flex: 1;
  padding: 8px 0;
  border: none;
  border-radius: 8px;
  background-color: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  ${({ theme }) => theme.typography.h4};
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:disabled {
    opacity: 0.6;
    cursor: default;
  }

  &:active {
    background-color: ${({ theme }) => theme.color.point[500]};
  }
`;

export const CancelButton = styled.button`
  flex: 1;
  padding: 8px 0;
  border: none;
  border-radius: 8px;
  background-color: ${({ theme }) => theme.color.gray[100]};
  color: ${({ theme }) => theme.color.gray[700]};
  ${({ theme }) => theme.typography.h4};
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:disabled {
    opacity: 0.6;
    cursor: default;
  }

  &:active {
    background-color: ${({ theme }) => theme.color.gray[200]};
  }
`;

export const LogoutButton = styled.button`
  width: 100%;
  padding: 10px 0;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 10px;
  background: none;
  color: ${({ theme }) => theme.color.gray[500]};
  ${({ theme }) => theme.typography.small};
  cursor: pointer;
  text-align: center;

  &:active {
    background-color: ${({ theme }) => theme.color.gray[100]};
  }
`;
