import { useProbabilityHistory } from '@/contexts/ProbabilityHistory/ProbabilityHistoryContext';
import { useRouletteTransition } from '@/features/roulette/hooks/useRouletteTransition';
import RouletteWheel from '@/features/roulette/components/RouletteWheel/RouletteWheel';
import { useEffect } from 'react';

type Props = {
  finalRotation: number;
  isSpinStarted: boolean;
  startAnimation: boolean;
};

const ANIMATION_DURATION = 500;

const AnimatedRouletteWheel = ({ finalRotation, isSpinStarted, startAnimation }: Props) => {
  const { probabilityHistory } = useProbabilityHistory();

  const {
    animatedSectors,
    startAnimation: startAnimationTransition,
    setToPrev,
  } = useRouletteTransition(probabilityHistory.prev, probabilityHistory.current);

  useEffect(() => {
    if (startAnimation) {
      setToPrev();
      setTimeout(() => {
        startAnimationTransition();
      }, ANIMATION_DURATION);
    }
  }, [startAnimation, startAnimationTransition, setToPrev]);

  if (!animatedSectors) return null;

  return (
    <RouletteWheel
      sectors={animatedSectors}
      finalRotation={finalRotation}
      isSpinStarted={isSpinStarted}
    />
  );
};

export default AnimatedRouletteWheel;
