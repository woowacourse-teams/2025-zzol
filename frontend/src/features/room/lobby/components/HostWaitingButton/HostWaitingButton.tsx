import Button from '@/components/@common/Button/Button';
import type { ComponentProps } from 'react';

type Props = {
  currentReadyCount?: number;
  totalParticipantCount?: number;
} & Omit<ComponentProps<typeof Button>, 'onClick'>;

const HostWaitingButton = ({
  currentReadyCount = 0,
  totalParticipantCount = 0,
  ...rest
}: Props) => {
  return (
    <Button variant="ready" {...rest}>
      게임 대기중... {currentReadyCount}/{totalParticipantCount}
    </Button>
  );
};

export default HostWaitingButton;
