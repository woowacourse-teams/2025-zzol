import Divider from '@/components/@common/Divider/Divider';
import Headline4 from '@/components/@common/Headline4/Headline4';
import PlayerCard from '@/components/@composition/PlayerCard/PlayerCard';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { PlayerProbability } from '@/types/roulette';
import * as S from './ProbabilityList.styled';

type Props = {
  playerProbabilities: PlayerProbability[];
};

const ProbabilityList = ({ playerProbabilities }: Props) => {
  const { myName } = useIdentifier();
  const myProbability = playerProbabilities.find(({ playerName }) => playerName === myName);
  const myColor = myProbability ? myProbability.playerColor : '#FF6B6B';

  const filteredParticipants = playerProbabilities.filter(
    ({ playerName }) => playerName !== myName
  );

  return (
    <>
      <PlayerCard name={myProbability ? myProbability.playerName : myName} playerColor={myColor}>
        <Headline4>{myProbability ? `${myProbability.probability}` : '100'}%</Headline4>
      </PlayerCard>
      <Divider />
      <S.ScrollableWrapper>
        {filteredParticipants.length === 0 ? (
          <S.Empty>현재 참여한 인원이 없습니다</S.Empty>
        ) : (
          filteredParticipants.map(({ playerName, probability, playerColor }) => (
            <PlayerCard key={playerName} name={playerName} playerColor={playerColor}>
              <Headline4>{probability}%</Headline4>
            </PlayerCard>
          ))
        )}
      </S.ScrollableWrapper>
      <S.BottomGap />
    </>
  );
};

export default ProbabilityList;
