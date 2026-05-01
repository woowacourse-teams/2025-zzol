import usePatchNotes, { type PatchNoteChangeType } from '@/features/home/hooks/usePatchNotes';
import * as S from './PatchNotesView.styled';

const TYPE_LABEL: Record<PatchNoteChangeType, string> = {
  new: 'NEW',
  fix: 'FIX',
  improve: 'UPD',
};

// TODO: API 연동 완료 후 제거
const IS_IN_DEVELOPMENT = true;

const PatchNotesView = () => {
  const { data: patchNotes, loading } = usePatchNotes();

  if (IS_IN_DEVELOPMENT) {
    return (
      <S.EmptyContainer>
        <S.EmptyIconWrap>📝</S.EmptyIconWrap>
        <S.EmptyTitle>패치 노트 준비 중</S.EmptyTitle>
        <S.EmptyDesc>{'새로운 업데이트가 있으면\n이곳에서 가장 먼저 알려드릴게요'}</S.EmptyDesc>
        <S.VersionPill>현재 버전 v1.0.0</S.VersionPill>
      </S.EmptyContainer>
    );
  }

  return (
    <S.Container>
      {loading && <S.LoadingText>불러오는 중...</S.LoadingText>}
      {patchNotes.map(({ id, version, date, changes }) => (
        <S.TimelineEntry key={id}>
          <S.TimelineLeft>
            <S.VersionDot />
            <S.TimelineLine />
          </S.TimelineLeft>
          <S.TimelineRight>
            <S.TimelineHeader>
              <S.VersionTag>{version}</S.VersionTag>
              <S.VersionDate>{date}</S.VersionDate>
            </S.TimelineHeader>
            <S.ChangeList>
              {changes.map(({ type, text }, j) => (
                <S.ChangeItem key={j}>
                  <S.ChangeType $type={type}>{TYPE_LABEL[type]}</S.ChangeType>
                  <S.ChangeText>{text}</S.ChangeText>
                </S.ChangeItem>
              ))}
            </S.ChangeList>
          </S.TimelineRight>
        </S.TimelineEntry>
      ))}
    </S.Container>
  );
};

export default PatchNotesView;
