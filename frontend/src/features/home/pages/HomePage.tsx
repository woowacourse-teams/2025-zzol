import { useEffect, useState } from 'react';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useFriends } from '@/features/friends/hooks/useFriends';
import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import HomeHeader from '../components/HomeHeader/HomeHeader';
import HomeBottomTab, { type HomeTabType } from '../components/HomeBottomTab/HomeBottomTab';
import { type MenuView } from '../components/MenuTab/MenuTab';
import HomeTab from '../components/tabs/HomeTab/HomeTab';
import RankingTab from '../components/RankingTab/RankingTab';
import MenuTab from '../components/MenuTab/MenuTab';
import FriendsTab from '../components/FriendsTab/FriendsTab';
import Splash from '../components/Splash/Splash';
import * as S from './HomePage.styled';

const HomePage = () => {
  const [showSplash, setShowSplash] = useState(false);
  const [activeTab, setActiveTab] = useState<HomeTabType>('home');
  const [menuInitialView, setMenuInitialView] = useState<MenuView | null>(null);
  const { clearIdentifier } = useIdentifier();
  const { isConnected, stopSocket } = useWebSocket();
  const { pendingReceivedCount } = useFriends();

  useEffect(() => {
    if (isConnected) stopSocket();
    clearIdentifier();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // 메뉴 탭을 벗어나면 initialView 초기화 (다음 번 직접 진입 시 루트 메뉴로 복귀)
  useEffect(() => {
    if (activeTab !== 'menu') setMenuInitialView(null);
  }, [activeTab]);

  useEffect(() => {
    const hasVisited = storageManager.getItem(STORAGE_KEYS.VISITED, 'sessionStorage');
    if (!hasVisited) {
      setShowSplash(true);
      storageManager.setItem(STORAGE_KEYS.VISITED, 'true', 'sessionStorage');
    }
  }, []);

  if (showSplash) {
    return <Splash onComplete={() => setShowSplash(false)} />;
  }

  return (
    <S.PageContainer>
      <S.VisuallyHidden>
        <h1>쫄 (ZZOL) - 미니게임 기반 당첨자 추첨 서비스</h1>
        <p>
          점심 메뉴 고르기, 커피 내기, 당번 정하기처럼 매번 반복되는 결정을 쫄(ZZOL)로 해결하세요.
          단순한 뽑기 대신 카드 게임, 레이싱, 스피드터치 등 다양한 미니게임으로 당첨 확률을 직접
          바꿀 수 있어 더 쫄깃한 경험을 제공합니다.
        </p>
      </S.VisuallyHidden>
      <HomeHeader />
      <S.ScrollArea>
        {activeTab === 'home' && (
          <HomeTab
            onNavigateToPatchNotes={() => {
              setMenuInitialView('patch-notes');
              setActiveTab('menu');
            }}
          />
        )}
        {activeTab === 'ranking' && <RankingTab />}
        {activeTab === 'friends' && <FriendsTab />}
        {activeTab === 'menu' && <MenuTab initialView={menuInitialView} />}
      </S.ScrollArea>
      <HomeBottomTab
        activeTab={activeTab}
        onTabChange={setActiveTab}
        friendsBadgeCount={pendingReceivedCount}
      />
    </S.PageContainer>
  );
};

export default HomePage;
