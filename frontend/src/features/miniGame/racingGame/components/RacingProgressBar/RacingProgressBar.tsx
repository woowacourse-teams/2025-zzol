import * as S from './RacingProgressBar.styled';
import { usePlayersProgressData } from '../../hooks/usePlayersProgressData';
import { colorList } from '@/constants/color';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { RacingPlayer } from '@/types/miniGame/racingGame';

type Props = {
  myName: string;
  endDistance: number;
  players: RacingPlayer[];
};

const RacingProgressBar = ({ myName, endDistance, players }: Props) => {
  const playersProgressData = usePlayersProgressData({ players, endDistance, myName });
  const { getParticipantColorIndex } = useParticipants();

  return (
    <S.Container>
      <S.ProgressTrack>
        {playersProgressData.map(({ player, progress, isMe }) => [
          <S.ProgressFill
            key={`fill-${player.playerName}`}
            $progress={progress}
            $color={colorList[getParticipantColorIndex(player.playerName)]}
            $isMe={isMe}
          />,
          <S.ProgressMarker
            key={`marker-${player.playerName}`}
            $progress={progress}
            $color={colorList[getParticipantColorIndex(player.playerName)]}
            $isMe={isMe}
          />,
        ])}
      </S.ProgressTrack>
    </S.Container>
  );
};

export default RacingProgressBar;
