import { useEffect, useRef, useState } from 'react';

type Props = {
  myPosition: number;
  endDistance: number;
};

export const useGoalDisplay = ({ myPosition, endDistance }: Props) => {
  const [isGoal, setIsGoal] = useState(false);
  const hasShownGoalRef = useRef(false);

  useEffect(() => {
    // endDistance 0 은 결승선 거리를 아직 모른다는 뜻이다. 0 >= 0 으로 오발동하면 안 된다.
    const hasReachedGoal = endDistance > 0 && myPosition >= endDistance;

    if (hasReachedGoal && !hasShownGoalRef.current) {
      hasShownGoalRef.current = true;
      setIsGoal(true);
    }
  }, [myPosition, endDistance]);

  return isGoal;
};
