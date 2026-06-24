import BackIcon from '@/assets/back-icon.svg';
import { useButtonInteraction } from '@/hooks/useButtonInteraction';
import type { ComponentProps } from 'react';

import * as S from './BackButton.styled';

type Props = {
  onClick: () => void;
  text?: string;
} & ComponentProps<'button'>;

const BackButton = ({ onClick, text, ...rest }: Props) => {
  const { touchState, pointerHandlers } = useButtonInteraction({ onClick });
  const hasText = Boolean(text);

  return (
    <S.Container {...pointerHandlers} $touchState={touchState} $hasText={hasText} {...rest}>
      <img src={BackIcon} alt="뒤로가기" />
      {hasText && <S.Text>{text}</S.Text>}
    </S.Container>
  );
};

export default BackButton;
