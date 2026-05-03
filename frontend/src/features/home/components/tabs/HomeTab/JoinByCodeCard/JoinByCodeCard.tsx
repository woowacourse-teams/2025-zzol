import * as S from './JoinByCodeCard.styled';

type Props = {
  onClick: () => void;
};

const JoinByCodeCard = ({ onClick }: Props) => (
  <S.Card type="button" onClick={onClick}>
    <S.Top>
      <S.IconCircle>#</S.IconCircle>
      <S.Title>코드로{'\n'}참가하기</S.Title>
      <S.Sub>초대코드 입력</S.Sub>
    </S.Top>
    <S.Arrow>→</S.Arrow>
  </S.Card>
);

export default JoinByCodeCard;
