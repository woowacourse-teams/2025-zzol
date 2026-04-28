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

export const InfoSlide = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 14px;
  border-radius: 10px;
  background-color: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[100]};
`;

export const InfoStepNumber = styled.span`
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background-color: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  font-size: 13px;
  font-weight: 700;
`;

export const InfoSlideBody = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 100%;
`;

export const InfoSlideText = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[800]};
  line-height: 1.5;
  margin: 0;
  text-align: center;
`;

export const InfoSlideImage = styled.img`
  width: 100%;
  max-height: 140px;
  object-fit: contain;
  border-radius: 8px;
`;

export const InfoSummary = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
  text-align: center;
  padding: 4px 0;
  margin: 0;
`;

export const InfoSlideBody = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  width: 100%;
`;

export const InfoSlideText = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[800]};
  line-height: 1.5;
  margin: 0;
  text-align: center;
`;

export const InfoSlideImage = styled.img`
  width: 100%;
  max-height: 140px;
  object-fit: contain;
  border-radius: 8px;
`;

export const InfoSummary = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
  text-align: center;
  padding: 4px 0;
  margin: 0;
`;


