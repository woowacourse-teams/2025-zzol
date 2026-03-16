import * as S from './CurrentWord.styled';

type Props = {
  currentWord: string;
};

const CurrentWord = ({ currentWord }: Props) => {
  const lastChar = currentWord.charAt(currentWord.length - 1);

  return (
    <S.Container>
      <S.Label>현재 단어</S.Label>
      <S.Word>{currentWord}</S.Word>
      <S.LastChar>다음 글자: {lastChar}</S.LastChar>
    </S.Container>
  );
};

export default CurrentWord;
