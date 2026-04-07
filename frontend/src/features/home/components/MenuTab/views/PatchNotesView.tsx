import { useState } from 'react';
import * as S from './PatchNotesView.styled';

type ChangeType = 'new' | 'fix' | 'improve';

type PatchNote = {
  version: string;
  date: string;
  changes: { type: ChangeType; text: string }[];
};

const TYPE_LABEL: Record<ChangeType, string> = {
  new: 'NEW',
  fix: 'FIX',
  improve: 'UPD',
};

// TODO: API 연동 시 /patch-notes 엔드포인트로 교체 (docs/tab-api-todo.md 참고)
const IS_IN_DEVELOPMENT =
  typeof process !== 'undefined' ? process.env.NODE_ENV !== 'production' : false;

const PATCH_NOTES: PatchNote[] = [
  {
    version: 'v1.2.0',
    date: '2025.07',
    changes: [
      { type: 'new', text: '하단 메뉴 탭 추가 — 건의사항, 패치 내역, 서비스 정보' },
      { type: 'improve', text: '홈 탭 UI 개편 및 레이아웃 일관성 개선' },
    ],
  },
  {
    version: 'v1.1.0',
    date: '2025.06',
    changes: [
      { type: 'new', text: '스피드 터치 미니게임 추가' },
      { type: 'new', text: '폭탄 돌리기 미니게임 추가' },
      { type: 'fix', text: 'QR 스캔 후 방 입장 오류 수정' },
      { type: 'improve', text: '룰렛 애니메이션 성능 개선' },
    ],
  },
  {
    version: 'v1.0.0',
    date: '2025.06',
    changes: [
      { type: 'new', text: '쫄(ZZOL) 서비스 런칭' },
      { type: 'new', text: '카드 게임, 레이싱 미니게임 지원' },
      { type: 'new', text: 'QR코드 기반 방 참가 기능' },
    ],
  },
];

const PatchNotesView = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  if (IS_IN_DEVELOPMENT) {
    return (
      <S.Container>
        <S.PlaceholderCard>
          <S.PlaceholderIcon>🛠️</S.PlaceholderIcon>
          <S.PlaceholderTitle>패치 내역을 준비 중입니다</S.PlaceholderTitle>
          <S.PlaceholderSub>업데이트 후 이곳에서 확인할 수 있어요</S.PlaceholderSub>
        </S.PlaceholderCard>
      </S.Container>
    );
  }

  const toggle = (i: number) => setOpenIndex(openIndex === i ? null : i);

  return (
    <S.Container>
      {PATCH_NOTES.map(({ version, date, changes }, i) => (
        <S.AccordionCard key={version}>
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
