import styled from '@emotion/styled';

export const Header = styled.header`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: calc(14px + env(safe-area-inset-top)) 20px 12px;
  background: ${({ theme }) => theme.color.white};
  box-shadow: 0 1px 0 ${({ theme }) => theme.color.gray[100]};
  flex-shrink: 0;
`;

export const Logo = styled.div`
  display: flex;
  align-items: center;
`;
