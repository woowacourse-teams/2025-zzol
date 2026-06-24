import Headline3 from '../Headline3/Headline3';
import * as S from './ProbabilityTag.styled';

type Props = {
  probability: number;
};

const ProbabilityTag = ({ probability }: Props) => {
  return (
    <S.Container>
      <Headline3>{probability}% 확률로 당첨! </Headline3>
    </S.Container>
  );
};

export default ProbabilityTag;
