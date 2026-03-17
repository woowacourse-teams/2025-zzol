import * as S from './RoundInfo.styled';

type Props = {
  currentRound: number;
  maxRounds: number;
};

const RoundInfo = ({ currentRound, maxRounds }: Props) => {
  return (
    <S.Container>
      <S.RoundBadge>
        ROUND {currentRound} / {maxRounds}
      </S.RoundBadge>
    </S.Container>
  );
};

export default RoundInfo;
