import styled from '@emotion/styled';
import { Z_INDEX } from '@/constants/zIndex';

type ThumbProps = {
  index: number;
  optionCount: number;
};

export const Container = styled.div`
  width: 100%;
  height: 42px;
  background-color: ${({ theme }) => theme.color.gray[100]};
  padding: 4px;
  border-radius: 20px;
  cursor: pointer;
  box-shadow: 0 3px 3px rgba(0, 0, 0, 0.15);
`;

export const Track = styled.div`
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
`;

export const Option = styled.button`
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 1;
  height: 100%;
  z-index: ${Z_INDEX.TOGGLE_BUTTON_OPTION};
  cursor: pointer;
`;

export const Thumb = styled.div<ThumbProps>`
  position: absolute;
  width: ${({ optionCount }) => `${100 / optionCount}%`};
  height: 100%;
  background-color: ${({ theme }) => theme.color.point[400]};
  border-radius: 20px;
  left: ${({ index, optionCount }) => `${(100 / optionCount) * index}%`};
  transition: left 0.2s;
  z-index: ${Z_INDEX.TOGGLE_BUTTON_THUMB};
`;
