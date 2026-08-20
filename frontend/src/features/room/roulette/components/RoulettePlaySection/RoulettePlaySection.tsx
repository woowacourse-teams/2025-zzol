import * as S from './RoulettePlaySection.styled';
import { useProbabilityHistory } from '@/contexts/ProbabilityHistory/ProbabilityHistoryContext';
import { convertProbabilitiesToAngles } from '@/features/roulette/utils/convertProbabilitiesToAngles';
import { calculateFinalRotation } from '../../utils/calculateFinalRotation';
import AnimatedRouletteWheel from '../AnimatedRouletteWheel/AnimatedRouletteWheel';
import NearbyProbabilityList from '../NearbyProbabilityList/NearbyProbabilityList';
import RouletteWheelBack from '@/features/roulette/components/RouletteWheelBack/RouletteWheelBack';
import Flip from '@/components/@common/Flip/Flip';
import ScreenReaderOnly from '@/components/@common/ScreenReaderOnly/ScreenReaderOnly';
import { PlayerProbability } from '@/types/roulette';
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
      <S.RouletteWheelArea>
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
      </S.RouletteWheelArea>
      {/*
        휠은 aria-hidden 이고 아래 리스트는 나와 가까운 사람만 보여준다.
        얇은 조각이라 이름이 그려지지 않은 참가자까지 낭독되도록 전원을 여기서 읽어준다.
      */}
      {!isLoading && (
        <ScreenReaderOnly>{describeProbabilities(probabilityHistory.current)}</ScreenReaderOnly>
      )}
      <NearbyProbabilityList isProbabilitiesLoading={isLoading} />
    </S.Container>
  );
};

const describeProbabilities = (players: PlayerProbability[]) =>
  players.length === 0
    ? '현재 참여한 인원이 없습니다.'
    : players
        .map(({ playerName, probability }) => `${playerName}님의 확률 ${probability}%`)
        .join(', ');

export default RoulettePlaySection;
