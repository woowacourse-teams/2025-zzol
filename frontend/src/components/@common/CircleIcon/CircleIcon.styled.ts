import styled from '@emotion/styled';

type Props = {
  $color: string;
};

export const Container = styled.div<Props>`
  width: 50px;
  height: 50px;
  background-color: ${({ $color }) => $color};
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
`;

export const Icon = styled.img`
  width: 45px;
  margin-top: 10px;
`;
