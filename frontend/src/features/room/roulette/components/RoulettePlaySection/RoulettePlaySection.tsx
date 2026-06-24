import * as S from './RoulettePlaySection.styled';
import { useProbabilityHistory } from '@/contexts/ProbabilityHistory/ProbabilityHistoryContext';
import { convertProbabilitiesToAngles } from '@/features/roulette/utils/convertProbabilitiesToAngles';
import { calculateFinalRotation } from '../../utils/calculateFinalRotation';
import AnimatedRouletteWheel from '../AnimatedRouletteWheel/AnimatedRouletteWheel';
import ProbabilitiesText from '../ProbabilitiesText/ProbabilitiesText';
import RouletteWheelBack from '@/features/roulette/components/RouletteWheelBack/RouletteWheelBack';
import Flip from '@/components/@common/Flip/Flip';
import { RefObject, useEffect, useState } from 'react';
import useRouletteProbabilities from '../../pages/RoulettePlayPage/hooks/useRouletteProbabilities';

type Props = {
  isSpinStarted: boolean;
  winner: string | null;
  randomAngle: number;
  isFirstLoadRef: RefObject<boolean>;
};

const RoulettePlaySection = ({ isSpinStarted, winner, randomAngle, isFirstLoadRef }: Props) => {
  const { probabilityHistory } = useProbabilityHistory();
  const [isFlipped, setIsFlipped] = useState(false);
  const { isLoading } = useRouletteProbabilities(isFirstLoadRef);

  const shouldComputeFinalRotation = isSpinStarted && winner;
  const finalRotation = shouldComputeFinalRotation
    ? calculateFinalRotation({
        finalAngles: convertProbabilitiesToAngles(probabilityHistory.current),
        winner,
        randomAngle,
      })
    : 0;

  useEffect(() => {
    if (!isLoading) {
      requestAnimationFrame(() => {
        setIsFlipped(true);
      });
    }
  }, [isLoading]);

  return (
    <S.Container>
      <S.RouletteWheelWrapper>
        <Flip
          flipped={isFlipped}
          initialView={<RouletteWheelBack />}
          flippedView={
            <AnimatedRouletteWheel
              finalRotation={finalRotation}
              isSpinStarted={isSpinStarted}
              startAnimation={isFlipped}
            />
          }
        />
      </S.RouletteWheelWrapper>
      <ProbabilitiesText isProbabilitiesLoading={isLoading} />
    </S.Container>
  );
};

export default RoulettePlaySection;
