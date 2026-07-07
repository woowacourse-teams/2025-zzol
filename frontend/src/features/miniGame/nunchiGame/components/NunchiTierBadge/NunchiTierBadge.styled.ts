import styled from '@emotion/styled';
import { css } from '@emotion/react';
import { NunchiResultTier } from '@/types/miniGame/nunchiGame';

export const Badge = styled.span<{ $tier: NunchiResultTier }>`
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;

  ${({ $tier, theme }) => {
    switch ($tier) {
      case 'SOLO':
        return css`
          background: ${theme.color.point[100]};
          color: ${theme.color.point[500]};
        `;
      case 'COLLISION':
        return css`
          background: ${theme.color.gray[200]};
          color: ${theme.color.gray[700]};
        `;
      case 'MISS':
      default:
        return css`
          background: ${theme.color.gray[100]};
          color: ${theme.color.gray[500]};
        `;
    }
  }}
`;
