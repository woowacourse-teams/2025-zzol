import { useState } from 'react';
import BackButton from '@/components/@common/BackButton/BackButton';
import SuggestionTab from '../SuggestionTab/SuggestionTab';
import PatchNotesView from './views/PatchNotesView';
import ServiceInfoView from './views/ServiceInfoView';
import * as S from './MenuTab.styled';

type MenuView = 'report' | 'patch-notes' | 'service-info';

const MENU_ITEMS: {
  key: MenuView;
  icon: string;
  title: string;
  desc: string;
}[] = [
  {
    key: 'report',
    icon: '🐛',
    title: '건의사항 / 신고',
    desc: '버그 신고, 게임 추가 요청, 기타 건의',
  },
  {
    key: 'patch-notes',
    icon: '📋',
    title: '패치 내역',
    desc: '업데이트 및 변경 사항',
  },
  {
    key: 'service-info',
    icon: 'ℹ️',
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
          {activeView === 'report' && (
            <SuggestionTab onBackToMenu={() => setActiveView(null)} />
          )}
          {activeView === 'patch-notes' && <PatchNotesView />}
          {activeView === 'service-info' && <ServiceInfoView />}
        </S.SubViewContent>
      </S.SubViewContainer>
    );
  }

  return (
    <S.Container>
      <S.MenuList>
        {MENU_ITEMS.map(({ key, icon, title, desc }) => (
          <S.MenuItem key={key} onClick={() => setActiveView(key)}>
            <S.MenuItemLeft>
              <S.MenuItemIcon>{icon}</S.MenuItemIcon>
              <S.MenuItemTexts>
                <S.MenuItemTitle>{title}</S.MenuItemTitle>
                <S.MenuItemDesc>{desc}</S.MenuItemDesc>
              </S.MenuItemTexts>
            </S.MenuItemLeft>
            <S.MenuItemChevron>›</S.MenuItemChevron>
          </S.MenuItem>
        ))}
      </S.MenuList>
    </S.Container>
  );
};

export default MenuTab;
