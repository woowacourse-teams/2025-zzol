import styled from '@emotion/styled';
import { theme } from '@/styles/theme';

type WrapperProps = {
  $gap: number;
};

type ContainerProps = {
  $showBorder: boolean;
};

export const Container = styled.div<ContainerProps>`
  display: flex;
  align-items: center;
  width: 100%;
  padding: 15px 0;
  justify-content: space-between;
  ${({ $showBorder }) => $showBorder && `border-bottom: 1px solid ${theme.color.gray[200]};`}
`;

export const Wrapper = styled.div<WrapperProps>`
  display: flex;
  align-items: center;
  gap: ${({ $gap }) => $gap}px;
  flex: 1;
  min-width: 0;
`;

export const IconWrapper = styled.div`
  flex-shrink: 0;
`;

export const TextWrapper = styled.div`
  flex: 1;
  min-width: 0;
  padding-right: 20px;

  div {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
`;
