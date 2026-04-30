import { MINI_GAME_ICON_MAP, MINI_GAME_NAME_MAP, MiniGameType } from '@/types/miniGame/common';
import { GAME_CONFIGS } from '@/features/miniGame/config/gameConfigs';
import useModal from '@/components/@common/Modal/useModal';
import GameActionButton from '@/components/@common/GameActionButton/GameActionButton';
import GameInfoCarousel from '@/features/room/lobby/components/GameInfoCarousel/GameInfoCarousel';
import * as S from './GameManualView.styled';
import * as MS from '@/features/room/lobby/components/MiniGameSection/MiniGameSection.styled';

const GameManualView = () => {
  const { openModal } = useModal();

  const handleGameClick = (miniGame: MiniGameType) => {
    const name = MINI_GAME_NAME_MAP[miniGame];
    const slides = GAME_CONFIGS[miniGame]?.slides ?? [];

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

  const gameTypes = Object.keys(MINI_GAME_NAME_MAP) as MiniGameType[];

  return (
    <S.Container>
      <S.Header>
        <S.Title>게임 설명</S.Title>
        <S.Subtitle>게임을 탭하면 자세한 규칙을 볼 수 있어요</S.Subtitle>
      </S.Header>
      <MS.Wrapper>
        {gameTypes.map((type) => (
          <GameActionButton
            key={type}
            isSelected={false}
            isDisabled={false}
            gameName={MINI_GAME_NAME_MAP[type]}
            onClick={() => handleGameClick(type)}
            onInfoClick={() => handleGameClick(type)}
            icon={<MS.Icon src={MINI_GAME_ICON_MAP[type]} alt={type} />}
          />
        ))}
      </MS.Wrapper>
    </S.Container>
  );
};

export default GameManualView;
