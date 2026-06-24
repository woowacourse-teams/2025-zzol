import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 1.2em;
  margin-top: 1rem;
`;

export const ButtonContainer = styled.div`
  display: flex;
  gap: 1rem;
`;

export const ErrorText = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.point[500]};
  height: 8px;
`;
