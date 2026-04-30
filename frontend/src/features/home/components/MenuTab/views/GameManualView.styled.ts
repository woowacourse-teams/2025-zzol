import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 16px 32px;
`;

export const Header = styled.div`
  display: flex;
  flex-direction: column;
  gap: 3px;
`;

export const Title = styled.h3`
  font-size: 17px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.02em;
  margin: 0;
`;

export const Subtitle = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.color.gray[400]};
  margin: 0;
`;
