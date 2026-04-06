import useModal from '@/components/@common/Modal/useModal';
import RoomActionButton from '@/components/@common/RoomActionButton/RoomActionButton';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import Layout from '@/layouts/Layout';
import { useEffect, useState } from 'react';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import EnterRoomModal from '../components/EnterRoomModal/EnterRoomModal';
import Splash from '../components/Splash/Splash';
import HomeTabs, { type HomeTabType } from '../components/HomeTabs/HomeTabs';
import RankingTab from '../components/RankingTab/RankingTab';
import SuggestionTab from '../components/SuggestionTab/SuggestionTab';
import * as S from './HomePage.styled';
import DashBoard from '../components/DashBoard/DashBoard';

const HomePage = () => {
  const navigate = useReplaceNavigate();
  const [showSplash, setShowSplash] = useState<boolean>(false);
  const [activeTab, setActiveTab] = useState<HomeTabType>('game');
  const { openModal, closeModal } = useModal();
  const { setHost, setGuest } = usePlayerType();
  const { clearIdentifier } = useIdentifier();
  const { isConnected, stopSocket } = useWebSocket();

  useEffect(() => {
    if (isConnected) {
      stopSocket();
    }
    clearIdentifier();
  }, [clearIdentifier, isConnected, stopSocket]);

  useEffect(() => {
    const checkFirstVisit = () => {
      const hasVisited = storageManager.getItem(STORAGE_KEYS.VISITED, 'sessionStorage');

      if (!hasVisited) {
        setShowSplash(true);
        storageManager.setItem(STORAGE_KEYS.VISITED, 'true', 'sessionStorage');
      }
    };
    checkFirstVisit();
  }, []);

  if (showSplash) {
    return <Splash onComplete={() => setShowSplash(false)} />;
  }
  const handleEnterRoom = () => {
    openModal(<EnterRoomModal onClose={closeModal} />, {
      title: '방 참가하기',
      showCloseButton: true,
    });
    setGuest();
  };

  const handleClickHostButton = () => {
    setHost();
    setTimeout(() => {
      navigate('/entry/name');
    }, 120);
  };

  return (
    <Layout>
      <S.VisuallyHidden>
        <h1>쫄 (ZZOL) - 미니게임 기반 당첨자 추첨 서비스</h1>
        <p>
          {
            '점심 메뉴 고르기, 커피 내기, 당번 정하기처럼 매번 반복되는 결정을 쫄(ZZOL)로 해결하세요. 단순한 뽑기 대신 카드 게임, 레이싱, 스피드터치, 폭탄 돌리기 등 다양한 미니게임으로 당첨 확률을 직접 바꿀 수 있어 더 쫄깃한 경험을 제공합니다.'
          }
        </p>
        <p>
          {
            'QR코드로 친구와 함께 방에 입장하고, 미니게임 결과에 따라 룰렛 당첨 확률이 결정됩니다. 커피 내기, 밥값 내기, 업무 당번 정하기 등 무엇이든 공정하고 재미있게 결정할 수 있습니다.'
          }
        </p>
      </S.VisuallyHidden>
      {activeTab === 'game' && (
        <Layout.Banner height="55%">
          <S.Banner>
            <DashBoard />
          </S.Banner>
        </Layout.Banner>
      )}
      <S.ContentArea>
        {activeTab === 'game' && (
          <S.GameTabContent>
            <RoomActionButton
              title="방 만들기"
              descriptions={['새로운 방을 만들어', '재미있는 커피내기를 시작해보세요 ']}
              onClick={handleClickHostButton}
            />
            <RoomActionButton
              title="방 참가하러 가기"
              descriptions={['받은 초대 코드를 입력해서', '방으로 들어가보세요']}
              onClick={handleEnterRoom}
            />
          </S.GameTabContent>
        )}
        {activeTab === 'ranking' && <RankingTab />}
        {activeTab === 'suggestion' && <SuggestionTab />}
      </S.ContentArea>
      <HomeTabs activeTab={activeTab} onTabChange={setActiveTab} />
    </Layout>
  );
};

export default HomePage;
