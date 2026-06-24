import styled from '@emotion/styled';

export const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 8px 0 4px;
`;

export const WarningBox = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[100]};
  border-radius: 12px;
`;

export const WarningIcon = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: ${({ theme }) => theme.color.point[100]};
  color: ${({ theme }) => theme.color.point[500]};
  flex-shrink: 0;
`;

export const WarningText = styled.p`
  font-size: 13px;
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[700]};
  line-height: 1.5;
  margin: 0;
`;

export const InfoList = styled.ul`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[500]};
  line-height: 1.7;
  list-style: none;
  margin: 0;
  padding: 0 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
`;

export const ButtonGroup = styled.div`
  display: flex;
  gap: 10px;
  margin-top: 4px;
`;

export const CancelButton = styled.button`
  flex: 1;
  padding: 14px 0;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 12px;
  background: ${({ theme }) => theme.color.white};
  color: ${({ theme }) => theme.color.gray[700]};
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s ease;

  &:active {
    background-color: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const DeleteButton = styled.button`
  flex: 1;
  padding: 14px 0;
  border: none;
  border-radius: 12px;
  background: ${({ theme }) => theme.color.point[500]};
  color: ${({ theme }) => theme.color.white};
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.15s ease;

  &:active {
    filter: brightness(0.88);
  }

  &:disabled {
    opacity: 0.6;
    cursor: default;
  }
`;
