import useFetch from '@/apis/rest/useFetch';
import GameActionButton from '@/components/@common/GameActionButton/GameActionButton';
import ScreenReaderOnly from '@/components/@common/ScreenReaderOnly/ScreenReaderOnly';
import GameActionButtonSkeleton from '@/components/@composition/GameActionButtonSkeleton/GameActionButtonSkeleton';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import {
  HIDDEN_MINI_GAMES,
  MINI_GAME_DESCRIPTION_MAP,
  MINI_GAME_ICON_MAP,
  MINI_GAME_NAME_MAP,
  MiniGameType,
} from '@/types/miniGame/common';
import { useMemo } from 'react';
import * as S from './MiniGameSection.styled';
import { useMiniGameScreenReader } from './useMiniGameScreenReader';

/**
 * 백엔드가 아직 지원하지 않는 게임을 프론트에서 임시로 추가.
 * 백엔드 연동 완료 후 이 상수와 useMemo 보완 로직을 제거한다.
 */
const FRONTEND_ONLY_GAMES: MiniGameType[] = ['BLOCK_STACKING'];

type Props = {
  selectedMiniGames: MiniGameType[];
  handleMiniGameClick: (miniGameType: MiniGameType) => void;
};

export const MiniGameSection = ({ selectedMiniGames, handleMiniGameClick }: Props) => {
  const { playerType } = usePlayerType();
  const { data: miniGames, loading } = useFetch<MiniGameType[]>({
    endpoint: '/rooms/minigames',
  });

  const allMiniGames = useMemo(() => {
    const fromApi = miniGames ?? [];
    const extra = FRONTEND_ONLY_GAMES.filter((g) => !fromApi.includes(g));
    return [...fromApi, ...extra];
  }, [miniGames]);

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
          allMiniGames
            .filter((miniGame) => !HIDDEN_MINI_GAMES.includes(miniGame))
            .map((miniGame) => (
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
