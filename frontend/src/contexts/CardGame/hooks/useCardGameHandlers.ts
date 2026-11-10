import { CardGameRound, CardInfo } from '@/types/miniGame/cardGame';
import type { MiniGameStateMessage } from '@/apis/websocket/generated/wsContract';
import React, { useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useIdentifier } from '../../Identifier/IdentifierContext';
import { Action } from '../reducer/cardGameReducer';

type CardGameStateHandlers = {
  updateSelectedCardInfo: (
    cardInfoMessages: CardInfo[],
    round: CardGameRound,
    shouldCheckAlreadySelected?: boolean
  ) => void;
};

export const useCardGameHandlers = (
  dispatch: React.Dispatch<Action>,
  { updateSelectedCardInfo }: CardGameStateHandlers
) => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useIdentifier();
  const { miniGameType } = useParams();

  const handleCardGameState = useCallback(
    (data: MiniGameStateMessage) => {
      const { cardGameState, cardInfoMessages } = data;
      // BE 의 RoundLabel 은 READY 를 포함한다. 라운드가 필요한 상태에서는 오지 않는다.
      const currentRound: CardGameRound | null =
        data.currentRound === 'READY' ? null : data.currentRound;

      switch (cardGameState) {
        case 'PREPARE':
          dispatch({ type: 'PREPARE', payload: { cardInfos: cardInfoMessages } });
          break;

        case 'PLAYING':
          if (currentRound === null) break;
          dispatch({
            type: 'PLAYING',
            payload: { cardInfos: cardInfoMessages, round: currentRound },
          });
          updateSelectedCardInfo(cardInfoMessages, currentRound);
          break;

        case 'SCORE_BOARD':
          if (currentRound === null) break;
          dispatch({
            type: 'SCORE_BOARD',
            payload: { cardInfos: cardInfoMessages, round: currentRound },
          });
          updateSelectedCardInfo(cardInfoMessages, currentRound, true);
          break;

        case 'LOADING':
          if (currentRound === null) break;
          dispatch({ type: 'LOADING', payload: { round: currentRound } });
          break;

        case 'DONE':
          dispatch({ type: 'DONE' });
          navigate(`/room/${joinCode}/${miniGameType}/result`);
          break;
      }
    },
    [dispatch, updateSelectedCardInfo, navigate, joinCode, miniGameType]
  );

  return {
    handleCardGameState,
  };
};
