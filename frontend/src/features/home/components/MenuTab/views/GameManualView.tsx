import {
  HIDDEN_MINI_GAMES,
  MINI_GAME_DESCRIPTION_MAP,
  MINI_GAME_ICON_MAP,
  MINI_GAME_NAME_MAP,
  type MiniGameType,
} from '@/types/miniGame/common';
import { GAME_CONFIGS } from '@/features/miniGame/config/gameConfigs';
import useModal from '@/components/@common/Modal/useModal';
import GameInfoCarousel from '@/features/room/lobby/components/GameInfoCarousel/GameInfoCarousel';
import * as MS from '@/features/room/lobby/components/MiniGameSection/MiniGameSection.styled';
import * as MC from '@/features/home/components/tabs/HomeTab/MiniGameCarousel/MiniGameCarousel.styled';
import * as S from './GameManualView.styled';

const GAME_TYPES = (Object.keys(MINI_GAME_NAME_MAP) as MiniGameType[]).filter(
  (type) => !HIDDEN_MINI_GAMES.includes(type)
);

const GameManualView = () => {
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
    <S.Container>
      <S.HintBanner>
        <S.Subtitle>게임을 탭하면 자세한 규칙을 볼 수 있어요</S.Subtitle>
      </S.HintBanner>
      <MC.Grid>
        {GAME_TYPES.map((type, index) => (
          <MC.GameCard key={type} type="button" $index={index} onClick={() => handleGameClick(type)}>
            <MC.IconWrapper>
              <MC.GameIcon src={MINI_GAME_ICON_MAP[type]} alt="" aria-hidden="true" />
            </MC.IconWrapper>
            <MC.GameMeta>
              <MC.GameName>{MINI_GAME_NAME_MAP[type]}</MC.GameName>
              <MC.GameDesc>{MINI_GAME_DESCRIPTION_MAP[type][0]}</MC.GameDesc>
            </MC.GameMeta>
          </MC.GameCard>
        ))}
      </MC.Grid>
    </S.Container>
  );
};

export default GameManualView;
