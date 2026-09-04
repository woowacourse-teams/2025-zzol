import Headline1 from '@/components/@common/Headline1/Headline1';
import * as S from './Goal.styled';

const Goal = () => {
  return (
    <S.Container role="status" aria-live="assertive">
      <S.Plate>
        <Headline1 color="white">Goal!</Headline1>
      </S.Plate>
    </S.Container>
  );
};

export default Goal;
