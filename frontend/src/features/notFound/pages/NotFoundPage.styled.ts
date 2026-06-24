import styled from '@emotion/styled';

export const Container = styled.section`
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 40px;
`;

export const NotFoundText = styled.h1`
  font-size: 40px;
  color: white;
  font-family: 'PartialSansKR-Regular';
`;

export const ButtonContainer = styled.button`
  width: 30%;
  display: flex;
  justify-content: center;
  align-items: center;

  text-decoration: underline;
  text-decoration-color: white;
  text-underline-offset: 4px;
`;
