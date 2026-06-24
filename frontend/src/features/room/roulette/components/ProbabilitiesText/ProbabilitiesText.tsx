import Headline4 from '@/components/@common/Headline4/Headline4';
import * as S from './ProbabilitiesText.styled';
import { useProbabilityHistory } from '@/contexts/ProbabilityHistory/ProbabilityHistoryContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { ProbabilityHistory } from '@/types/roulette';

const formatPercent = new Intl.NumberFormat('ko-KR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

type Props = {
  isProbabilitiesLoading: boolean;
};

const ProbabilitiesText = ({ isProbabilitiesLoading }: Props) => {
  const { probabilityHistory } = useProbabilityHistory();
  const { myName } = useIdentifier();

  const myCurrentProbability = getMyCurrentProbability(probabilityHistory, myName);

  const myProbabilityChange = getMyProbabilityChange(probabilityHistory, myName);

  return (
    <S.ProbabilityText $isProbabilitiesLoading={isProbabilitiesLoading}>
      <Headline4>
        현재 확률 : {myCurrentProbability + '%'} {'('}
        <S.ProbabilityChange $isPositive={myProbabilityChange >= 0}>
          {(myProbabilityChange >= 0 ? '+' : '') + formatPercent.format(myProbabilityChange) + '%'}
        </S.ProbabilityChange>
        {')'}
      </Headline4>
    </S.ProbabilityText>
  );
};

export default ProbabilitiesText;

const getMyCurrentProbability = (probabilityHistory: ProbabilityHistory, myName: string) => {
  return (
    probabilityHistory.current.find((player) => player.playerName === myName)?.probability ?? 0
  );
};

const getMyPrevProbability = (probabilityHistory: ProbabilityHistory, myName: string) => {
  return probabilityHistory.prev.find((player) => player.playerName === myName)?.probability ?? 0;
};

const getMyProbabilityChange = (probabilityHistory: ProbabilityHistory, myName: string) => {
  return (
    getMyCurrentProbability(probabilityHistory, myName) -
    getMyPrevProbability(probabilityHistory, myName)
  );
};
