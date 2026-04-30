import { ComponentProps, MouseEvent, ReactElement } from 'react';
import * as S from './GameActionButton.styled';

type Props = {
  onClick: () => void;
  isSelected: boolean;
  isDisabled: boolean;
  gameName: string;
  description?: string[];
  icon: ReactElement;
  orderNumber?: number;
  onInfoClick?: () => void;
  onSettingClick?: () => void;
} & Omit<ComponentProps<'button'>, 'onClick'>;

const GameActionButton = ({
  onClick,
  isSelected,
  isDisabled,
  gameName,
  icon,
  orderNumber,
  onInfoClick,
  onSettingClick,
  ...rest
}: Props) => {
  const handleClick = () => {
    if (isDisabled) return;
    onClick();
  };

  const handleInfoClick = (e: MouseEvent) => {
    e.stopPropagation();
    onInfoClick?.();
  };

  const handleSettingClick = (e: MouseEvent) => {
    e.stopPropagation();
    onSettingClick?.();
  };

  return (
    <S.Container onClick={handleClick} $isSelected={isSelected} $disabled={isDisabled} {...rest}>
      {isSelected && orderNumber && <S.NumberBadge>{orderNumber}</S.NumberBadge>}
      <S.InfoButton
        $isSelected={isSelected}
        onClick={handleInfoClick}
        aria-label={`${gameName} 정보`}
      >
        i
      </S.InfoButton>
      <S.GameIcon $isSelected={isSelected}>{icon}</S.GameIcon>
      <S.GameName $isSelected={isSelected}>{gameName}</S.GameName>
      <S.SettingsButton
        $isSelected={isSelected}
        onClick={handleSettingClick}
        aria-label={`${gameName} 설정`}
      >
        ⚙
      </S.SettingsButton>
    </S.Container>
  );
};

export default GameActionButton;
