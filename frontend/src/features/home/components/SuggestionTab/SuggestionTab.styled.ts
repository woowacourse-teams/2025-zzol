import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px;
  gap: 16px;
  height: 100%;
`;

/* 카테고리 선택 step — 수직 중앙 정렬 */
export const CategoryWrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  flex: 1;
  gap: 16px;
`;

export const StepHeader = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

export const StepLabel = styled.p`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.gray[700]};
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
  gap: 6px;
  padding: 16px 12px;
  border-radius: 12px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  background: ${({ theme }) => theme.color.gray[50]};
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    border-color: ${({ theme }) => theme.color.point[300]};
    background: ${({ theme }) => theme.color.point[50]};
  }

  &:active {
    transform: scale(0.97);
  }
`;

export const ChipIcon = styled.span`
  font-size: 24px;
  line-height: 1;
`;

export const ChipLabel = styled.span`
  ${({ theme }) => theme.typography.small}
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const Textarea = styled.textarea`
  width: 100%;
  min-height: 120px;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  background: ${({ theme }) => theme.color.gray[50]};
  resize: none;
  outline: none;
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.gray[800]};
  box-sizing: border-box;

  &::placeholder {
    color: ${({ theme }) => theme.color.gray[400]};
  }

  &:focus {
    border-color: ${({ theme }) => theme.color.point[300]};
  }
`;

export const CharCount = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: right;
  margin-top: -8px;
`;

export const SubmitButton = styled.button`
  width: 100%;
  height: 48px;
  border-radius: 10px;
  border: none;
  background: ${({ theme, disabled }) =>
    disabled ? theme.color.gray[200] : theme.color.point[400]};
  color: ${({ theme, disabled }) =>
    disabled ? theme.color.gray[400] : theme.color.white};
  ${({ theme }) => theme.typography.h4}
  cursor: ${({ disabled }) => (disabled ? 'default' : 'pointer')};
  transition: all 0.15s ease;

  &:not(:disabled):active {
    transform: scale(0.98);
    background: ${({ theme }) => theme.color.point[500]};
  }
`;

export const BackButton = styled.button`
  border: none;
  background: transparent;
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[400]};
  cursor: pointer;
  padding: 0;
  text-align: left;
  width: fit-content;

  &:hover {
    color: ${({ theme }) => theme.color.gray[600]};
  }
`;

/* Success */
export const SuccessBox = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  height: 100%;
`;

export const SuccessIcon = styled.span`
  font-size: 40px;
  margin-bottom: 4px;
`;

export const SuccessTitle = styled.p`
  ${({ theme }) => theme.typography.h3}
  color: ${({ theme }) => theme.color.gray[800]};
`;

export const SuccessDesc = styled.p`
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const ResetButton = styled.button`
  margin-top: 12px;
  border: none;
  background: transparent;
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.point[400]};
  cursor: pointer;
  text-decoration: underline;
`;

/* Info */
export const InfoBox = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border-radius: 12px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  background: ${({ theme }) => theme.color.gray[50]};
`;

export const InfoTitle = styled.p`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.gray[800]};
`;

export const InfoRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
`;

export const InfoLabel = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const InfoValue = styled.span`
  ${({ theme }) => theme.typography.small}
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const Divider = styled.hr`
  border: none;
  border-top: 1px solid ${({ theme }) => theme.color.gray[200]};
  margin: 0;
`;

export const InfoLinkButton = styled.a`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  background: ${({ theme }) => theme.color.white};
  text-decoration: none;
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.gray[700]};
  font-weight: 600;
  transition: all 0.15s ease;

  &:hover {
    border-color: ${({ theme }) => theme.color.point[300]};
    background: ${({ theme }) => theme.color.point[50]};
    color: ${({ theme }) => theme.color.point[400]};
  }
`;
