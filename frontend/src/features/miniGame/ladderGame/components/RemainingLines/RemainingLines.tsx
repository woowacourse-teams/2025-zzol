import { colorList } from '@/constants/color';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useLadderGameContext } from '@/contexts/LadderGame/LadderGameContext';
import { MAX_LINES_PER_PLAYER } from '@/types/miniGame/ladderGame';
import * as S from './RemainingLines.styled';

const RemainingLines = () => {
  const { poles, lines, ghost } = useLadderGameContext();
  const { myName } = useIdentifier();

  const myPole = poles.find((p) => p.playerName === myName);
  if (!myPole) return null;

  const color = colorList[(myPole.colorIndex ?? myPole.index) % colorList.length];
  const used = lines.filter((l) => l.playerName === myName).length + (ghost ? 1 : 0);
  const remaining = Math.max(0, MAX_LINES_PER_PLAYER - used);

  return (
    <S.Container role="status">
      <S.Dots aria-hidden>
        {Array.from({ length: MAX_LINES_PER_PLAYER }).map((_, i) => (
          <S.Dot key={i} $color={color} $used={i < used} />
        ))}
      </S.Dots>
      {remaining > 0 ? `선 ${remaining}개 남음` : '선을 모두 그었어요'}
    </S.Container>
  );
};

export default RemainingLines;
