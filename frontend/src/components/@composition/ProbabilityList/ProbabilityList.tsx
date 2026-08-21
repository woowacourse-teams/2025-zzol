import Divider from '@/components/@common/Divider/Divider';
import { colorList } from '@/constants/color';
import Headline4 from '@/components/@common/Headline4/Headline4';
import PlayerCard from '@/components/@composition/PlayerCard/PlayerCard';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { PlayerProbability } from '@/types/roulette';
import { formatProbability } from '@/utils/formatProbability';
import * as S from './ProbabilityList.styled';

type Props = {
  playerProbabilities: PlayerProbability[];
};

const ProbabilityList = ({ playerProbabilities }: Props) => {
  const { myName } = useIdentifier();
  const myProbability = playerProbabilities.find(({ playerName }) => playerName === myName);
  // 확률 정보가 아직 없을 때의 기본 색 — 참가자 색 팔레트의 첫 번째
  const myColor = myProbability ? myProbability.playerColor : colorList[0];

  const filteredParticipants = playerProbabilities.filter(
    ({ playerName }) => playerName !== myName
  );

  return (
    <>
      <S.MyRow>
        <PlayerCard name={myProbability ? myProbability.playerName : myName} playerColor={myColor}>
          <Headline4>
            <S.Percent>
              {formatProbability(myProbability ? myProbability.probability : 100)}
            </S.Percent>
          </Headline4>
        </PlayerCard>
      </S.MyRow>
      <Divider />
      <S.ScrollableWrapper>
        {filteredParticipants.length === 0 ? (
          <S.Empty>현재 참여한 인원이 없습니다</S.Empty>
        ) : (
          filteredParticipants.map(({ playerName, probability, playerColor }) => (
            <PlayerCard key={playerName} name={playerName} playerColor={playerColor}>
              <Headline4>
                <S.Percent>{formatProbability(probability)}</S.Percent>
              </Headline4>
            </PlayerCard>
          ))
        )}
      </S.ScrollableWrapper>
      <S.BottomGap />
    </>
  );
};

export default ProbabilityList;
