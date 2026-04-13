import BreadLogoWhiteIcon from '@/assets/logo/bread-logo-white.png';
import Button from '@/components/@common/Button/Button';
import Headline1 from '@/components/@common/Headline1/Headline1';
import Headline3 from '@/components/@common/Headline3/Headline3';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { useLocation } from 'react-router-dom';
import { useEffect, useRef } from 'react';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { storageManager, STORAGE_KEYS } from '@/utils/StorageManager';
import * as S from './RouletteResultPage.styled';

const RouletteResultPage = () => {
  const navigate = useReplaceNavigate();
  const { state } = useLocation() as ReturnType<typeof useLocation> & {
    state: { winner?: string } | null;
  };
  const { myName } = useIdentifier();
  const winner = state?.winner ?? '알 수 없는 사용자';
  const hasUpdatedStats = useRef(false);

  useEffect(() => {
    if (hasUpdatedStats.current || winner === '알 수 없는 사용자') return;

    const currentWinCount = Number(
      storageManager.getItem(STORAGE_KEYS.WIN_COUNT, 'localStorage', '0')
    );
    const currentStreak = Number(
      storageManager.getItem(STORAGE_KEYS.NON_WIN_STREAK, 'localStorage', '0')
    );

    if (winner === myName) {
      storageManager.setItem(STORAGE_KEYS.WIN_COUNT, String(currentWinCount + 1), 'localStorage');
      storageManager.setItem(STORAGE_KEYS.NON_WIN_STREAK, '0', 'localStorage');
    } else {
      storageManager.setItem(
        STORAGE_KEYS.NON_WIN_STREAK,
        String(currentStreak + 1),
        'localStorage'
      );
    }

    hasUpdatedStats.current = true;
  }, [winner, myName]);

  const handleClickGoMain = () => {
    navigate('/');
  };

  return (
    <Layout color="point-400">
      <Layout.Content>
        <S.Container>
          <S.Logo src={BreadLogoWhiteIcon} alt="" />
          <Headline1 color="white">{winner}</Headline1>
          <Headline3 color="white">님이 당첨되었습니다!</Headline3>
        </S.Container>
      </Layout.Content>
      <Layout.ButtonBar>
        <Button variant="secondary" onClick={handleClickGoMain}>
          메인 화면으로 가기
        </Button>
      </Layout.ButtonBar>
    </Layout>
  );
};

export default RouletteResultPage;
