import { CardGameRound, CardInfo, SelectedCardInfo } from '@/types/miniGame/cardGame';
import { useCallback, useState } from 'react';

export const useSelectedCard = (myName: string) => {
  const [selectedCardInfo, setSelectedCardInfo] = useState<SelectedCardInfo>({
    FIRST: {
      isSelected: false,
      type: null,
      value: null,
    },
    SECOND: {
      isSelected: false,
      type: null,
      value: null,
    },
  });

  const updateSelectedCardInfo = useCallback(
    (cardInfoMessages: CardInfo[], round: CardGameRound, shouldCheckAlreadySelected = false) => {
      const myCardInfo = cardInfoMessages.find((card) => card.playerName === myName);
      if (!myCardInfo) return;

      setSelectedCardInfo((prev) => {
        if (shouldCheckAlreadySelected && prev[round].isSelected) return prev;

        return {
          ...prev,
          [round]: {
            isSelected: true,
            type: myCardInfo.cardType,
            value: myCardInfo.value,
          },
        };
      });
    },
    [myName]
  );

  return {
    selectedCardInfo,
    updateSelectedCardInfo,
  };
};
