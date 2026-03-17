import * as S from './CurrentWord.styled';

type Props = {
  currentWord: string;
};

const CurrentWord = ({ currentWord }: Props) => {
  const lastChar = currentWord.charAt(currentWord.length - 1);

  return (
    <S.Container>
      <S.WordCard>
        <S.Label>현재 단어</S.Label>
        <S.Word>{currentWord}</S.Word>
      </S.WordCard>
      <S.NextCharContainer>
        <S.NextCharLabel>다음 글자</S.NextCharLabel>
        <S.NextChar>{lastChar}</S.NextChar>
      </S.NextCharContainer>
    </S.Container>
  );
};

export default CurrentWord;
