import useLazyFetch from '@/apis/rest/useLazyFetch';
import RankingItem from '@/components/@common/RankingItem/RankingItem';
import { useRef, useState } from 'react';
import type { RankingCategory } from '../../config/rankingConfigs';
import * as S from './RankingTab.styled';

type Props = {
  category: RankingCategory;
};

const RankingAccordionItem = ({ category }: Props) => {
  const [isOpen, setIsOpen] = useState(false);
  const [openKey, setOpenKey] = useState(0);
  const [items, setItems] = useState<ReturnType<RankingCategory['transformData']>>([]);
  const bodyRef = useRef<HTMLDivElement>(null);

  const handleTransitionEnd = () => {
    if (!isOpen || !bodyRef.current) return;
    const container = bodyRef.current.closest<HTMLElement>('[data-scroll-container]');
    if (!container) return;
    const { bottom: bodyBottom } = bodyRef.current.getBoundingClientRect();
    const { bottom: containerBottom } = container.getBoundingClientRect();
    if (bodyBottom > containerBottom) {
      container.scrollTop += bodyBottom - containerBottom + 16;
    }
  };

  const { execute, loading } = useLazyFetch<unknown>({
    endpoint: category.endpoint,
    errorDisplayMode: 'toast',
    onSuccess: (data) => {
      setItems(category.transformData(data));
    },
  });

  const handleToggle = async () => {
    if (isOpen) {
      setIsOpen(false);
      return;
    }

    if (items.length > 0) {
      setIsOpen(true);
      setOpenKey((k) => k + 1);
      return;
    }

    const result = await execute();

    if (result == null) {
      return;
    }

    setIsOpen(true);
    setOpenKey((k) => k + 1);
  };

  const Icon = category.icon;

  return (
    <S.AccordionItem>
      <S.AccordionHeader
        onClick={handleToggle}
        aria-expanded={isOpen}
        aria-controls={`accordion-body-${category.label}`}
      >
        <S.AccordionTitle>
          <Icon />
          <span>{category.label}</span>
        </S.AccordionTitle>
        <S.ChevronIcon $isOpen={isOpen} aria-hidden="true">
          ▾
        </S.ChevronIcon>
      </S.AccordionHeader>
      <S.AccordionBody
        ref={bodyRef}
        id={`accordion-body-${category.label}`}
        $isOpen={isOpen}
        onTransitionEnd={handleTransitionEnd}
      >
        <S.AccordionContent key={openKey}>
          {loading && <S.Spinner role="status" aria-label="로딩 중" />}
          {!loading && items.length === 0 && isOpen && (
            <S.EmptyText>데이터가 없습니다.</S.EmptyText>
          )}
          {items.map((item, index) => (
            <S.AnimatedItem key={item.rank} $index={index}>
              <RankingItem rank={item.rank} name={item.name} count={item.count} unit={item.unit} />
            </S.AnimatedItem>
          ))}
        </S.AccordionContent>
      </S.AccordionBody>
    </S.AccordionItem>
  );
};

export default RankingAccordionItem;
