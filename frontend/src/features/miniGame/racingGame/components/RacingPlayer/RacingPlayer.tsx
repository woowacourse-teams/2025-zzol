import PlayerIcon from '@/components/@composition/PlayerIcon/PlayerIcon';
import { ColorList } from '@/constants/color';
import { useRotationAnimation } from '../../hooks/useRotationAnimation';
import Description from '@/components/@common/Description/Description';
import type { RacingPlayer as RacingPlayerType } from '@/types/miniGame/racingGame';
import type { Ref } from 'react';
import * as S from './RacingPlayer.styled';

type Props = {
  player: RacingPlayerType;
  isMe: boolean;
  myPosition: number;
  color: ColorList;
  /** 탭할 때 튕기는 연출을 부모가 걸 수 있게 내 캐릭터만 넘겨받는다. */
  ref?: Ref<HTMLDivElement>;
};

const RacingPlayer = ({ player, isMe, myPosition, color, ref }: Props) => {
  const rotatingRef = useRotationAnimation({ speed: player.speed });

  return (
    <S.Container ref={ref} $isMe={isMe} $position={player.position} $myPosition={myPosition}>
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
