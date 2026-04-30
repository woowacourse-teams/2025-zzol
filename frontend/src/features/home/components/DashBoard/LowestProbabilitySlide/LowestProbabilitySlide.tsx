import * as S from './LowestProbabilitySlide.styled';

type Props = {
  WinnerNames: string[];
  probability: number;
};

const LowestProbabilitySlide = ({ WinnerNames, probability }: Props) => {
  const pct = (probability * 100).toFixed(1);

  return (
    <S.Card>
      <S.Header>
        <S.CardTitle>최저 확률 당첨</S.CardTitle>
        <S.Sub>이번달 가장 짜릿한 역전</S.Sub>
      </S.Header>
      {WinnerNames.length === 0 ? (
        <S.Empty>아직 당첨자가 없어요</S.Empty>
      ) : (
        <S.Content>
          <S.ProbRow>
            <S.BigProb>{pct}%</S.BigProb>
            <S.ProbLabel>당첨 확률</S.ProbLabel>
          </S.ProbRow>
          <S.Divider />
          <S.Names>{WinnerNames.join(', ')}</S.Names>
        </S.Content>
      )}
    </S.Card>
  );
};

export default LowestProbabilitySlide;
