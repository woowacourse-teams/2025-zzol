import styled from '@emotion/styled';
import { buttonHoverPress } from '@/styles/animations/buttonHoverPress';
import { TouchState } from '@/types/touchState';

type Props = {
  $touchState: TouchState;
};

export const Container = styled.button<Props>`
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 20px;

  width: 100%;
  height: fit-content;
  border-radius: 12px;
  padding: 16px;
  background-color: ${({ theme }) => theme.color.gray[50]};

  ${({ theme, $touchState }) =>
    buttonHoverPress({
      activeColor: theme.color.gray[200],
      touchState: $touchState,
    })}
`;

export const NextStepIcon = styled.img`
  position: absolute;
  right: 20px;
  top: 50%;
  transform: translateY(-50%);
`;

export const DescriptionBox = styled.div`
  display: flex;
  align-items: flex-start;
`;
