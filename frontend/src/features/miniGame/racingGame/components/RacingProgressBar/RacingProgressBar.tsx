import * as S from './RacingProgressBar.styled';
import { useRacingGameData } from '@/contexts/RacingGame/RacingGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { usePlayersProgressData } from '../../hooks/usePlayersProgressData';
import { colorList } from '@/constants/color';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';

const RacingProgressBar = () => {
  const racingGameData = useRacingGameData();
  const { myName } = useIdentifier();
  const { getParticipantColorIndex } = useParticipants();

  const playersProgressData = usePlayersProgressData({
    players: racingGameData.players,
    endDistance: racingGameData.distance.end,
    myName,
  });

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
