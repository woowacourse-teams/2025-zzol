// TODO: 백엔드 명세 전달 후 아래 두 가지 교체
//   1. PatchNote 타입을 실제 응답 스키마에 맞게 수정
//   2. mock 데이터 제거 후 useFetch<PatchNote[]>({ endpoint: '/patch-notes' }) 로 교체

export type PatchNoteChangeType = 'new' | 'fix' | 'improve';

export type PatchNoteChange = {
  type: PatchNoteChangeType;
  text: string;
};

export type PatchNote = {
  id: string;
  version: string;
  date: string;
  title: string;
  summary: string;
  changes: PatchNoteChange[];
};

const MOCK_PATCH_NOTES: PatchNote[] = [
  {
    id: '3',
    version: 'v1.2.0',
    date: '2025.07',
    title: '홈 화면 UI 전면 개편',
    summary:
      '더 쾌적한 경험을 위해 홈 화면 레이아웃을 새롭게 개편했어요. 미니게임 소개와 서비스 소식을 한눈에 확인할 수 있어요.',
    changes: [
      { type: 'new', text: '하단 메뉴 탭 추가 — 건의사항, 패치 내역, 서비스 정보' },
      { type: 'improve', text: '홈 탭 UI 개편 및 레이아웃 일관성 개선' },
    ],
  },
  {
    id: '2',
    version: 'v1.1.0',
    date: '2025.06',
    title: '레이싱 게임 랭킹 시스템 출시',
    summary:
      '이제 레이싱 게임의 전국 랭킹을 확인할 수 있어요. 랭킹 탭에서 이번 달 상위 플레이어를 만나보세요.',
    changes: [
      { type: 'new', text: '스피드 터치 미니게임 추가' },
      { type: 'new', text: '폭탄 돌리기 미니게임 추가' },
      { type: 'fix', text: 'QR 스캔 후 방 입장 오류 수정' },
      { type: 'improve', text: '룰렛 애니메이션 성능 개선' },
    ],
  },
  {
    id: '1',
    version: 'v1.0.0',
    date: '2025.06',
    title: '쫄(ZZOL) 서비스 오픈',
    summary:
      '커피내기, 밥값 내기, 당번 정하기. 매번 반복되는 결정을 쫄깃한 미니게임으로 해결해보세요.',
    changes: [
      { type: 'new', text: '쫄(ZZOL) 서비스 런칭' },
      { type: 'new', text: '카드 게임, 레이싱 미니게임 지원' },
      { type: 'new', text: 'QR코드 기반 방 참가 기능' },
    ],
  },
];

const usePatchNotes = () => {
  // TODO: useFetch<PatchNote[]>({ endpoint: '/patch-notes' }) 로 교체
  return { data: MOCK_PATCH_NOTES, loading: false };
};

export default usePatchNotes;
