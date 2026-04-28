import styled from '@emotion/styled';

const SPACING = {
  PADDING_VERTICAL: 20,
  PADDING_HORIZONTAL: 18,
  ICON_SIZE: 23,
  BORDER_DIFFERENCE: 2,
} as const;

const getAdjustedPadding = (isSelected: boolean) => {
  if (isSelected) {
    return {
      vertical: SPACING.PADDING_VERTICAL - SPACING.BORDER_DIFFERENCE,
      horizontal: SPACING.PADDING_HORIZONTAL - SPACING.BORDER_DIFFERENCE,
    };
  }
  return {
    vertical: SPACING.PADDING_VERTICAL,
    horizontal: SPACING.PADDING_HORIZONTAL,
  };
};

type Props = {
  $isSelected: boolean;
  $disabled?: boolean;
};

export const Container = styled.button<Props>`
  position: relative;
  display: flex;
  justify-content: space-between;
  background-color: ${({ theme, $isSelected }) =>
    $isSelected ? theme.color.point[400] : theme.color.white};

  border: ${({ theme, $isSelected }) =>
    $isSelected ? `3px solid ${theme.color.point[200]}` : `1px solid ${theme.color.point[200]}`};
  border-radius: 12px;

  width: 100%;
  height: 130px;
  padding: ${({ $isSelected }) => getAdjustedPadding($isSelected).vertical}px
    ${({ $isSelected }) => getAdjustedPadding($isSelected).horizontal}px;

  cursor: default;

  ${({ $disabled }) =>
    !$disabled &&
    `
    cursor: pointer;
    transition: transform 0.2s ease;

    &:active {
      transform: scale(0.98);
    }
  `}
`;

export const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
`;

export const DescriptionWrapper = styled.div<Props>`
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  margin-top: 8px;
  color: ${({ theme, $isSelected }) => ($isSelected ? theme.color.white : theme.color.point[300])};
`;

export const GameIcon = styled.div<Props>`
  position: absolute;
  bottom: ${({ $isSelected }) => getAdjustedPadding($isSelected).vertical}px;
  right: ${({ $isSelected }) => getAdjustedPadding($isSelected).horizontal}px;
`;

export const Description = styled.p`
  ${({ theme }) => theme.typography.small}
`;

export const Icon = styled.img`
  width: 20px;
`;

export const NumberBadge = styled.div`
  position: absolute;
  top: 12px;
  right: 12px;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.color.white};
  font-weight: bold;
  font-size: 18px;
  background-color: rgb(251, 164, 164);
  border-radius: 50%;
  box-shadow: 1px 1px 4px ${({ theme }) => theme.color.point[500]};
`;
