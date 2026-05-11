import styled from '@emotion/styled';

const SLIDER_TRACK_HEIGHT = '6px';
const SLIDER_THUMB_SIZE = '22px';
const SLIDER_THUMB_BORDER = '3px';

export const Content = styled.div`
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 4px 0 8px;
`;

export const ValueSection = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
`;

export const ValueText = styled.span`
  font-size: 2.4rem;
  font-weight: 700;
  line-height: 1;
  color: ${({ theme }) => theme.color.point[500]};
`;

export const DefaultTag = styled.span`
  padding: 3px 10px;
  border-radius: 20px;
  background-color: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[200]};
  ${({ theme }) => theme.typography.caption};
  color: ${({ theme }) => theme.color.point[400]};
  font-weight: 600;
`;

export const SliderSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const Slider = styled.input<{ $fillPercent: number }>`
  width: 100%;
  height: ${SLIDER_TRACK_HEIGHT};
  appearance: none;
  border-radius: 3px;
  background: linear-gradient(
    to right,
    ${({ theme }) => theme.color.point[400]} ${({ $fillPercent }) => $fillPercent}%,
    ${({ theme }) => theme.color.gray[200]} ${({ $fillPercent }) => $fillPercent}%
  );
  outline: none;
  cursor: pointer;

  &::-webkit-slider-thumb {
    appearance: none;
    width: ${SLIDER_THUMB_SIZE};
    height: ${SLIDER_THUMB_SIZE};
    border-radius: 50%;
    background: ${({ theme }) => theme.color.point[400]};
    border: ${SLIDER_THUMB_BORDER} solid ${({ theme }) => theme.color.white};
    box-shadow: 0 1px 4px ${({ theme }) => theme.color.gray[300]};
    cursor: pointer;
  }
`;

export const Description = styled.p`
  ${({ theme }) => theme.typography.small};
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
`;

export const RangeLabels = styled.div`
  display: flex;
  justify-content: space-between;
`;

export const RangeLabel = styled.span<{ $align: 'left' | 'right' }>`
  ${({ theme }) => theme.typography.caption};
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: ${({ $align }) => $align};
`;

export const ConfirmButton = styled.button`
  width: 100%;
  padding: 13px 0;
  border: none;
  border-radius: 10px;
  background-color: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  ${({ theme }) => theme.typography.paragraph};
  font-weight: 700;
  cursor: pointer;
  transition: opacity 0.15s ease;

  &:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  &:active:not(:disabled) {
    opacity: 0.85;
  }
`;
