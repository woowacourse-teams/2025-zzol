import styled from '@emotion/styled';

type WrapperProps = {
  $flexRatio: number;
};

type ContainerProps = {
  $height: string;
  $gap?: string;
};

export const Container = styled.div<ContainerProps>`
  width: 100%;
  height: ${({ $height }) => $height};
  display: flex;
  gap: ${({ $gap }) => $gap ?? '1.5rem'};
  padding-top: 8px;
`;

export const Wrapper = styled.div<WrapperProps>`
  flex: ${({ $flexRatio }) => $flexRatio};
`;
