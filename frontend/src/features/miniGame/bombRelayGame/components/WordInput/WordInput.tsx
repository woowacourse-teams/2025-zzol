import React, { useCallback, useState } from 'react';
import * as S from './WordInput.styled';

type Props = {
  isMyTurn: boolean;
  onSubmit: (word: string) => void;
};

const WordInput = ({ isMyTurn, onSubmit }: Props) => {
  const [word, setWord] = useState('');

  const handleSubmit = useCallback(() => {
    const trimmed = word.trim();
    if (!trimmed || !isMyTurn) return;
    onSubmit(trimmed);
    setWord('');
  }, [word, isMyTurn, onSubmit]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter') {
        handleSubmit();
      }
    },
    [handleSubmit]
  );

  return (
    <S.Container>
      <S.Input
        $isMyTurn={isMyTurn}
        value={word}
        onChange={(e) => setWord(e.target.value)}
        onKeyDown={handleKeyDown}
        placeholder={isMyTurn ? '단어를 입력하세요' : '상대방 차례입니다'}
        disabled={!isMyTurn}
        autoFocus={isMyTurn}
      />
      <S.SubmitButton
        $disabled={!isMyTurn || !word.trim()}
        disabled={!isMyTurn || !word.trim()}
        onClick={handleSubmit}
      >
        전송
      </S.SubmitButton>
    </S.Container>
  );
};

export default WordInput;
