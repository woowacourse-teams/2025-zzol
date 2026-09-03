import styled from '@emotion/styled';

type ToneProps = {
  $isSlowing: boolean;
};

export const Container = styled.div`
  position: absolute;
  top: calc(100% + 8px);
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
`;

export const Bars = styled.div`
  display: flex;
  align-items: center;
  gap: 3px;
`;

type SegmentProps = ToneProps & {
  $isFilled: boolean;
};

export const Segment = styled.span<SegmentProps>`
  width: 7px;
  height: 11px;
  border-radius: 1px;
  background: ${({ theme, $isFilled, $isSlowing }) => {
    if (!$isFilled) return `${theme.color.white}47`;
    return $isSlowing ? theme.color.point[300] : theme.color.yellow;
  }};
`;

export const Arrow = styled.span<ToneProps>`
  margin-left: 3px;
  width: 0;
  height: 0;
  border-left: 5px solid transparent;
  border-right: 5px solid transparent;
  ${({ theme, $isSlowing }) =>
    $isSlowing
      ? `border-top: 6px solid ${theme.color.point[300]};`
      : `border-bottom: 6px solid ${theme.color.yellow};`}
`;

export const Readout = styled.span<ToneProps>`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme, $isSlowing }) => ($isSlowing ? theme.color.point[300] : theme.color.yellow)};
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.85);
`;
