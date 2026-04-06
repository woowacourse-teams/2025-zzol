import useLazyFetch from '@/apis/rest/useLazyFetch';
import RankingItem from '@/components/@common/RankingItem/RankingItem';
import { useState } from 'react';
import type { RankingCategory } from '../../config/rankingConfigs';
import * as S from './RankingTab.styled';

type Props = {
  category: RankingCategory;
};

const RankingAccordionItem = ({ category }: Props) => {
  const [isOpen, setIsOpen] = useState(false);
  const [items, setItems] = useState<ReturnType<RankingCategory['transformData']>>([]);

  const { execute, loading } = useLazyFetch<unknown>({
    endpoint: category.endpoint,
    errorDisplayMode: 'toast',
    onSuccess: (data) => {
      setItems(category.transformData(data));
    },
  });

  const handleToggle = async () => {
    if (!isOpen && items.length === 0) {
      await execute();
    }
    setIsOpen((prev) => !prev);
  };

  return (
    <S.AccordionItem>
      <S.AccordionHeader onClick={handleToggle} aria-expanded={isOpen}>
        <S.AccordionTitle>
          <span>{category.icon}</span>
          <span>{category.label}</span>
        </S.AccordionTitle>
        <S.ChevronIcon $isOpen={isOpen}>▾</S.ChevronIcon>
      </S.AccordionHeader>
      <S.AccordionBody $isOpen={isOpen}>
        <S.AccordionContent>
          {loading && <S.LoadingText>불러오는 중...</S.LoadingText>}
          {!loading && items.length === 0 && isOpen && (
            <S.EmptyText>데이터가 없습니다.</S.EmptyText>
          )}
          {items.map((item) => (
            <RankingItem
              key={item.rank}
              rank={item.rank}
              name={item.name}
              count={item.count}
              unit={item.unit}
            />
          ))}
        </S.AccordionContent>
      </S.AccordionBody>
    </S.AccordionItem>
  );
};

export default RankingAccordionItem;
