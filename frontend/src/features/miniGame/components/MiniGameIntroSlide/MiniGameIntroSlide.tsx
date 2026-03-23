import Headline2 from '@/components/@common/Headline2/Headline2';
import * as S from './MiniGameIntroSlide.styled';

type Props = {
  textLines: string[];
  imageSrc?: string;
  className: string;
};

const MiniGameIntroSlide = ({ textLines, imageSrc, className }: Props) => {
  return (
    <S.Container className={className}>
      <S.TextWrapper>
        {textLines.map((text, index) => (
          <Headline2 key={index} color="white">
            {text}
          </Headline2>
        ))}
      </S.TextWrapper>
      {imageSrc && (
        <S.ImageWrapper>
          <S.Image src={imageSrc} />
        </S.ImageWrapper>
      )}
    </S.Container>
  );
};

export default MiniGameIntroSlide;
