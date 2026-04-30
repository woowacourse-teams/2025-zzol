import { useState } from 'react';
import type { ComponentType } from 'react';
import BackButton from '@/components/@common/BackButton/BackButton';
import SuggestionTab from '../SuggestionTab/SuggestionTab';
import PatchNotesView from './views/PatchNotesView';
import ServiceInfoView from './views/ServiceInfoView';
import MyInfoView from './views/MyInfoView';
import GameManualView from './views/GameManualView';
import * as S from './MenuTab.styled';
import { PersonIcon, BubbleIcon, ClipboardIcon, InfoIcon, GameIcon } from './menuIcons';

type MenuView = 'my-info' | 'game-manual' | 'report' | 'patch-notes' | 'service-info';

const MENU_ITEMS: {
  key: MenuView;
  icon: ComponentType;
  title: string;
  desc: string;
}[] = [
  {
    key: 'my-info',
    icon: PersonIcon,
    title: '내 정보',
    desc: '누적 통계 및 활동 내역',
  },
  {
    key: 'game-manual',
    icon: GameIcon,
    title: '게임 설명',
    desc: '각 미니게임의 방법과 규칙',
  },
  {
    key: 'report',
    icon: BubbleIcon,
    title: '건의사항 / 신고',
    desc: '버그 신고, 게임 추가 요청, 기타 건의',
  },
  {
    key: 'patch-notes',
    icon: ClipboardIcon,
    title: '패치 내역',
    desc: '업데이트 및 변경 사항',
  },
  {
    key: 'service-info',
    icon: InfoIcon,
    title: '서비스 정보',
    desc: '쫄(ZZOL) 소개 및 링크',
  },
];

const MenuTab = () => {
  const [activeView, setActiveView] = useState<MenuView | null>(null);

  if (activeView !== null) {
    return (
      <S.SubViewContainer>
        {activeView !== 'report' && (
          <S.SubViewHeader>
            <BackButton onClick={() => setActiveView(null)} text="메뉴로 가기" />
          </S.SubViewHeader>
        )}
        <S.SubViewContent>
          {activeView === 'report' && <SuggestionTab onBackToMenu={() => setActiveView(null)} />}
          {activeView === 'patch-notes' && <PatchNotesView />}
          {activeView === 'service-info' && <ServiceInfoView />}
          {activeView === 'my-info' && <MyInfoView />}
          {activeView === 'game-manual' && <GameManualView />}
        </S.SubViewContent>
      </S.SubViewContainer>
    );
  }

  return (
    <S.Container>
      <S.MenuList>
        {MENU_ITEMS.map(({ key, icon: Icon, title, desc }) => (
          <li key={key}>
            <S.MenuItemButton onClick={() => setActiveView(key)}>
              <S.MenuItemLeft>
                <S.MenuItemIcon>
                  <Icon />
                </S.MenuItemIcon>
                <S.MenuItemTexts>
                  <S.MenuItemTitle>{title}</S.MenuItemTitle>
                  <S.MenuItemDesc>{desc}</S.MenuItemDesc>
                </S.MenuItemTexts>
              </S.MenuItemLeft>
              <S.MenuItemChevron aria-hidden="true">›</S.MenuItemChevron>
            </S.MenuItemButton>
          </li>
        ))}
      </S.MenuList>
    </S.Container>
  );
};

export default MenuTab;
