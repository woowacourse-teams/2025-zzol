import { useState, useEffect } from 'react';
import { ANIMATION_DURATION } from '@/constants/animation';

type AnimationState = 'fadingIn' | 'fadingOut';

type UseAutoSlideCarouselOptions = {
  slideCount: number;
  displayDuration?: number;
  fadeDuration?: number;
};

export const useAutoSlideCarousel = ({
  slideCount,
  displayDuration = ANIMATION_DURATION.DASHBOARD_DISPLAY,
  fadeDuration = ANIMATION_DURATION.DASHBOARD_FADE,
}: UseAutoSlideCarouselOptions) => {
  const [currentSlideIndex, setCurrentSlideIndex] = useState(0);
  const [animationState, setAnimationState] = useState<AnimationState>('fadingIn');

  useEffect(() => {
    const slideTransitionCycle = setInterval(() => {
      setAnimationState('fadingOut');

      setTimeout(() => {
        setCurrentSlideIndex((prevIndex) => (prevIndex + 1) % slideCount);
        setAnimationState('fadingIn');
      }, fadeDuration);
    }, displayDuration);

    return () => {
      clearInterval(slideTransitionCycle);
    };
  }, [slideCount, displayDuration, fadeDuration]);

  return { currentSlideIndex, animationState };
};
