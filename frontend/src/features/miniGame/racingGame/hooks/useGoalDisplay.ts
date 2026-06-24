import { useEffect, useRef, useState } from 'react';

type Props = {
  myPosition: number;
  endDistance: number;
};

export const useGoalDisplay = ({ myPosition, endDistance }: Props) => {
  const [isGoal, setIsGoal] = useState(false);
  const hasShownGoalRef = useRef(false);

  useEffect(() => {
    const hasReachedGoal = myPosition >= endDistance;

    if (hasReachedGoal && !hasShownGoalRef.current) {
      hasShownGoalRef.current = true;
      setIsGoal(true);
    }
  }, [myPosition, endDistance]);

  return isGoal;
};
