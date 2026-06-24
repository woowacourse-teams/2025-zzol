import styled from '@emotion/styled';

export const Container = styled.section`
  display: flex;
  flex-direction: column;
  gap: 35px;
  justify-content: center;
  height: 100%;
`;

export const Wrapper = styled.section`
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const HeadlineRow = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
`;

export const RandomButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex-shrink: 0;

  height: 32px;
  padding: 0 12px;
  border: none;
  border-radius: 16px;
  background: ${({ theme }) => theme.color.point[50]};
  cursor: pointer;
  transition: all 0.15s ease;

  color: ${({ theme }) => theme.color.point[400]};
  font-size: 14px;
  font-weight: 600;

  &:active {
    transform: scale(0.95);
    background: ${({ theme }) => theme.color.point[100]};
  }

  &:disabled {
    cursor: default;
    opacity: 0.5;
    transform: none;
  }
`;

export const ProgressWrapper = styled.div<{ $hasRecent: boolean }>`
  display: flex;
  align-items: center;
  justify-content: ${({ $hasRecent }) => ($hasRecent ? 'space-between' : 'flex-end')};
`;

export const RecentNicknamesLabel = styled.span`
  font-size: 11px;
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const NicknameChipList = styled.ul`
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  list-style: none;
  padding: 0;
  margin: 0;
`;

export const NicknameChip = styled.li`
  display: flex;
  align-items: center;
  gap: 6px;
  height: 34px;
  padding: 0 12px;
  border-radius: 17px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  background: ${({ theme }) => theme.color.gray[50]};
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    border-color: ${({ theme }) => theme.color.point[300]};
    background: ${({ theme }) => theme.color.point[50]};
  }

  &:active {
    transform: scale(0.96);
  }
`;

export const NicknameChipText = styled.span`
  font-size: 14px;
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const NicknameChipDeleteButton = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: ${({ theme }) => theme.color.gray[300]};
  color: ${({ theme }) => theme.color.gray[600]};
  font-size: 10px;
  line-height: 1;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s ease;

  &:hover {
    background: ${({ theme }) => theme.color.gray[400]};
    color: ${({ theme }) => theme.color.white};
  }
`;
