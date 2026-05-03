import styled from '@emotion/styled';

export const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 8px 0 4px;
`;

export const Title = styled.h2`
  ${({ theme }) => theme.typography.h3};
  color: ${({ theme }) => theme.color.gray[900]};
  text-align: center;
  margin-bottom: 4px;
`;

export const Description = styled.p`
  ${({ theme }) => theme.typography.small};
  color: ${({ theme }) => theme.color.gray[500]};
  text-align: center;
  margin-bottom: 8px;
`;

export const Divider = styled.hr`
  border: none;
  border-top: 1px solid ${({ theme }) => theme.color.gray[100]};
  margin: 4px 0;
`;

export const AnonButton = styled.button`
  width: 100%;
  padding: 12px 0;
  border: none;
  background: none;
  color: ${({ theme }) => theme.color.gray[500]};
  ${({ theme }) => theme.typography.small};
  text-decoration: underline;
  cursor: pointer;
  text-align: center;

  &:active {
    opacity: 0.7;
  }
`;
