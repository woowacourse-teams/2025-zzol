import useModal from '@/components/@common/Modal/useModal';
import GameInfoCarousel from '@/features/room/lobby/components/GameInfoCarousel/GameInfoCarousel';
import * as MS from '@/features/room/lobby/components/MiniGameSection/MiniGameSection.styled';
import { GAME_CONFIGS } from '@/features/miniGame/config/gameConfigs';
import {
  HIDDEN_MINI_GAMES,
  MINI_GAME_DESCRIPTION_MAP,
  MINI_GAME_ICON_MAP,
  MINI_GAME_NAME_MAP,
  type MiniGameType,
} from '@/types/miniGame/common';
import * as S from './MiniGameCarousel.styled';


const GAME_TYPES = (Object.keys(MINI_GAME_NAME_MAP) as MiniGameType[]).filter(
  (type) => !HIDDEN_MINI_GAMES.includes(type)
);

const MiniGameCarousel = () => {
  const { openModal } = useModal();

  const handleGameClick = (type: MiniGameType) => {
    const name = MINI_GAME_NAME_MAP[type];
    const slides = GAME_CONFIGS[type]?.slides ?? [];
    openModal(
      <MS.InfoContent>
        <GameInfoCarousel slides={slides} name={name} />
      </MS.InfoContent>,
      {
        title: name,
        showCloseButton: false,
        closeOnBackdropClick: true,
        showBottomCloseButton: true,
      }
    );
  };

  return (
    <S.Grid>
      {GAME_TYPES.map((type) => (
        <S.GameCard key={type} type="button" onClick={() => handleGameClick(type)}>
          <S.IconWrapper>
            <S.GameIcon src={MINI_GAME_ICON_MAP[type]} alt="" aria-hidden="true" />
          </S.IconWrapper>
          <S.GameMeta>
            <S.GameName>{MINI_GAME_NAME_MAP[type]}</S.GameName>
            <S.GameDesc>{MINI_GAME_DESCRIPTION_MAP[type][0]}</S.GameDesc>
          </S.GameMeta>
        </S.GameCard>
      ))}
    </S.Grid>
  );
};

export default MiniGameCarousel;
