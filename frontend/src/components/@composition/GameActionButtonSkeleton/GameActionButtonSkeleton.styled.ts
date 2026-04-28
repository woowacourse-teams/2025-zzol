import styled from '@emotion/styled';

export const Container = styled.div`
  position: relative;
  display: flex;
  justify-content: space-between;
  background-color: ${({ theme }) => theme.color.gray[50]};
  border-radius: 12px;
  width: 100%;
  height: 130px;
  padding: 20px 18px;
  margin-bottom: 16px;
`;

export const ContentWrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  width: 100%;
`;

export const DescriptionWrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  margin-top: 12px;
  gap: 4px;
  width: 100%;
`;

export const IconWrapper = styled.div`
  position: absolute;
  bottom: 20px;
  right: 18px;
`;
