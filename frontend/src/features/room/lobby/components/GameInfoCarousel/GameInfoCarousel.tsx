import { useState } from 'react';
import { SlideConfig } from '@/features/miniGame/config/gameConfigs';
import * as S from './GameInfoCarousel.styled';

type Props = {
  slides: SlideConfig[];
  name: string;
};

export const GameInfoCarousel = ({ slides, name }: Props) => {
  const [currentIndex, setCurrentIndex] = useState(0);

  if (slides.length === 0) return null;

  const handlePrev = () => {
    setCurrentIndex((prev) => (prev === 0 ? slides.length - 1 : prev - 1));
  };

  const handleNext = () => {
    setCurrentIndex((prev) => (prev === slides.length - 1 ? 0 : prev + 1));
  };

  return (
    <S.CarouselContainer>
      <S.SlideWrapper $currentIndex={currentIndex}>
        {slides.map((slide, i) => (
          <S.SlideItem key={i}>
            <S.InfoSlide>
              {slide.imageSrc && (
                <S.InfoSlideImage src={slide.imageSrc} alt={`${name} 설명 ${i + 1}`} />
              )}
              <S.InfoSlideBody>
                <S.InfoStepNumber>{i + 1}</S.InfoStepNumber>
                <S.InfoSlideText>{slide.textLines.join(' ')}</S.InfoSlideText>
              </S.InfoSlideBody>
            </S.InfoSlide>
          </S.SlideItem>
        ))}
      </S.SlideWrapper>

      {slides.length > 1 && (
        <S.Controls>
          <S.ArrowButton onClick={handlePrev} aria-label="이전 슬라이드">
            &lt;
          </S.ArrowButton>
          <S.DotsContainer>
            {slides.map((_, i) => (
              <S.Dot
                key={i}
                $isActive={i === currentIndex}
                onClick={() => setCurrentIndex(i)}
                aria-label={`${i + 1}번째 슬라이드`}
              />
            ))}
          </S.DotsContainer>
          <S.ArrowButton onClick={handleNext} aria-label="다음 슬라이드">
            &gt;
          </S.ArrowButton>
        </S.Controls>
      )}
    </S.CarouselContainer>
  );
};

export default GameInfoCarousel;
