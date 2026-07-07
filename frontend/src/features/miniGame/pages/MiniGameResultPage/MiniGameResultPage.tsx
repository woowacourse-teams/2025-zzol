import useFetch from '@/apis/rest/useFetch';
import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import Button from '@/components/@common/Button/Button';
import Description from '@/components/@common/Description/Description';
import Headline2 from '@/components/@common/Headline2/Headline2';
import Headline3 from '@/components/@common/Headline3/Headline3';
import Headline4 from '@/components/@common/Headline4/Headline4';
import PlayerCard from '@/components/@composition/PlayerCard/PlayerCard';
import MiniGameResultSkeleton from '@/components/@composition/MiniGameResultSkeleton/MiniGameResultSkeleton';
import { colorList } from '@/constants/color';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import Layout from '@/layouts/Layout';
import { MiniGameType } from '@/types/miniGame/common';
import { useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import * as S from './MiniGameResultPage.styled';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { useWebSocketSubscription } from '@/apis/websocket/hooks/useWebSocketSubscription';
import LocalErrorBoundary from '@/components/@common/ErrorBoundary/LocalErrorBoundary';
import { GAME_CONFIGS } from '../../config/gameConfigs';

type PlayerRank = {
  playerName: string;
  rank: number;
};
type PlayerRankResponse = {
  ranks: PlayerRank[];
};

type PlayerScore = {
  playerName: string;
  score: number;
};
type PlayerScoreResponse = {
  scores: PlayerScore[];
};

type ShowRouletteResponse = {
  joinCode: string;
  roomState: 'ROULETTE_SHOW';
};

const SECONDS_FORMATTER = new Intl.NumberFormat('ko-KR', {
  minimumFractionDigits: 2,
  maximumFractionDigits: 2,
});

const MiniGameResultPage = () => {
  const navigate = useReplaceNavigate();
  const miniGameType = useParams<{ miniGameType: MiniGameType }>().miniGameType;
  const { send } = useWebSocket();
  const { joinCode } = useIdentifier();
  const { playerType } = usePlayerType();

  const handleNavigateToRoulettePlayPage = useCallback(() => {
    navigate(`/room/${joinCode}/roulette/play`);
  }, [navigate, joinCode]);

  useWebSocketSubscription<ShowRouletteResponse>(
    `/room/${joinCode}/roulette`,
    handleNavigateToRoulettePlayPage
  );

  const handleClickRouletteResultButton = () => {
    send(`/room/${joinCode}/show-roulette`);
  };

  if (!miniGameType) return <div>잘못된 미니게임 타입입니다.</div>;

  // 게임 전용 결과 본문이 있으면 그것을, 없으면 공유 스코어보드를 Content 슬롯에 렌더한다.
  // 배너·룰렛 진행 버튼·룰렛 구독은 모든 게임 공통이므로 여기서 유지한다(룰렛 흐름 보존).
  const ResultContent = GAME_CONFIGS[miniGameType]?.ResultContent;

  return (
    <Layout>
      <Layout.Banner height="30%">
        <S.Banner>
          <Headline2 color="white">게임 결과</Headline2>
          <S.DescriptionWrapper>
            <Description color="white">게임 결과를 통해</Description>
            <Description color="white">룰렛 가중치가 조정됩니다</Description>
          </S.DescriptionWrapper>
        </S.Banner>
      </Layout.Banner>
      <Layout.Content>
        <LocalErrorBoundary>
          {ResultContent ? (
            <ResultContent />
          ) : (
            <ScoreBoardResultList joinCode={joinCode} miniGameType={miniGameType} />
          )}
        </LocalErrorBoundary>
      </Layout.Content>
      <Layout.ButtonBar>
        {playerType === 'HOST' ? (
          <Button
            variant="primary"
            onClick={handleClickRouletteResultButton}
            data-testid="roulette-result-button"
          >
            룰렛 현황 보러가기
          </Button>
        ) : (
          <Button variant="loading" loadingText="대기 중" />
        )}
      </Layout.ButtonBar>
    </Layout>
  );
};

export default MiniGameResultPage;

const getScoreTextByGameType = ({
  gameType,
  playScores,
  playerName,
}: {
  gameType: MiniGameType;
  playScores: PlayerScore[];
  playerName: string;
}) => {
  const playerScore = playScores.find((score) => score.playerName === playerName);
  const scoreValue = playerScore ? playerScore.score : 0;

  switch (gameType) {
    case 'RACING_GAME': {
      const seconds = scoreValue / 1000;
      return SECONDS_FORMATTER.format(seconds) + '초';
    }
    case 'CARD_GAME': {
      return scoreValue + '점';
    }
    case 'BLOCK_STACKING': {
      return scoreValue + '층';
    }
    case 'LADDER_GAME': {
      return scoreValue + '위';
    }
    default:
      return null;
  }
};

const ScoreBoardResultList = ({
  joinCode,
  miniGameType,
}: {
  joinCode: string;
  miniGameType: MiniGameType;
}) => {
  const { myName } = useIdentifier();
  const { getParticipantColorIndex } = useParticipants();

  const { data: ranksData, loading: ranksLoading } = useFetch<PlayerRankResponse>({
    endpoint: `/minigames/ranks?joinCode=${joinCode}&miniGameType=${miniGameType}`,
    enabled: !!(joinCode && miniGameType),
  });

  const { data: scoresData, loading: scoresLoading } = useFetch<PlayerScoreResponse>({
    endpoint: `/minigames/scores?joinCode=${joinCode}&miniGameType=${miniGameType}`,
    enabled: !!(joinCode && miniGameType),
  });

  const loading = ranksLoading || scoresLoading;

  const ranks = ranksData?.ranks?.sort((a, b) => a.rank - b.rank);
  const scores = scoresData?.scores;

  if (loading) {
    return <MiniGameResultSkeleton />;
  }

  if (!ranks || !scores) return <div>데이터를 불러오지 못했습니다.</div>;

  return (
    <S.ResultList>
      {ranks.map((playerRank) => (
        <S.PlayerCardWrapper
          key={playerRank.playerName}
          $isHighlighted={playerRank.playerName === myName}
        >
          <Headline3>
            <S.RankNumber $rank={playerRank.rank}>{playerRank.rank}</S.RankNumber>
          </Headline3>
          <PlayerCard
            name={playerRank.playerName}
            playerColor={colorList[getParticipantColorIndex(playerRank.playerName)]}
          >
            <Headline4>
              {getScoreTextByGameType({
                gameType: miniGameType,
                playScores: scores,
                playerName: playerRank.playerName,
              })}
            </Headline4>
          </PlayerCard>
        </S.PlayerCardWrapper>
      ))}
    </S.ResultList>
  );
};
