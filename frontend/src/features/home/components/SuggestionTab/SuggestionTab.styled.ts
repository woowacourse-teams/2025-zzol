import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px 16px;
  gap: 16px;
  height: 100%;
`;

export const CenterWrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: stretch;
  flex: 1;
  gap: 14px;
`;

export const ChipGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
`;

export const CategoryChip = styled.button<{ $fullWidth?: boolean }>`
  ${({ $fullWidth }) => $fullWidth && 'grid-column: 1 / -1;'}

  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 20px 12px;
  border-radius: 16px;
  border: 1.5px solid ${({ theme }) => theme.color.gray[100]};
  background: ${({ theme }) => theme.color.white};
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  &:active {
    border-color: ${({ theme }) => theme.color.point[300]};
    background: ${({ theme }) => theme.color.point[50]};
    transform: scale(0.97);
  }
`;

export const ChipIcon = styled.img`
  width: 30px;
  height: 30px;
  object-fit: contain;
`;

export const ChipLabel = styled.span`
  font-size: 13px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const Textarea = styled.textarea`
  width: 100%;
  min-height: 130px;
  padding: 14px;
  border-radius: 14px;
  border: 1.5px solid ${({ theme }) => theme.color.gray[100]};
  background: ${({ theme }) => theme.color.white};
  resize: none;
  outline: none;
  font-size: 14px;
  line-height: 1.6;
  color: ${({ theme }) => theme.color.gray[800]};
  box-sizing: border-box;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  &::placeholder {
    color: ${({ theme }) => theme.color.gray[400]};
  }

  &:focus {
    border-color: ${({ theme }) => theme.color.point[400]};
    box-shadow: 0 0 0 3px ${({ theme }) => theme.color.point[50]};
  }
`;

export const CharCount = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: right;
  margin-top: -6px;
`;

/* Success */
export const SuccessIconWrap = styled.div`
  width: 64px;
  height: 64px;
  border-radius: 20px;
  background: ${({ theme }) => theme.color.point[50]};
  display: flex;
  align-items: center;
  justify-content: center;
  align-self: center;
  margin-bottom: 4px;
`;

export const SuccessIcon = styled.img`
  width: 36px;
  height: 36px;
`;

export const SuccessTitle = styled.p`
  font-size: 20px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  text-align: center;
  letter-spacing: -0.02em;
  margin: 0;
`;

export const SuccessDesc = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  margin: 0;
`;

export const ResetButton = styled.button`
  margin-top: 8px;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.point[400]};
  cursor: pointer;
  text-decoration: underline;
  text-align: center;
`;
