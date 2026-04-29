import styled from '@emotion/styled';

type Props = {
  $isSelected: boolean;
  $disabled?: boolean;
};

export const Container = styled.button<Props>`
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;

  background-color: ${({ theme, $isSelected }) =>
    $isSelected ? theme.color.point[400] : theme.color.white};

  border: ${({ theme, $isSelected }) =>
    $isSelected ? `3px solid ${theme.color.point[200]}` : `1px solid ${theme.color.point[200]}`};
  border-radius: 12px;

  width: 100%;
  aspect-ratio: 1 / 1;
  padding: 16px 12px;

  cursor: default;

  ${({ $disabled }) =>
    !$disabled &&
    `
    cursor: pointer;
    transition: transform 0.2s ease;

    &:active {
      transform: scale(0.96);
    }
  `}
`;

export const GameIcon = styled.div<Pick<Props, '$isSelected'>>`
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 56px;
    height: 56px;
    object-fit: contain;
  }
`;

export const GameName = styled.span<Pick<Props, '$isSelected'>>`
  ${({ theme }) => theme.typography.h4}
  font-size: 14px;
  font-weight: 700;
  color: ${({ theme, $isSelected }) => ($isSelected ? theme.color.white : theme.color.point[400])};
  text-align: center;
  word-break: keep-all;
`;

export const NumberBadge = styled.div`
  position: absolute;
  top: 8px;
  left: 8px;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: ${({ theme }) => theme.color.white};
  font-weight: bold;
  font-size: 14px;
  background-color: rgb(251, 164, 164);
  border-radius: 50%;
  box-shadow: 1px 1px 4px ${({ theme }) => theme.color.point[500]};
`;

export const InfoButton = styled.button<Pick<Props, '$isSelected'>>`
  position: absolute;
  top: 6px;
  right: 6px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background-color: ${({ theme, $isSelected }) =>
    $isSelected ? 'rgba(255, 255, 255, 0.25)' : theme.color.gray[100]};
  color: ${({ theme, $isSelected }) => ($isSelected ? theme.color.white : theme.color.gray[500])};
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  z-index: 1;

  &:hover {
    background-color: ${({ theme, $isSelected }) =>
      $isSelected ? 'rgba(255, 255, 255, 0.4)' : theme.color.gray[200]};
  }
`;

export const SettingsButton = styled.button<Pick<Props, '$isSelected'>>`
  position: absolute;
  bottom: 6px;
  right: 6px;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  background-color: ${({ theme, $isSelected }) =>
    $isSelected ? 'rgba(255, 255, 255, 0.25)' : theme.color.gray[100]};
  color: ${({ theme, $isSelected }) => ($isSelected ? theme.color.white : theme.color.gray[400])};
  font-size: 16px;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  z-index: 1;
  visibility: hidden;

  &:hover {
    background-color: ${({ theme, $isSelected }) =>
      $isSelected ? 'rgba(255, 255, 255, 0.4)' : theme.color.gray[200]};
  }
`;

export const Icon = styled.img`
  width: 56px;
`;

/* 기존 호환 */
export const Wrapper = styled.div``;
export const DescriptionWrapper = styled.div<Props>``;
export const Description = styled.p``;
