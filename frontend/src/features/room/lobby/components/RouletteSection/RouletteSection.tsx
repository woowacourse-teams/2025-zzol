import ScreenReaderOnly from '@/components/@common/ScreenReaderOnly/ScreenReaderOnly';
import ProbabilityList from '@/components/@composition/ProbabilityList/ProbabilityList';
import RouletteViewToggle from '@/components/@composition/RouletteViewToggle/RouletteViewToggle';
import SectionTitle from '@/components/@composition/SectionTitle/SectionTitle';
import RouletteWheel from '@/features/roulette/components/RouletteWheel/RouletteWheel';
import { PlayerProbability, RouletteView } from '@/types/roulette';
import { useState } from 'react';
import * as S from './RouletteSection.styled';
import { useRouletteScreenReader } from './useRouletteScreenReader';

type Props = {
  playerProbabilities: PlayerProbability[];
};

export const RouletteSection = ({ playerProbabilities }: Props) => {
  const [currentView, setCurrentView] = useState<RouletteView>('roulette');
  const { message, updateViewMessage } = useRouletteScreenReader(playerProbabilities);

  const handleViewChange = () => {
    setCurrentView((prev) => (prev === 'roulette' ? 'statistics' : 'roulette'));
    updateViewMessage();
  };

  return (
    <>
      {message && <ScreenReaderOnly>{message}</ScreenReaderOnly>}
      <SectionTitle title="룰렛" description="미니게임을 통해 당첨 확률이 조정됩니다" />
      <S.IconButtonWrapper>
        <RouletteViewToggle currentView={currentView} onViewChange={handleViewChange} />
      </S.IconButtonWrapper>
      {renderContent(currentView, playerProbabilities)}
    </>
  );
};

const renderContent = (currentView: RouletteView, playerProbabilities: PlayerProbability[]) => {
  switch (currentView) {
    case 'statistics':
      return <ProbabilityList playerProbabilities={playerProbabilities} />;
    case 'roulette':
    default:
      return (
        <S.RouletteWheelWrapper>
          <RouletteWheel playerProbabilities={playerProbabilities} />
        </S.RouletteWheelWrapper>
      );
  }
};
