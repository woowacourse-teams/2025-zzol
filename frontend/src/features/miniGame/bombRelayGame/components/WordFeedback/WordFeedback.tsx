import { BombRelayWordResult } from '@/types/miniGame/bombRelayGame';
import * as S from './WordFeedback.styled';

type Props = {
  result: BombRelayWordResult | null;
};

const WordFeedback = ({ result }: Props) => {
  if (!result) return null;

  return (
    <S.Container key={`${result.word}-${Date.now()}`} $accepted={result.accepted}>
      {result.accepted
        ? `✅ "${result.word}" 정답!`
        : `❌ "${result.word}" - ${result.rejectReason}`}
    </S.Container>
  );
};

export default WordFeedback;
