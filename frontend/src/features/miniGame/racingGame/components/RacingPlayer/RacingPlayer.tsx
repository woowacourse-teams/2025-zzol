import PlayerIcon from '@/components/@composition/PlayerIcon/PlayerIcon';
import { ColorList } from '@/constants/color';
import { useRotationAnimation } from '../../hooks/useRotationAnimation';
import Description from '@/components/@common/Description/Description';
import type { RacingPlayer as RacingPlayerType } from '@/types/miniGame/racingGame';
import type { Ref } from 'react';
import { hashIndex } from '@/utils/hashIndex';
import SpeedGauge from '../SpeedGauge/SpeedGauge';
import * as S from './RacingPlayer.styled';

/** 색만으로는 적록색약 사용자가 서로를 못 가른다. 테두리 모양을 하나 더 얹는다. */
const BORDER_STYLES = ['solid', 'dashed', 'dotted', 'double'];

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

  // 참가자 색 인덱스가 아니라 이름에서 뽑는다. 명단이 아직 없어도 모양은 사람마다 다르다.
  const borderStyle = isMe
    ? BORDER_STYLES[0]
    : BORDER_STYLES[hashIndex(player.playerName, BORDER_STYLES.length)];

  return (
    <S.Container ref={ref} $isMe={isMe} $position={player.position} $myPosition={myPosition}>
      {isMe && <S.MyMarker />}
      <S.PlayerName>
        <Description color={isMe ? 'point-500' : 'white'}>{player.playerName}</Description>
      </S.PlayerName>

      <S.IconRing $borderStyle={borderStyle} $isMe={isMe}>
        <S.RotatingWrapper ref={rotatingRef}>
          <PlayerIcon color={color} />
        </S.RotatingWrapper>
      </S.IconRing>

      {isMe && <SpeedGauge speed={player.speed} />}
    </S.Container>
  );
};

export default RacingPlayer;
