import styled from '@emotion/styled';

export const CarouselContainer = styled.div`
  width: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  gap: 12px;
  position: relative;
`;

export const SlideWrapper = styled.div<{ $currentIndex: number }>`
  display: flex;
  transition: transform 0.3s ease-in-out;
  transform: translateX(${({ $currentIndex }) => -$currentIndex * 100}%);
  width: 100%;
`;

export const SlideItem = styled.div`
  flex: 0 0 100%;
  width: 100%;
  padding: 0 2px;
  box-sizing: border-box;
`;

export const InfoSlide = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border-radius: 10px;
  background-color: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[100]};
  height: 100%;
  box-sizing: border-box;
`;

export const InfoStepNumber = styled.span`
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  font-size: 13px;
  font-weight: 700;
`;

export const InfoSlideBody = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 100%;
`;

export const InfoSlideText = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[800]};
  line-height: 1.5;
  margin: 0;
  text-align: center;
  word-break: keep-all;
`;

export const InfoSlideImage = styled.img`
  width: 100%;
  max-height: 140px;
  object-fit: contain;
  border-radius: 8px;
`;

export const Controls = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 4px;
  margin-top: 4px;
`;

export const ArrowButton = styled.button`
  background: none;
  border: none;
  color: ${({ theme }) => theme.color.gray[500]};
  font-size: 24px;
  font-weight: bold;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;

  &:hover {
    background-color: ${({ theme }) => theme.color.gray[100]};
    color: ${({ theme }) => theme.color.point[400]};
  }
`;

export const DotsContainer = styled.div`
  display: flex;
  gap: 6px;
`;

export const Dot = styled.button<{ $isActive: boolean }>`
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: ${({ theme, $isActive }) =>
    $isActive ? theme.color.point[400] : theme.color.gray[300]};
  border: none;
  padding: 0;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background-color: ${({ theme, $isActive }) =>
      $isActive ? theme.color.point[400] : theme.color.point[200]};
  }
`;
