import { ReactNode } from 'react';
import FadeInItem from '../../@common/FadeInItem/FadeInItem';

type FadeInUpListProps<T> = {
  items: T[];
  renderItem: (item: T, index: number) => ReactNode;
  staggerDelay?: number;
  animationDuration?: number;
};

const FadeInUpList = <T,>({
  items,
  renderItem,
  staggerDelay = 200,
  animationDuration = 600,
}: FadeInUpListProps<T>) => {
  return (
    <>
      {items.map((item, index) => (
        <FadeInItem key={index} index={index} delay={staggerDelay} duration={animationDuration}>
          {renderItem(item, index)}
        </FadeInItem>
      ))}
    </>
  );
};

export default FadeInUpList;
