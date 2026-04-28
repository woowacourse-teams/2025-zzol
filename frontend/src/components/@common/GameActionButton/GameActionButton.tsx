import { ComponentProps, ReactElement } from 'react';
import Headline4 from '../Headline4/Headline4';
import * as S from './GameActionButton.styled';

type Props = {
  onClick: () => void;
  isSelected: boolean;
  isDisabled: boolean;
  gameName: string;
  description: string[];
  icon: ReactElement;
  orderNumber?: number;
} & Omit<ComponentProps<'button'>, 'onClick'>;

const GameActionButton = ({
  onClick,
  isSelected,
  isDisabled,
  gameName,
  description,
  icon,
  orderNumber,
  ...rest
}: Props) => {
  const handleClick = () => {
    if (isDisabled) return;
    onClick();
  };

  return (
    <S.Container onClick={handleClick} $isSelected={isSelected} $disabled={isDisabled} {...rest}>
      <S.Wrapper>
        <Headline4 color={isSelected ? 'white' : 'point-400'}>{gameName}</Headline4>
        <S.DescriptionWrapper $isSelected={isSelected}>
          {description &&
            description.map((desc, index) => <S.Description key={index}>{desc}</S.Description>)}
        </S.DescriptionWrapper>
      </S.Wrapper>
      <S.GameIcon $isSelected={isSelected}>{icon}</S.GameIcon>
      {isSelected && orderNumber && <S.NumberBadge>{orderNumber}</S.NumberBadge>}
    </S.Container>
  );
};

export default GameActionButton;
