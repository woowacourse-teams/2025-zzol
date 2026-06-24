import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import * as S from './NunchiEliminatedBanner.styled';

/**
 * 탈락자 배너(요구사항 2) — 플레이 화면 최상단에 또렷하게 고정.
 *
 * 무대(Stage) 안에 두면 중앙 숫자·비네트에 묻혀 잘 안 보이므로, 페이지 최상단 배너로 분리한다.
 * 탈락자가 없으면 렌더하지 않는다(생존자 시점에서 누가 죽었는지 한눈에 보이게).
 */
const NunchiEliminatedBanner = () => {
  const { myName } = useIdentifier();
  const { collided } = useNunchiGameContext();

  if (collided.length === 0) return null;

  return (
    <S.Banner role="status" aria-live="polite">
      <S.Label>탈락</S.Label>
      <S.Names>
        {collided.map((name) => (
          <S.Chip key={name} $isMe={name === myName}>
            {name}
          </S.Chip>
        ))}
      </S.Names>
    </S.Banner>
  );
};

export default NunchiEliminatedBanner;
