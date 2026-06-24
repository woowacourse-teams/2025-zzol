import { useWebSocket } from '@/apis/websocket/contexts/WebSocketContext';
import { colorList } from '@/constants/color';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useParticipants } from '@/contexts/Participants/ParticipantsContext';
import { useRacingGame } from '@/contexts/RacingGame/RacingGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useEffect, useRef } from 'react';
import { useParams } from 'react-router-dom';
import PrepareOverlay from '../../components/PrepareOverlay/PrepareOverlay';
import Finish from '../components/Finish/Finish';
import Goal from '../components/Goal/Goal';
import RacingGameOverlay from '../components/RacingGameOverlay/RacingGameOverlay';
import RacingLine from '../components/RacingLine/RacingLine';
import RacingPlayer from '../components/RacingPlayer/RacingPlayer';
import RacingProgressBar from '../components/RacingProgressBar/RacingProgressBar';
import RacingRanks from '../components/RacingRanks/RacingRanks';
import { useBackgroundAnimation } from '../hooks/useBackgroundAnimation';
import { useGoalDisplay } from '../hooks/useGoalDisplay';
import { usePlayerData } from '../hooks/usePlayerData';
import { getVisiblePlayers } from '../utils/getVisiblePlayers';
import * as S from './RacingGamePlayPage.styled';

const FINISH_LINE_VISUAL_OFFSET = 30;

const RacingGamePage = () => {
  const { joinCode, myName } = useIdentifier();
  const { send } = useWebSocket();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();
  const { racingGameState, racingGameData } = useRacingGame();
  const { getParticipantColorIndex } = useParticipants();

  const containerRef = useRef<HTMLDivElement | null>(null);

  const visiblePlayers = getVisiblePlayers(racingGameData.players, myName);

  const { myPosition, mySpeed } = usePlayerData({
    players: visiblePlayers,
    myName,
  });

  const isGoal = useGoalDisplay({
    myPosition,
    endDistance: racingGameData.distance.end,
  });

  useBackgroundAnimation({
    containerRef,
    mySpeed,
  });

  useEffect(() => {
    setTimeout(() => {
      send(`/room/${joinCode}/racing-game/start`, {
        hostName: myName,
      });
    }, 2000);
  }, [joinCode, send, myName]);

  useEffect(() => {
    if (racingGameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [racingGameState, joinCode, navigate, miniGameType]);

  return (
    <>
      {racingGameState === 'PREPARE' && <PrepareOverlay />}
      {racingGameState === 'DONE' && <Finish />}
      {isGoal && racingGameState === 'PLAYING' && <Goal />}
      <RacingGameOverlay isGoal={isGoal}>
        <S.Container ref={containerRef}>
          <RacingRanks
            players={racingGameData.players}
            myName={myName}
            endDistance={racingGameData.distance.end}
          />
          <RacingProgressBar
            myName={myName}
            endDistance={racingGameData.distance.end}
            players={racingGameData.players}
          />
          <S.ContentWrapper>
            <S.PlayersWrapper>
              {/* 출발선 */}
              <RacingLine position={racingGameData.distance.start} myPosition={myPosition} />
              {/* 도착선 */}
              <RacingLine
                position={racingGameData.distance.end - FINISH_LINE_VISUAL_OFFSET}
                myPosition={myPosition}
              />
              {visiblePlayers.map((player) => (
                <RacingPlayer
                  key={player.playerName}
                  player={player}
                  isMe={player.playerName === myName}
                  myPosition={myPosition}
                  color={colorList[getParticipantColorIndex(player.playerName)]}
                />
              ))}
            </S.PlayersWrapper>
          </S.ContentWrapper>
        </S.Container>
      </RacingGameOverlay>
    </>
  );
};

export default RacingGamePage;
