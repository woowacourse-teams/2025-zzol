import Description from '@/components/@common/Description/Description';
import Headline2 from '@/components/@common/Headline2/Headline2';
import { ReactNode } from 'react';
import * as S from './SectionTitle.styled';

type Props = {
  title: string;
  description: string;
  suffix?: ReactNode;
};

const SectionTitle = ({ title, description, suffix }: Props) => {
  return (
    <S.Container>
      <S.Wrapper>
        <Headline2>{title}</Headline2>
        {suffix}
      </S.Wrapper>
      <Description color="gray-400">{description}</Description>
    </S.Container>
  );
};

export default SectionTitle;
