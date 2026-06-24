import styled from '@emotion/styled';

type AlignType = 'start' | 'center' | 'end' | 'stretch';
type SectionProps = {
  $align?: AlignType;
};

type ContainerProps = {
  $height: string;
};

export const Container = styled.header<ContainerProps>`
  width: 100%;
  height: ${({ $height }) => $height};
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  flex-shrink: 0;
  padding: 0 16px;
  box-sizing: border-box;
`;

export const LeftSection = styled.div<SectionProps>`
  justify-self: start;
  align-self: ${({ $align }) => $align || 'center'};
`;

export const CenterSection = styled.div<SectionProps>`
  justify-self: center;
  align-self: ${({ $align }) => $align || 'center'};
`;

export const RightSection = styled.div<SectionProps>`
  justify-self: end;
  align-self: ${({ $align }) => $align || 'center'};
`;
