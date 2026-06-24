import styled from '@emotion/styled';

export const Wrapper = styled.section`
  padding: 16px 0;
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
`;

export const Icon = styled.img`
  width: 56px;
`;

/* 게임 정보 모달 스타일 */
export const InfoContent = styled.div`
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 4px 0 8px;
`;
