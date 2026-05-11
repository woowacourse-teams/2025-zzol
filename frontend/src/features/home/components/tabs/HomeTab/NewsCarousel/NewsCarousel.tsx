import { useEffect, useRef, useState } from 'react';
import useModal from '@/components/@common/Modal/useModal';
import {
  formatPatchNoteDate,
  usePatchNoteList,
  type PatchNote,
} from '@/features/home/hooks/usePatchNotes';
import * as S from './NewsCarousel.styled';

const MAX_VISIBLE = 5;

type Props = { onMoreClick: () => void };

type NewsDetailProps = { item: PatchNote };

const NewsDetail = ({ item }: NewsDetailProps) => (
  <S.Detail>
    <S.DetailMeta>
      <S.Tag>{item.categoryLabel}</S.Tag>
      <S.DetailDate>{formatPatchNoteDate(item.createdAt)}</S.DetailDate>
    </S.DetailMeta>
    <S.DetailBody>{item.content}</S.DetailBody>
  </S.Detail>
);

const NewsCarousel = ({ onMoreClick }: Props) => {
  const { data: allItems, loading } = usePatchNoteList();
  const items = allItems.slice(0, MAX_VISIBLE);

  const [index, setIndex] = useState(0);
  const indexRef = useRef(0);
  const maxIndexRef = useRef(0);
  const containerRef = useRef<HTMLDivElement>(null);
  const { openModal } = useModal();

  useEffect(() => {
    indexRef.current = index;
  }, [index]);

  useEffect(() => {
    // +1 for the "전체보기" slide at the end
    maxIndexRef.current = items.length;
  }, [items.length]);

  useEffect(() => {
    const el = containerRef.current;
    if (!el) return;

    let startX = 0;
    let startY = 0;
    let isHorizontal = false;
    let isMouseDown = false;
    let wasDragged = false;

    const navigate = (delta: number) => {
      if (Math.abs(delta) <= 40) return;
      const next = Math.max(
        0,
        Math.min(indexRef.current + (delta > 0 ? 1 : -1), maxIndexRef.current)
      );
      setIndex(next);
      indexRef.current = next;
    };

    // Touch
    const onTouchStart = (e: TouchEvent) => {
      startX = e.touches[0].clientX;
      startY = e.touches[0].clientY;
      isHorizontal = false;
    };
    const onTouchMove = (e: TouchEvent) => {
      const dx = Math.abs(e.touches[0].clientX - startX);
      const dy = Math.abs(e.touches[0].clientY - startY);
      if (dx > dy && dx > 5) {
        isHorizontal = true;
        e.preventDefault();
      }
    };
    const onTouchEnd = (e: TouchEvent) => {
      if (!isHorizontal) return;
      wasDragged = true;
      navigate(startX - e.changedTouches[0].clientX);
      setTimeout(() => {
        wasDragged = false;
      }, 0);
    };

    // Mouse (PC)
    const onMouseDown = (e: MouseEvent) => {
      startX = e.clientX;
      isMouseDown = true;
      wasDragged = false;
      el.style.cursor = 'grabbing';
    };
    const onMouseMove = (e: MouseEvent) => {
      if (!isMouseDown) return;
      e.preventDefault();
    };
    const onMouseUp = (e: MouseEvent) => {
      if (!isMouseDown) return;
      isMouseDown = false;
      el.style.cursor = '';
      const delta = startX - e.clientX;
      if (Math.abs(delta) > 40) {
        wasDragged = true;
        navigate(delta);
        setTimeout(() => {
          wasDragged = false;
        }, 0);
      }
    };
    const onMouseLeave = () => {
      isMouseDown = false;
      el.style.cursor = '';
    };

    // capture phase에서 드래그 후 발생하는 click 차단
    const preventClickOnDrag = (e: MouseEvent) => {
      if (wasDragged) e.stopPropagation();
    };

    el.addEventListener('touchstart', onTouchStart, { passive: true });
    el.addEventListener('touchmove', onTouchMove, { passive: false });
    el.addEventListener('touchend', onTouchEnd, { passive: true });
    el.addEventListener('mousedown', onMouseDown);
    el.addEventListener('mousemove', onMouseMove);
    el.addEventListener('mouseup', onMouseUp);
    el.addEventListener('mouseleave', onMouseLeave);
    el.addEventListener('click', preventClickOnDrag, true);

    return () => {
      el.removeEventListener('touchstart', onTouchStart);
      el.removeEventListener('touchmove', onTouchMove);
      el.removeEventListener('touchend', onTouchEnd);
      el.removeEventListener('mousedown', onMouseDown);
      el.removeEventListener('mousemove', onMouseMove);
      el.removeEventListener('mouseup', onMouseUp);
      el.removeEventListener('mouseleave', onMouseLeave);
      el.removeEventListener('click', preventClickOnDrag, true);
    };
  }, []);

  const handleCardClick = (item: PatchNote) => {
    openModal(<NewsDetail item={item} />, {
      title: item.title,
      showCloseButton: true,
      closeOnBackdropClick: true,
    });
  };

  const totalSlides = items.length + 1;

  if (loading) return <S.SkeletonCard />;
  if (!items.length) return null;

  return (
    <S.Wrapper>
      <S.CarouselArea ref={containerRef}>
        <S.Track style={{ transform: `translateX(-${index * 100}%)` }}>
          {items.map((item) => (
            <S.Card
              key={item.id}
              onClick={() => handleCardClick(item)}
              role="button"
              tabIndex={0}
              onKeyDown={(e) => {
                if (e.key === 'Enter' || e.key === ' ') {
                  e.preventDefault();
                  handleCardClick(item);
                }
              }}
            >
              <S.Tag>{item.categoryLabel}</S.Tag>
              <S.CardTop>
                <S.Title>{item.title}</S.Title>
                <S.Date>{formatPatchNoteDate(item.createdAt)}</S.Date>
              </S.CardTop>
              <S.Body>{item.content}</S.Body>
              <S.ReadMore>자세히 보기 →</S.ReadMore>
            </S.Card>
          ))}
          <S.MoreCard
            onClick={onMoreClick}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                onMoreClick();
              }
            }}
          >
            <S.MoreTop>
              <S.MoreIconCircle>↗</S.MoreIconCircle>
              <S.MoreTitle>전체보기</S.MoreTitle>
              <S.MoreSub>총 {allItems.length}개의 소식</S.MoreSub>
            </S.MoreTop>
            <S.MoreArrow>›</S.MoreArrow>
          </S.MoreCard>
        </S.Track>
      </S.CarouselArea>
      <S.Controls>
        <S.NavChevron
          onClick={() => setIndex((i) => Math.max(0, i - 1))}
          disabled={index === 0}
          aria-label="이전"
        >
          ‹
        </S.NavChevron>
        <S.Dots>
          {Array.from({ length: totalSlides }).map((_, i) => (
            <S.Dot key={i} $active={i === index} />
          ))}
        </S.Dots>
        <S.NavChevron
          onClick={() => setIndex((i) => Math.min(i + 1, totalSlides - 1))}
          disabled={index === totalSlides - 1}
          aria-label="다음"
        >
          ›
        </S.NavChevron>
      </S.Controls>
    </S.Wrapper>
  );
};

export default NewsCarousel;
