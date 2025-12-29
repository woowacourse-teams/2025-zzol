import PlayerIcon from '@/components/@composition/PlayerIcon/PlayerIcon';
import { ColorList } from '@/constants/color';
import { useRotationAnimation } from '../../hooks/useRotationAnimation';
import Description from '@/components/@common/Description/Description';
import type { RacingPlayer as RacingPlayerType } from '@/types/miniGame/racingGame';
import * as S from './RacingPlayer.styled';
import { memo, RefObject } from 'react';

type Props = {
  player: RacingPlayerType;
  isMe: boolean;
  myPosition: number;
  color: ColorList;
};

const RacingPlayer = ({ player, isMe, myPosition, color }: Props) => {
  const rotatingRef = useRotationAnimation({ speed: player.speed });

  return (
    <S.Container $isMe={isMe} $position={player.position} $myPosition={myPosition}>
      <PlayerName playerName={player.playerName} isMe={isMe} />
      <RotatingWrapper rotatingRef={rotatingRef} color={color} />
    </S.Container>
  );
};

export default memo(RacingPlayer, (prevProps, nextProps) => {
  return prevProps.isMe && prevProps.player.speed === nextProps.player.speed;
});

type PlayerNameProps = {
  playerName: string;
  isMe: boolean;
};

const PlayerName = memo(({ playerName, isMe }: PlayerNameProps) => {
  return (
    <S.PlayerName>
      <Description color={isMe ? 'point-500' : 'white'}>{playerName}</Description>
    </S.PlayerName>
  );
});

PlayerName.displayName = 'PlayerName';

type RotatingWrapperProps = {
  rotatingRef: RefObject<HTMLDivElement | null>;
  color: ColorList;
};

const RotatingWrapper = memo(
  ({ rotatingRef, color }: RotatingWrapperProps) => {
    return (
      <S.RotatingWrapper ref={rotatingRef}>
        <PlayerIcon color={color} />
      </S.RotatingWrapper>
    );
  },
  (prevProps, nextProps) => {
    return prevProps.color === nextProps.color;
  }
);

RotatingWrapper.displayName = 'RotatingWrapper';
