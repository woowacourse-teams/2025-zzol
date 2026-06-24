import styled from '@emotion/styled';

export const NameWrapper = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;

  h4 {
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    line-height: 2;
  }
`;

export const CrownIcon = styled.img`
  width: 22px;
  height: 22px;
  margin-bottom: 4px;
`;

export const ReadyIcon = styled.img`
  width: 22px;
  height: 22px;
`;
