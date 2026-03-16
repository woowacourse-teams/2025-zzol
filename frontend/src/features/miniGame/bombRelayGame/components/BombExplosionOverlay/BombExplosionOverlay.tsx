import * as S from './BombExplosionOverlay.styled';

type Props = {
  eliminatedPlayerName: string;
  currentRound: number;
  isGameOver: boolean;
};

const BombExplosionOverlay = ({ eliminatedPlayerName, currentRound, isGameOver }: Props) => {
  return (
    <S.Overlay>
      <S.ExplosionRing />
      <S.BombEmoji>💥</S.BombEmoji>
      <S.EliminatedText>{eliminatedPlayerName} 탈락!</S.EliminatedText>
      <S.SubText>
        {isGameOver ? '게임 종료!' : `${currentRound}라운드 종료`}
      </S.SubText>
    </S.Overlay>
  );
};

export default BombExplosionOverlay;
