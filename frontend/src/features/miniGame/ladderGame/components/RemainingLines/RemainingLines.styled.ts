import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 16px 0;
  flex-shrink: 0;
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const Dots = styled.div`
  display: flex;
  gap: 5px;
`;

export const Dot = styled.span<{ $color: string; $used: boolean }>`
  width: 12px;
  height: 12px;
  border-radius: 50%;
  border: 2px solid ${({ $color }) => $color};
  background: ${({ $color, $used }) => ($used ? 'transparent' : $color)};
  opacity: ${({ $used }) => ($used ? 0.45 : 1)};
  transition:
    background 0.2s,
    opacity 0.2s;

  @media (prefers-reduced-motion: reduce) {
    transition: none;
  }
`;
