import PlayerIcon from '@/components/@composition/PlayerIcon/PlayerIcon';
import { ColorList } from '@/constants/color';
import { useRotationAnimation } from '../../hooks/useRotationAnimation';
import Description from '@/components/@common/Description/Description';
import type { RacingPlayer as RacingPlayerType } from '@/types/miniGame/racingGame';
import * as S from './RacingPlayer.styled';

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
      <S.PlayerName>
        <Description color={isMe ? 'point-500' : 'white'}>{player.playerName}</Description>
      </S.PlayerName>

      <S.RotatingWrapper ref={rotatingRef}>
        <PlayerIcon color={color} />
      </S.RotatingWrapper>
    </S.Container>
  );
};

export default RacingPlayer;
