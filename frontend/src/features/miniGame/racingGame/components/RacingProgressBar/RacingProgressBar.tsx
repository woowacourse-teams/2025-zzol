import * as S from './RacingProgressBar.styled';
import { usePlayersProgressData } from '../../hooks/usePlayersProgressData';
import { colorList } from '@/constants/color';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { RacingPlayer } from '@/types/miniGame/racingGame';
import { isLightColor } from '../../utils/isLightColor';

const MAX_PROGRESS = 100;

type Props = {
  myName: string;
  endDistance: number;
  players: RacingPlayer[];
};

const RacingProgressBar = ({ myName, endDistance, players }: Props) => {
  const playersProgressData = usePlayersProgressData({ players, endDistance, myName });
  const { getParticipantColorIndex } = useParticipants();

  const myProgress = playersProgressData.find(({ isMe }) => isMe)?.progress ?? 0;
  const myColor = colorList[getParticipantColorIndex(myName)];

  return (
    <S.Container>
      <S.Legend>
        <S.LegendLabel>출발 0</S.LegendLabel>
        <S.LegendLabel>결승 {endDistance > 0 ? endDistance : '—'}</S.LegendLabel>
      </S.Legend>
      <S.ProgressTrack>
        {/* 채움 막대는 내 것만 그린다. 전원 것을 그리면 같은 자리에 겹쳐 선두 것만 보였다. */}
        <S.FillClip>
          <S.ProgressFill
            $color={myColor}
            style={{ transform: `scaleX(${myProgress / MAX_PROGRESS})` }}
          />
        </S.FillClip>
        {playersProgressData.map(({ player, progress, isMe }) => {
          const color = colorList[getParticipantColorIndex(player.playerName)];

          return (
            <S.MarkerAnchor
              key={player.playerName}
              style={{ transform: `translateX(${progress}%)` }}
            >
              <S.ProgressMarker $color={color} $isMe={isMe}>
                <S.MarkerInitial $onLightColor={isLightColor(color)}>
                  {[...player.playerName][0]}
                </S.MarkerInitial>
              </S.ProgressMarker>
            </S.MarkerAnchor>
          );
        })}
      </S.ProgressTrack>
    </S.Container>
  );
};

export default RacingProgressBar;
