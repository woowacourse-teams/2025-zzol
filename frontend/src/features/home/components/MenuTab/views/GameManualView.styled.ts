import styled from '@emotion/styled';
import { Link } from 'react-router-dom';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 20px 16px 32px;
`;

export const HintBanner = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: ${({ theme }) => theme.color.point[50]};
  border-radius: 12px;
  border-left: 3px solid ${({ theme }) => theme.color.point[400]};
`;

export const Subtitle = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.point[500]};
  margin: 0;
  line-height: 1.5;
`;

/* 문서로 나가는 링크 — 메뉴 행과 헷갈리지 않게 별도 섹션 + 포인트 톤 카드로 둔다 */

export const MoreSection = styled.section`
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
  padding-top: 16px;
  border-top: 1px solid ${({ theme }) => theme.color.gray[100]};
`;

export const MoreLabel = styled.h3`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
  font-weight: ${({ theme }) => theme.typography.h4.fontWeight};
  letter-spacing: 0.02em;
`;

export const MoreLink = styled(Link)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px;
  background: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[200]};
  border-radius: 14px;
  text-decoration: none;
  transition: background 0.12s ease;

  &:active {
    background: ${({ theme }) => theme.color.point[100]};
  }
`;

export const MoreTexts = styled.span`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const MoreTitle = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.point[500]};
  font-weight: ${({ theme }) => theme.typography.h1.fontWeight};
  letter-spacing: -0.01em;
`;

export const MoreDesc = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[500]};
  line-height: 1.4;
`;

export const MoreArrow = styled.span`
  ${({ theme }) => theme.typography.h4}
  flex-shrink: 0;
  color: ${({ theme }) => theme.color.point[400]};
  line-height: 1;
`;
