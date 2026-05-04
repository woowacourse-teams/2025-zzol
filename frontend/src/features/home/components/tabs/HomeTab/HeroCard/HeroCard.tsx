import * as S from './HeroCard.styled';

type Props = {
  onClick: () => void;
};

const HeroCard = ({ onClick }: Props) => (
  <S.Card type="button" data-testid="create-room-button" onClick={onClick}>
    <S.Top>
      <S.IconCircle>＋</S.IconCircle>
      <S.Title>새 방 만들기</S.Title>
      <S.Description>방장으로 시작</S.Description>
    </S.Top>
    <S.Arrow>→</S.Arrow>
  </S.Card>
);

export default HeroCard;
