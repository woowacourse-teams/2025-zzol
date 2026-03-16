import * as S from './RoundInfo.styled';

type Props = {
  currentRound: number;
  maxRounds: number;
  currentTurnPlayerName: string;
  myName: string;
};

const RoundInfo = ({ currentRound, maxRounds, currentTurnPlayerName, myName }: Props) => {
  const isMyTurn = currentTurnPlayerName === myName;
  const turnText = isMyTurn ? '내 차례!' : `${currentTurnPlayerName}의 차례`;

  return (
    <S.Container>
      <S.RoundBadge>
        라운드 {currentRound}/{maxRounds}
      </S.RoundBadge>
      <S.TurnInfo>💣 {turnText}</S.TurnInfo>
    </S.Container>
  );
};

export default RoundInfo;
