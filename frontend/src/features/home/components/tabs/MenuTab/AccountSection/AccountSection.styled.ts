import styled from '@emotion/styled';

export const Section = styled.div`
  margin: 0 0 12px;
`;

export const Card = styled.div`
  margin: 16px 16px 8px;
  padding: 18px 20px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
`;

export const UserRow = styled.div`
  display: flex;
  align-items: center;
  gap: 14px;
`;

export const Avatar = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(
    135deg,
    ${({ theme }) => theme.color.point[500]},
    ${({ theme }) => theme.color.point[300]}
  );
  color: ${({ theme }) => theme.color.white};
  font-size: 18px;
  font-weight: 800;
  flex-shrink: 0;
  letter-spacing: -0.02em;
`;

export const UserInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
  flex: 1;
  min-width: 0;
`;

export const NicknameRow = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const Nickname = styled.span`
  ${({ theme }) => theme.typography.h4};
  color: ${({ theme }) => theme.color.gray[900]};
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
`;

export const EditButton = styled.button`
  padding: 2px 8px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 6px;
  background: transparent;
  color: ${({ theme }) => theme.color.gray[500]};
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.15s ease;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const EditRow = styled.div`
  display: flex;
  flex-direction: column;
  gap: 6px;
`;

export const NicknameInput = styled.input`
  width: 100%;
  padding: 6px 10px;
  border: 1.5px solid ${({ theme }) => theme.color.point[400]};
  border-radius: 8px;
  background: ${({ theme }) => theme.color.white};
  color: ${({ theme }) => theme.color.gray[900]};
  font-size: 14px;
  font-weight: 600;
  outline: none;

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
  padding: 5px 0;
  border: none;
  border-radius: 6px;
  background: ${({ theme }) => theme.color.point[500]};
  color: ${({ theme }) => theme.color.white};
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s ease;

  &:disabled {
    opacity: 0.6;
    cursor: default;
  }
`;

export const CancelButton = styled.button`
  flex: 1;
  padding: 5px 0;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 6px;
  background: transparent;
  color: ${({ theme }) => theme.color.gray[500]};
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.15s ease;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
  }

  &:disabled {
    opacity: 0.6;
    cursor: default;
  }
`;

export const Provider = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const UserCodeRow = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
`;

export const UserCode = styled.span`
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
  letter-spacing: 0.04em;
`;

export const CopyButton = styled.button`
  padding: 1px 7px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 5px;
  background: transparent;
  color: ${({ theme }) => theme.color.gray[400]};
  font-size: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.12s;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const LogoutButton = styled.button`
  padding: 7px 14px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 8px;
  background: ${({ theme }) => theme.color.white};
  color: ${({ theme }) => theme.color.gray[600]};
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  transition: background-color 0.15s ease;

  &:active {
    background-color: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const LoginCard = styled.button`
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: calc(100% - 32px);
  margin: 16px 16px 8px;
  padding: 18px 20px;
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 20px;
  background: ${({ theme }) => theme.color.white};
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  text-align: left;
  transition: background-color 0.12s ease;

  &:active {
    background-color: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const LoginLeft = styled.div`
  display: flex;
  align-items: center;
  gap: 14px;
`;

export const LoginAvatar = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: ${({ theme }) => theme.color.gray[100]};
  flex-shrink: 0;
`;

export const LoginTextGroup = styled.div`
  display: flex;
  flex-direction: column;
  gap: 3px;
`;

export const LoginTitle = styled.span`
  font-size: 15px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[900]};
`;

export const LoginSub = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const LoginArrow = styled.span`
  font-size: 20px;
  color: ${({ theme }) => theme.color.point[400]};
`;
