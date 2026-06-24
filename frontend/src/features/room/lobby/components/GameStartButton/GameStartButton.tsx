import Button from '@/components/@common/Button/Button';
import type { ComponentProps } from 'react';

type Props = {
  onClick?: () => void;
} & Omit<ComponentProps<typeof Button>, 'onClick'>;

const GameStartButton = ({ onClick, ...rest }: Props) => {
  return (
    <Button variant="primary" onClick={onClick} data-testid="game-start-button" {...rest}>
      게임 시작
    </Button>
  );
};

export default GameStartButton;
