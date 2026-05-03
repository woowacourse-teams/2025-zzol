import useModal from '@/components/@common/Modal/useModal';
import {
  formatPatchNoteDate,
  usePatchNoteList,
  type PatchNote,
  type PatchNoteCategory,
} from '@/features/home/hooks/usePatchNotes';
import * as S from './PatchNotesView.styled';

const CATEGORY_ICON: Record<PatchNoteCategory, string> = {
  UPDATE: '✨',
  NOTICE: '📢',
  EVENT: '🎉',
  MAINTENANCE: '🔧',
};

const isNew = (createdAt: string | number): boolean => {
  const ms =
    typeof createdAt === 'number'
      ? createdAt < 1e12
        ? createdAt * 1000
        : createdAt
      : new Date(createdAt).getTime();
  return Date.now() - ms < 3 * 24 * 60 * 60 * 1000;
};

type NoteDetailProps = { note: PatchNote };

const NoteDetail = ({ note }: NoteDetailProps) => (
  <S.DetailContainer>
    <S.DetailMeta>
      <S.CategoryChip $category={note.category}>
        {CATEGORY_ICON[note.category]} {note.categoryLabel}
      </S.CategoryChip>
      <S.MetaDate>{formatPatchNoteDate(note.createdAt)}</S.MetaDate>
    </S.DetailMeta>
    <S.DetailBody>{note.content}</S.DetailBody>
  </S.DetailContainer>
);

type CardProps = { note: PatchNote };

const NoteCard = ({ note }: CardProps) => {
  const { openModal } = useModal();

  const handleClick = () => {
    openModal(<NoteDetail note={note} />, {
      title: note.title,
      showCloseButton: true,
      closeOnBackdropClick: true,
    });
  };

  return (
    <S.NoteCard
      $category={note.category}
      onClick={handleClick}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          e.preventDefault();
          handleClick();
        }
      }}
    >
      <S.CardInner>
        <S.CardMeta>
          <S.CategoryChip $category={note.category}>
            {CATEGORY_ICON[note.category]} {note.categoryLabel}
          </S.CategoryChip>
          {isNew(note.createdAt) && <S.NewBadge>NEW</S.NewBadge>}
          <S.MetaDate>{formatPatchNoteDate(note.createdAt)}</S.MetaDate>
        </S.CardMeta>
        <S.CardTitle>{note.title}</S.CardTitle>
        <S.CardPreview>{note.content}</S.CardPreview>
        <S.CardFooter>
          <S.CardReadMore>전체 보기 →</S.CardReadMore>
        </S.CardFooter>
      </S.CardInner>
    </S.NoteCard>
  );
};

const PatchNotesView = () => {
  const { data: patchNotes, loading } = usePatchNoteList();

  if (loading) {
    return (
      <S.Container>
        {Array.from({ length: 3 }).map((_, i) => (
          <S.SkeletonCard key={i}>
            <S.SkeletonInner>
              <S.SkeletonLine $width="80px" $height="20px" />
              <S.SkeletonLine $width="70%" $height="22px" />
              <S.SkeletonLine />
              <S.SkeletonLine $width="85%" />
            </S.SkeletonInner>
          </S.SkeletonCard>
        ))}
      </S.Container>
    );
  }

  if (!patchNotes.length) {
    return (
      <S.EmptyContainer>
        <S.EmptyIconWrap>📝</S.EmptyIconWrap>
        <S.EmptyTitle>패치 노트 준비 중</S.EmptyTitle>
        <S.EmptyDesc>{'새로운 업데이트가 있으면\n이곳에서 가장 먼저 알려드릴게요'}</S.EmptyDesc>
      </S.EmptyContainer>
    );
  }

  return (
    <S.Container>
      <S.ListSummary>
        <S.SummaryText>총 {patchNotes.length}개의 소식</S.SummaryText>
      </S.ListSummary>
      {patchNotes.map((note) => (
        <NoteCard key={note.id} note={note} />
      ))}
    </S.Container>
  );
};

export default PatchNotesView;
