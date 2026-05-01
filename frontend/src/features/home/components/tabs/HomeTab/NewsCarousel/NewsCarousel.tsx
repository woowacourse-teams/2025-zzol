import { useRef, useState } from 'react';
import useModal from '@/components/@common/Modal/useModal';
import usePatchNotes, {
  type PatchNote,
  type PatchNoteChangeType,
} from '@/features/home/hooks/usePatchNotes';
import * as S from './NewsCarousel.styled';

const TAG_LABEL: Record<PatchNoteChangeType, string> = {
  new: 'NEW',
  fix: 'FIX',
  improve: 'UPD',
};

type NewsDetailProps = { item: PatchNote };

const NewsDetail = ({ item }: NewsDetailProps) => (
  <S.Detail>
    <S.DetailMeta>
      <S.Tag>{TAG_LABEL[item.changes[0]?.type ?? 'new']}</S.Tag>
      <S.DetailDate>{item.date}</S.DetailDate>
    </S.DetailMeta>
    <S.DetailBody>{item.summary}</S.DetailBody>
  </S.Detail>
);

const NewsCarousel = () => {
  const { data: items, loading } = usePatchNotes();
  const [activeIndex, setActiveIndex] = useState(0);
  const trackRef = useRef<HTMLDivElement>(null);
  const { openModal } = useModal();

  const handleScroll = () => {
    const track = trackRef.current;
    if (!track) return;
    setActiveIndex(Math.round(track.scrollLeft / track.offsetWidth));
  };

  const handleCardClick = (item: PatchNote) => {
    openModal(<NewsDetail item={item} />, {
      title: item.title,
      showCloseButton: true,
      closeOnBackdropClick: true,
    });
  };

  if (loading) return <S.SkeletonCard />;

  if (!items.length) return null;

  return (
    <S.Wrapper>
      <S.Track ref={trackRef} onScroll={handleScroll}>
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
            <S.Tag>{TAG_LABEL[item.changes[0]?.type ?? 'new']}</S.Tag>
            <S.CardTop>
              <S.Title>{item.title}</S.Title>
              <S.Date>{item.date}</S.Date>
            </S.CardTop>
            <S.Body>{item.summary}</S.Body>
            <S.ReadMore>자세히 보기 →</S.ReadMore>
          </S.Card>
        ))}
      </S.Track>
      {items.length > 1 && (
        <S.Dots>
          {items.map((item, i) => (
            <S.Dot key={item.id} $active={i === activeIndex} />
          ))}
        </S.Dots>
      )}
    </S.Wrapper>
  );
};

export default NewsCarousel;
