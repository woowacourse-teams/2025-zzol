import { ComponentProps, KeyboardEvent, MouseEvent, ReactElement } from 'react';
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

  const handleInfoKeyDown = (e: KeyboardEvent) => {
    if (e.key !== 'Enter' && e.key !== ' ') return;
    e.preventDefault();
    e.stopPropagation();
    onInfoClick?.();
  };

  const handleSettingClick = (e: MouseEvent) => {
    e.stopPropagation();
    onSettingClick?.();
  };

  const handleSettingKeyDown = (e: KeyboardEvent) => {
    if (e.key !== 'Enter' && e.key !== ' ') return;
    e.preventDefault();
    e.stopPropagation();
    onSettingClick?.();
  };

  return (
    <S.Container onClick={handleClick} $isSelected={isSelected} $disabled={isDisabled} {...rest}>
      {isSelected && orderNumber && <S.NumberBadge>{orderNumber}</S.NumberBadge>}
      <S.InfoButton
        $isSelected={isSelected}
        role="button"
        tabIndex={0}
        onClick={handleInfoClick}
        onKeyDown={handleInfoKeyDown}
        aria-label={`${gameName} 정보`}
      >
        i
      </S.InfoButton>
      <S.GameIcon $isSelected={isSelected}>{icon}</S.GameIcon>
      <S.GameName $isSelected={isSelected}>{gameName}</S.GameName>
      <S.SettingsButton
        $isSelected={isSelected}
        role="button"
        tabIndex={0}
        onClick={handleSettingClick}
        onKeyDown={handleSettingKeyDown}
        aria-label={`${gameName} 설정`}
      >
        ⚙
      </S.SettingsButton>
    </S.Container>
  );
};

export default GameActionButton;
