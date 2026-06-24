import * as S from './GuideContent.styled';
import { GuideInfo } from '../GuideModal';
import Headline4 from '@/components/@common/Headline4/Headline4';

type Props = {
  pageData: GuideInfo;
};

const GuideContent = ({ pageData }: Props) => {
  return (
    <S.ContentContainer>
      <S.ImageContainer>{pageData.image}</S.ImageContainer>
      <S.TextContainer>
        <Headline4>{pageData.title}</Headline4>
        <S.DescriptionWrapper>
          {pageData.description.map((line, index) => (
            <S.Description key={index}>{line}</S.Description>
          ))}
        </S.DescriptionWrapper>
      </S.TextContainer>
    </S.ContentContainer>
  );
};

export default GuideContent;
