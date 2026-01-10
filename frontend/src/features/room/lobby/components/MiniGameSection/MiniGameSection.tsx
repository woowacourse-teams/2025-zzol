import useFetch from '@/apis/rest/useFetch';
import GameActionButton from '@/components/@common/GameActionButton/GameActionButton';
import ScreenReaderOnly from '@/components/@common/ScreenReaderOnly/ScreenReaderOnly';
import GameActionButtonSkeleton from '@/components/@composition/GameActionButtonSkeleton/GameActionButtonSkeleton';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import {
  MINI_GAME_DESCRIPTION_MAP,
  MINI_GAME_ICON_MAP,
  MINI_GAME_NAME_MAP,
  MiniGameType,
} from '@/types/miniGame/common';
import * as S from './MiniGameSection.styled';
import { useMiniGameScreenReader } from './useMiniGameScreenReader';

type Props = {
  selectedMiniGames: MiniGameType[];
  handleMiniGameClick: (miniGameType: MiniGameType) => void;
};

export const MiniGameSection = ({ selectedMiniGames, handleMiniGameClick }: Props) => {
  const { playerType } = usePlayerType();
  const { data: miniGames, loading } = useFetch<MiniGameType[]>({
    endpoint: '/rooms/minigames',
  });

  const { message, screenReaderRef, announceSelection } = useMiniGameScreenReader(
    loading,
    !!miniGames?.length
  );

  const handleClick = (miniGame: MiniGameType) => {
    const isAlreadySelected = selectedMiniGames.includes(miniGame);

    handleMiniGameClick(miniGame);
    announceSelection(MINI_GAME_NAME_MAP[miniGame], isAlreadySelected);
  };

  return (
    <>
      {message && (
        <ScreenReaderOnly aria-live="assertive" ref={screenReaderRef}>
          {message}
        </ScreenReaderOnly>
      )}
      <S.Wrapper>
        {loading ? (
          <GameActionButtonSkeleton />
        ) : (
          miniGames?.map((miniGame) => (
            <GameActionButton
              key={miniGame}
              isSelected={selectedMiniGames.includes(miniGame)}
              isDisabled={playerType === 'GUEST'}
              gameName={MINI_GAME_NAME_MAP[miniGame]}
              description={MINI_GAME_DESCRIPTION_MAP[miniGame]}
              onClick={() => handleClick(miniGame)}
              icon={<S.Icon src={MINI_GAME_ICON_MAP[miniGame]} alt={miniGame} />}
              orderNumber={selectedMiniGames.indexOf(miniGame) + 1}
              data-testid={`game-action-${miniGame}`}
            />
          ))
        )}
      </S.Wrapper>
    </>
  );
};
