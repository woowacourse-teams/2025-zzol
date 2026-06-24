import { type ComponentProps } from 'react';

import { useButtonInteraction } from '@/hooks/useButtonInteraction';

import * as S from './TextButton.styled';

type Props = {
  text: string;
  onClick: () => void;
} & Omit<ComponentProps<'button'>, 'onClick'>;

const TextButton = ({ text, onClick, ...rest }: Props) => {
  const { touchState, pointerHandlers } = useButtonInteraction({ onClick });

  return (
    <S.Container {...pointerHandlers} $touchState={touchState} {...rest}>
      {text}
    </S.Container>
  );
};

export default TextButton;
