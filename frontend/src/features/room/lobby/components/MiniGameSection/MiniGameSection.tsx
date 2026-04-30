import useFetch from '@/apis/rest/useFetch';
import GameActionButton from '@/components/@common/GameActionButton/GameActionButton';
import ScreenReaderOnly from '@/components/@common/ScreenReaderOnly/ScreenReaderOnly';
import GameActionButtonSkeleton from '@/components/@composition/GameActionButtonSkeleton/GameActionButtonSkeleton';
import useModal from '@/components/@common/Modal/useModal';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import {
  HIDDEN_MINI_GAMES,
  MINI_GAME_DESCRIPTION_MAP,
  MINI_GAME_ICON_MAP,
  MINI_GAME_NAME_MAP,
  MiniGameType,
} from '@/types/miniGame/common';
import { GAME_CONFIGS } from '@/features/miniGame/config/gameConfigs';
import * as S from './MiniGameSection.styled';
import { useMiniGameScreenReader } from './useMiniGameScreenReader';

type Props = {
  selectedMiniGames: MiniGameType[];
  handleMiniGameClick: (miniGameType: MiniGameType) => void;
};

export const MiniGameSection = ({ selectedMiniGames, handleMiniGameClick }: Props) => {
  const { playerType } = usePlayerType();
  const { openModal } = useModal();
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

  const handleInfoClick = (miniGame: MiniGameType) => {
    const name = MINI_GAME_NAME_MAP[miniGame];
    const slides = GAME_CONFIGS[miniGame]?.slides ?? [];
    const descriptions = MINI_GAME_DESCRIPTION_MAP[miniGame];

    openModal(
      <S.InfoContent>
        {slides.map((slide, i) => (
          <S.InfoSlide key={i}>
            {slide.imageSrc && (
              <S.InfoSlideImage src={slide.imageSrc} alt={`${name} 설명 ${i + 1}`} />
            )}
            <S.InfoSlideBody>
              <S.InfoStepNumber>{i + 1}</S.InfoStepNumber>
              <S.InfoSlideText>{slide.textLines.join(' ')}</S.InfoSlideText>
            </S.InfoSlideBody>
          </S.InfoSlide>
        ))}
        {descriptions.length > 0 && <S.InfoSummary>{descriptions.join(' ')}</S.InfoSummary>}
      </S.InfoContent>,
      {
        title: name,
        showCloseButton: false,
        closeOnBackdropClick: true,
        showBottomCloseButton: true,
      }
    );
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
          (miniGames ?? [])
            .filter((miniGame) => miniGame in MINI_GAME_NAME_MAP)
            .filter((miniGame) => !HIDDEN_MINI_GAMES.includes(miniGame))
            .map((miniGame) => (
              <GameActionButton
                key={miniGame}
                isSelected={selectedMiniGames.includes(miniGame)}
                isDisabled={playerType === 'GUEST'}
                gameName={MINI_GAME_NAME_MAP[miniGame]}
                onClick={() => handleClick(miniGame)}
                icon={
                  <S.Icon src={MINI_GAME_ICON_MAP[miniGame]} alt={MINI_GAME_NAME_MAP[miniGame]} />
                }
                orderNumber={selectedMiniGames.indexOf(miniGame) + 1}
                onInfoClick={() => handleInfoClick(miniGame)}
                data-testid={`game-action-${miniGame}`}
              />
            ))
        )}
      </S.Wrapper>
    </>
  );
};
