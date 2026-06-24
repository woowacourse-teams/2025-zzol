import { useRef, useEffect, useCallback } from 'react';
import * as S from './TabBar.styled';

type Props = {
  tabs: string[];
  activeTabIndex: number;
  onTabChange: (index: number) => void;
};

const TabBar = ({ tabs, activeTabIndex, onTabChange }: Props) => {
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);
  const indicatorRef = useRef<HTMLDivElement | null>(null);

  const setTabRef = useCallback(
    (index: number) => (el: HTMLButtonElement | null) => {
      tabRefs.current[index] = el;
    },
    []
  );

  useEffect(() => {
    const activeTabElement = tabRefs.current[activeTabIndex];
    const indicator = indicatorRef.current;

    if (activeTabElement && indicator) {
      const { offsetLeft, offsetWidth } = activeTabElement;
      indicator.style.transform = `translateX(${offsetLeft}px)`;
      indicator.style.width = `${offsetWidth}px`;
    }
  }, [activeTabIndex]);

  return (
    <S.Container>
      {tabs.map((tab, index) => (
        <S.Tab
          key={index}
          ref={setTabRef(index)}
          isActive={index === activeTabIndex}
          onClick={() => onTabChange(index)}
        >
          {tab}
        </S.Tab>
      ))}
      <S.Indicator ref={indicatorRef} />
    </S.Container>
  );
};

export default TabBar;
