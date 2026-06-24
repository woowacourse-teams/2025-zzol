import styled from '@emotion/styled';

export const PageContainer = styled.div`
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100dvh;
  max-width: 430px;
  margin: 0 auto;
  background: ${({ theme }) => theme.color.gray[50]};
  overflow: hidden;
`;

export const ScrollArea = styled.main`
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  scroll-behavior: smooth;
`;

export const VisuallyHidden = styled.div`
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
`;
