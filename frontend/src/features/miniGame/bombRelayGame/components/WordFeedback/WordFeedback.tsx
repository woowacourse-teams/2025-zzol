import { BombRelayWordResult } from '@/types/miniGame/bombRelayGame';
import * as S from './WordFeedback.styled';

type Props = {
  result: BombRelayWordResult | null;
  myName: string;
};

const WordFeedback = ({ result, myName }: Props) => {
  if (!result) return null;

  // 오답은 입력한 본인에게만 표시
  if (!result.accepted && result.playerName !== myName) return null;

  return (
    <S.Container
      key={`${result.playerName}-${result.word}-${Date.now()}`}
      $accepted={result.accepted}
    >
      {result.accepted
        ? `✅ ${result.playerName}: "${result.word}" 정답!`
        : `❌ "${result.word}"${result.rejectReason ? ` - ${result.rejectReason}` : ''}`}
    </S.Container>
  );
};

export default WordFeedback;
