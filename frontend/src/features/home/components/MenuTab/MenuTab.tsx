import { useState } from 'react';
import type { ComponentType } from 'react';
import BackButton from '@/components/@common/BackButton/BackButton';
import TopBar from '@/layouts/TopBar/TopBar';
import SuggestionTab from '../SuggestionTab/SuggestionTab';
import AccountSection from '../tabs/MenuTab/AccountSection/AccountSection';
import PatchNotesView from './views/PatchNotesView';
import ServiceInfoView from './views/ServiceInfoView';
import MyInfoView from './views/MyInfoView';
import GameManualView from './views/GameManualView';
import AppInstallView from './views/AppInstallView';
import * as S from './MenuTab.styled';
import {
  PersonIcon,
  BubbleIcon,
  ClipboardIcon,
  InfoIcon,
  GameIcon,
  DownloadIcon,
} from './menuIcons';

export type MenuView =
  | 'my-info'
  | 'game-manual'
  | 'report'
  | 'patch-notes'
  | 'service-info'
  | 'app-install';

const VIEW_TITLE: Record<MenuView, string> = {
  'my-info': '내 정보',
  'game-manual': '게임 설명',
  report: '건의사항 / 신고',
  'patch-notes': '패치 내역',
  'service-info': '서비스 정보',
  'app-install': '앱 설치',
};

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
  {
    key: 'app-install',
    icon: DownloadIcon,
    title: '앱 설치',
    desc: '홈 화면에 추가하여 앱처럼 사용',
  },
];

type Props = { initialView?: MenuView | null };

const MenuTab = ({ initialView }: Props) => {
  const [activeView, setActiveView] = useState<MenuView | null>(initialView ?? null);

  if (activeView !== null) {
    return (
      <S.SubViewContainer>
        <TopBar
          left={<BackButton onClick={() => setActiveView(null)} />}
          center={<S.SubViewTitle>{VIEW_TITLE[activeView]}</S.SubViewTitle>}
        />
        <S.SubViewContent>
          {activeView === 'report' && <SuggestionTab />}
          {activeView === 'patch-notes' && <PatchNotesView />}
          {activeView === 'service-info' && <ServiceInfoView />}
          {activeView === 'my-info' && <MyInfoView />}
          {activeView === 'game-manual' && <GameManualView />}
          {activeView === 'app-install' && <AppInstallView />}
        </S.SubViewContent>
      </S.SubViewContainer>
    );
  }

  return (
    <S.Container>
      <AccountSection />
      <S.SectionLabel>서비스</S.SectionLabel>
      <S.MenuCard>
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
      </S.MenuCard>
      <S.VersionText>ZZOL · v1.0.0</S.VersionText>
    </S.Container>
  );
};

export default MenuTab;
