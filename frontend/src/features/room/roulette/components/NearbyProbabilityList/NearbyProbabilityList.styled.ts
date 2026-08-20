import { css } from '@emotion/react';
import styled from '@emotion/styled';

type RowProps = { $proximityRank: number; $isMine: boolean };
type DotProps = { $color: string };
type ChangeProps = { $isPositive: boolean };
type ContainerProps = { $isLoading: boolean };

/**
 * 근접 순위별로 행이 살아남는 최소 화면 높이(px).
 *
 * 화면에는 확률 내림차순으로 보이지만, 자리가 모자랄 때 사라져야 하는 것은
 * 나와 가장 먼 사람이다. 두 순서가 다르므로 nth-child 로는 고를 수 없고
 * 근접 순위를 받아 그 값으로 숨긴다. 나(0)와 가장 가까운 한 명(1)은 항상 남는다.
 */
const MIN_VIEWPORT_HEIGHT_BY_RANK: Record<number, number> = {
  2: 700,
  3: 800,
  4: 880,
};

export const Container = styled.div<ContainerProps>`
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  opacity: ${({ $isLoading }) => ($isLoading ? 0 : 1)};
  transition: opacity 0.3s ease-in-out;
`;

export const Caption = styled.p`
  ${({ theme }) => theme.typography.caption};
  color: ${({ theme }) => theme.color.gray[400]};
  padding-bottom: 0.375rem;
`;

export const List = styled.ul`
  display: flex;
  flex-direction: column;
  list-style: none;
`;

export const Row = styled.li<RowProps>`
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.25rem 0.5rem;
  border-radius: 8px;

  ${({ $isMine, theme }) =>
    $isMine &&
    css`
      background-color: ${theme.color.point[50]};
    `}

  ${({ $proximityRank }) => {
    const minHeight = MIN_VIEWPORT_HEIGHT_BY_RANK[$proximityRank];

    return (
      minHeight &&
      css`
        @media (max-height: ${minHeight - 1}px) {
          display: none;
        }
      `
    );
  }}
`;

export const Dot = styled.span<DotProps>`
  width: 0.75rem;
  height: 0.75rem;
  border-radius: 50%;
  flex-shrink: 0;
  background-color: ${({ $color }) => $color};
`;

// h4 와 paragraph 는 글자 크기가 같고 굵기만 다르다(600 / 500). 내 행만 h4 로 바꿔 강조한다.
export const Name = styled.span<{ $isMine: boolean }>`
  ${({ theme, $isMine }) => ($isMine ? theme.typography.h4 : theme.typography.paragraph)};
  color: ${({ theme }) => theme.color.gray[700]};
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Probability = styled.span`
  ${({ theme }) => theme.typography.h4};
  color: ${({ theme }) => theme.color.gray[700]};
  font-variant-numeric: tabular-nums;
`;

export const Change = styled.span<ChangeProps>`
  font-size: ${({ theme }) => theme.typography.caption.fontSize};
  line-height: ${({ theme }) => theme.typography.caption.lineHeight};
  font-weight: ${({ theme }) => theme.typography.h4.fontWeight};
  color: ${({ $isPositive, theme }) => ($isPositive ? theme.color.red : theme.color.blue)};
  background-color: ${({ theme }) => theme.color.white};
  border-radius: 999px;
  padding: 0.0625rem 0.4375rem;
  font-variant-numeric: tabular-nums;
`;
