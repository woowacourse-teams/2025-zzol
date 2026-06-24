import { buttonHoverPress } from '@/styles/animations/buttonHoverPress';
import { TouchState } from '@/types/touchState';
import styled from '@emotion/styled';

type Props = {
  $touchState: TouchState;
};

export const Container = styled.button<Props>`
  padding: 10px;
  border: none;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background-color: ${({ theme }) => theme.color.gray[100]};
  color: ${({ theme }) => theme.color.gray[400]};
  font-weight: 600;

  ${({ theme, $touchState }) =>
    buttonHoverPress({
      activeColor: theme.color.gray[200],
      touchState: $touchState,
    })};
`;
