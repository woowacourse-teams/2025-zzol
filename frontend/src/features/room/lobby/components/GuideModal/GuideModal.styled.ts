import styled from '@emotion/styled';

export const CloseButton = styled.button`
  color: ${({ theme }) => theme.color.gray[400]};
  ${({ theme }) => theme.typography.small}
  cursor: pointer;
  padding: 4px 8px;
  text-decoration: underline;
`;

export const Body = styled.div`
  padding: 32px 24px 24px;
  display: flex;
  flex-direction: column;
  min-height: 400px;
`;

export const Image = styled.img`
  width: 100%;
  height: 300px;
  object-fit: contain;
`;
