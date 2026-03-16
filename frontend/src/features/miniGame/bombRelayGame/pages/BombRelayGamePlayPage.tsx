import Headline4 from '@/components/@common/Headline4/Headline4';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useBombRelayGame } from '@/contexts/BombRelayGame/BombRelayGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { useCallback, useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import PrepareOverlay from '../../components/PrepareOverlay/PrepareOverlay';
import CurrentWord from '../components/CurrentWord/CurrentWord';
import WordInput from '../components/WordInput/WordInput';
import PlayerList from '../components/PlayerList/PlayerList';
import WordFeedback from '../components/WordFeedback/WordFeedback';
import RoundInfo from '../components/RoundInfo/RoundInfo';
import BombExplosionOverlay from '../components/BombExplosionOverlay/BombExplosionOverlay';
import { useBombRelayActions } from '../hooks/useBombRelayActions';
import * as S from './BombRelayGamePlayPage.styled';

const BombRelayGamePlayPage = () => {
  const { joinCode, myName } = useIdentifier();
  const {
    gameState,
    currentRound,
    maxRounds,
    currentWord,
    currentTurnPlayerName,
    eliminatedPlayerName,
    progressData,
    lastWordResult,
  } = useBombRelayGame();
  const { sendWord } = useBombRelayActions();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();
  const doneTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const isMyTurn = currentTurnPlayerName === myName;
  const isPlaying = gameState === 'PLAYING';
  const showExplosion =
    (gameState === 'ROUND_RESULT' || gameState === 'DONE') && !!eliminatedPlayerName;

  const handleSubmitWord = useCallback(
    (word: string) => {
      if (!isMyTurn || !isPlaying) return;
      sendWord(word);
    },
    [isMyTurn, isPlaying, sendWord]
  );

  useEffect(() => {
    if (gameState === 'DONE') {
      doneTimerRef.current = setTimeout(() => {
        navigate(`/room/${joinCode}/${miniGameType}/result`);
      }, 3500);
    }

    return () => {
      if (doneTimerRef.current) {
        clearTimeout(doneTimerRef.current);
      }
    };
  }, [gameState, joinCode, navigate, miniGameType]);

  const turnBannerText = isMyTurn
    ? '💣 내 차례! 빨리 단어를 입력하세요!'
    : `${currentTurnPlayerName}의 차례입니다...`;

  return (
    <Layout>
      <Layout.TopBar center={<Headline4>폭탄 끝말잇기</Headline4>} />
      <Layout.Content>
        <S.Container $isMyTurn={isMyTurn && isPlaying}>
          <S.RoundSection>
            <RoundInfo
              currentRound={currentRound}
              maxRounds={maxRounds}
              currentTurnPlayerName={currentTurnPlayerName}
              myName={myName}
            />
          </S.RoundSection>
          {isPlaying && (
            <S.TurnBanner $isMyTurn={isMyTurn}>{turnBannerText}</S.TurnBanner>
          )}
          <S.WordSection>
            {currentWord && <CurrentWord currentWord={currentWord} />}
          </S.WordSection>
          <S.FeedbackSection>
            <WordFeedback result={lastWordResult} myName={myName} />
          </S.FeedbackSection>
          <S.PlayerSection>
            <PlayerList
              players={progressData.players}
              currentTurnPlayerName={currentTurnPlayerName}
              myName={myName}
            />
          </S.PlayerSection>
          <S.InputSection>
            <WordInput isMyTurn={isMyTurn && isPlaying} onSubmit={handleSubmitWord} />
          </S.InputSection>
        </S.Container>
      </Layout.Content>
      {gameState === 'PREPARE' && <PrepareOverlay />}
      {showExplosion && (
        <BombExplosionOverlay
          eliminatedPlayerName={eliminatedPlayerName}
          currentRound={currentRound}
          isGameOver={gameState === 'DONE'}
        />
      )}
    </Layout>
  );
};

export default BombRelayGamePlayPage;
