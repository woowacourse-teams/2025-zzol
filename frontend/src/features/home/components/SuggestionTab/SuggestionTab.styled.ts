import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px 16px 32px;
  gap: 16px;
`;

/* 카테고리 탭 */
export const CategoryGrid = styled.div`
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
`;

export const CategoryTab = styled.button<{ $active: boolean }>`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 14px 8px;
  border-radius: 16px;
  border: 1.5px solid
    ${({ theme, $active }) => ($active ? theme.color.point[400] : theme.color.gray[100])};
  background: ${({ theme, $active }) => ($active ? theme.color.point[50] : theme.color.white)};
  cursor: pointer;
  transition: all 0.15s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);

  &:active {
    transform: scale(0.97);
  }
`;

export const TabIcon = styled.img<{ $active: boolean }>`
  width: 26px;
  height: 26px;
  object-fit: contain;
  opacity: ${({ $active }) => ($active ? 1 : 0.5)};
  transition: opacity 0.15s ease;
`;

export const TabLabel = styled.span<{ $active: boolean }>`
  font-size: 11px;
  font-weight: 600;
  color: ${({ theme, $active }) => ($active ? theme.color.point[500] : theme.color.gray[500])};
  white-space: nowrap;
  transition: color 0.15s ease;
`;

/* 게임 선택 (BUG 한정) */
export const GameSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  background: ${({ theme }) => theme.color.gray[50]};
  border-radius: 14px;
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
`;

export const GameSectionLabel = styled.p`
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
  margin: 0;
`;

export const GamePillRow = styled.div`
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
`;

export const GamePill = styled.button<{ $active: boolean }>`
  padding: 6px 13px;
  border-radius: 20px;
  border: 1.5px solid
    ${({ theme, $active }) => ($active ? theme.color.point[400] : theme.color.gray[200])};
  background: ${({ theme, $active }) => ($active ? theme.color.point[50] : theme.color.white)};
  font-size: 12px;
  font-weight: 500;
  color: ${({ theme, $active }) => ($active ? theme.color.point[500] : theme.color.gray[600])};
  cursor: pointer;
  transition: all 0.12s ease;

  &:active {
    transform: scale(0.96);
  }
`;

/* 텍스트에어리어 폼 */
export const FormSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

export const FormLabel = styled.p`
  font-size: 13px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[700]};
  margin: 0;
`;

export const Textarea = styled.textarea`
  width: 100%;
  min-height: 140px;
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
    color: ${({ theme }) => theme.color.gray[300]};
  }

  &:focus {
    border-color: ${({ theme }) => theme.color.point[400]};
    box-shadow: 0 0 0 3px ${({ theme }) => theme.color.point[50]};
  }
`;

export const FormFooter = styled.div`
  display: flex;
  justify-content: flex-end;
`;

export const CharCount = styled.span<{ $warn: boolean }>`
  font-size: 12px;
  color: ${({ theme, $warn }) => ($warn ? theme.color.point[400] : theme.color.gray[400])};
  transition: color 0.15s ease;
`;

/* 성공 화면 */
export const SuccessContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 32px;
  gap: 12px;
`;

export const SuccessIconWrap = styled.div`
  width: 72px;
  height: 72px;
  border-radius: 22px;
  background: ${({ theme }) => theme.color.point[50]};
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;

  img {
    width: 40px;
    height: 40px;
    object-fit: contain;
  }
`;

export const SuccessTitle = styled.p`
  font-size: 22px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  text-align: center;
  letter-spacing: -0.03em;
  margin: 0;
`;

export const SuccessDesc = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  margin: 0;
  line-height: 1.5;
`;

export const ResetButton = styled.button`
  margin-top: 16px;
  padding: 10px 24px;
  border-radius: 20px;
  border: 1.5px solid ${({ theme }) => theme.color.gray[200]};
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[600]};
  cursor: pointer;
  transition: all 0.15s ease;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
  }
`;
