import styled from '@emotion/styled';

export const Container = styled.div`
  position: relative;
`;

export const Wrapper = styled.div`
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background-color: ${({ theme }) => theme.color.point[100]};
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
`;

export const Pin = styled.div`
  width: 0;
  height: 0;
  border-left: 12px solid transparent;
  border-right: 12px solid transparent;
  border-top: 30px solid ${({ theme }) => theme.color.gray[500]};
  border-radius: 4px;
  position: absolute;
  top: -5px;
  left: 50%;
  transform: translateX(-50%);
  z-index: -1;
`;

export const BreadCharacter = styled.img`
  width: 200px;
  height: auto;
  position: absolute;
  top: 50%;
  left: 52%;
  transform: translate(-50%, -50%);
`;
