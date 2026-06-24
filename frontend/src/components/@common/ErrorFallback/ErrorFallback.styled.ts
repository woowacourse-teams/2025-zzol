import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 32px 24px;
  text-align: center;
  background: ${({ theme }) => theme.color.white};
  border-radius: 16px;
  max-width: 400px;
  height: 100%;
  margin: 0 auto;
`;

export const Message = styled.p`
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.gray[600]};
  margin: 0;
  line-height: 1.5;
  word-break: keep-all;
`;
