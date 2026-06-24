import Headline4 from '@/components/@common/Headline4/Headline4';
import { useBlockStackingGameContext } from '@/contexts/BlockStackingGame/BlockStackingGameContext';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import PrepareOverlay from '../../components/PrepareOverlay/PrepareOverlay';
import BlockStackingCanvas from '../components/BlockStackingCanvas/BlockStackingCanvas';
import EliminatedOverlay from '../components/EliminatedOverlay/EliminatedOverlay';
import * as S from './BlockStackingGamePlayPage.styled';

const BlockStackingGamePlayPage = () => {
  const { joinCode } = useIdentifier();
  const { gameState, isLocalGameOver } = useBlockStackingGameContext();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();

  useEffect(() => {
    if (gameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [gameState, joinCode, navigate, miniGameType]);

  return (
    <Layout>
      <Layout.TopBar center={<Headline4>블록 쌓기</Headline4>} />
      <Layout.Content>
        <S.Container>
          <BlockStackingCanvas />
          {isLocalGameOver && <EliminatedOverlay />}
        </S.Container>
      </Layout.Content>
      {gameState === 'PREPARE' && <PrepareOverlay />}
    </Layout>
  );
};

export default BlockStackingGamePlayPage;
