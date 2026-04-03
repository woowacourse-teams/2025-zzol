import styled from '@emotion/styled';

export const Wrapper = styled.div`
  display: flex;
  gap: 8px;
  width: 100%;
  max-width: 360px;
  margin: 0 auto;
  padding: 8px 12px;
  overflow-x: auto;
  background: rgba(0, 0, 0, 0.35);
  border-radius: 10px;
  backdrop-filter: blur(8px);
  flex-shrink: 0;

  /* 스크롤바 숨김 */
  scrollbar-width: none;
  &::-webkit-scrollbar {
    display: none;
  }
`;

export const Item = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  flex-shrink: 0;
  min-width: 52px;
  padding: 4px 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 8px;
`;

export const Name = styled.span`
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  font-family: 'Pretendard Variable', Pretendard, sans-serif;
  max-width: 52px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

export const Floor = styled.span`
  font-size: 13px;
  font-weight: 700;
  color: #48dbfb;
  font-family: 'Pretendard Variable', Pretendard, sans-serif;
`;
