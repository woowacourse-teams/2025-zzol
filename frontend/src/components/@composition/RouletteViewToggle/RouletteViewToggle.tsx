import TextButton from '@/components/@common/TextButton/TextButton';
import { RouletteView } from '@/types/roulette';

type Props = {
  currentView: RouletteView;
  onViewChange: () => void;
};

const TOGGLE_TEXT_MAP = {
  roulette: '확률 보기',
  statistics: '룰렛 보기',
} as const;

const RouletteViewToggle = ({ currentView, onViewChange }: Props) => {
  const buttonText = TOGGLE_TEXT_MAP[currentView];
  return <TextButton text={buttonText} onClick={onViewChange} aria-label={buttonText} />;
};

export default RouletteViewToggle;
