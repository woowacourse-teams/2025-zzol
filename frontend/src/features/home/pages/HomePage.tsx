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
import * as S from './HomePage.styled';
import DashBoard from '../components/DashBoard/DashBoard';

const HomePage = () => {
  const navigate = useReplaceNavigate();
  const [showSplash, setShowSplash] = useState<boolean>(false);
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
      <Layout.Banner height="55%">
        <S.Banner>
          <DashBoard />
        </S.Banner>
      </Layout.Banner>
      <S.ButtonContainer>
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
      </S.ButtonContainer>
    </Layout>
  );
};

export default HomePage;
