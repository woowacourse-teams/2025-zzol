import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  padding: 16px 0;
`;

export const RoundSection = styled.div`
  flex-shrink: 0;
`;

export const WordSection = styled.div`
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  padding: 16px 0;
`;

export const FeedbackSection = styled.div`
  flex-shrink: 0;
  min-height: 36px;
`;

export const PlayerSection = styled.div`
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const InputSection = styled.div`
  flex-shrink: 0;
  padding: 0 16px;
`;
