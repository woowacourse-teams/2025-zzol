import { useState } from 'react';
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
  const [openIndex, setOpenIndex] = useState<number | null>(null);
  const { data: patchNotes, loading } = usePatchNotes();

  if (IS_IN_DEVELOPMENT) {
    return (
      <S.Container>
        <S.PageHeader>
          <S.PageTitle>패치 내역</S.PageTitle>
          <S.PageSub>업데이트 및 변경 사항</S.PageSub>
        </S.PageHeader>
        <S.PlaceholderCard>
          <S.PlaceholderIconWrap>🛠️</S.PlaceholderIconWrap>
          <S.PlaceholderTitle>패치 내역을 준비 중입니다</S.PlaceholderTitle>
          <S.PlaceholderSub>업데이트 후 이곳에서 확인할 수 있어요</S.PlaceholderSub>
        </S.PlaceholderCard>
      </S.Container>
    );
  }

  const toggle = (i: number) => setOpenIndex(openIndex === i ? null : i);

  return (
    <S.Container>
      <S.PageHeader>
        <S.PageTitle>패치 내역</S.PageTitle>
        <S.PageSub>업데이트 및 변경 사항</S.PageSub>
      </S.PageHeader>
      {loading && <S.PlaceholderSub>불러오는 중...</S.PlaceholderSub>}
      {patchNotes.map(({ id, version, date, changes }, i) => (
        <S.AccordionCard key={id}>
          <S.AccordionHeader $open={openIndex === i} onClick={() => toggle(i)}>
            <S.AccordionHeaderLeft>
              <S.VersionBadge>{version}</S.VersionBadge>
              <S.VersionDate>{date}</S.VersionDate>
            </S.AccordionHeaderLeft>
            <S.ChevronIcon $open={openIndex === i}>›</S.ChevronIcon>
          </S.AccordionHeader>
          {openIndex === i && (
            <S.AccordionBody>
              {changes.map(({ type, text }, j) => (
                <S.ChangeItem key={j}>
                  <S.ChangeType $type={type}>{TYPE_LABEL[type]}</S.ChangeType>
                  <S.ChangeText>{text}</S.ChangeText>
                </S.ChangeItem>
              ))}
            </S.AccordionBody>
          )}
        </S.AccordionCard>
      ))}
    </S.Container>
  );
};

export default PatchNotesView;
