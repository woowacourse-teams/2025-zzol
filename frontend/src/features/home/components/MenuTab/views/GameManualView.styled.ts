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

/** MenuTab 메뉴 행과 같은 카드 행 — 목적지만 앱 밖(/games 문서)일 뿐 생김새는 같게 둔다 */
export const MoreLink = styled(Link)`
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
  padding: 14px 16px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  text-decoration: none;
  transition: background 0.12s ease;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const MoreLeft = styled.span`
  display: flex;
  align-items: center;
  gap: 14px;
`;

export const MoreIcon = styled.span`
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: ${({ theme }) => theme.color.point[50]};
`;

export const MoreTexts = styled.span`
  display: flex;
  flex-direction: column;
  gap: 3px;
`;

export const MoreTitle = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[900]};
  font-weight: ${({ theme }) => theme.typography.h4.fontWeight};
  letter-spacing: -0.01em;
`;

export const MoreDesc = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
  line-height: 1.4;
`;

export const MoreChevron = styled.span`
  ${({ theme }) => theme.typography.h4}
  flex-shrink: 0;
  color: ${({ theme }) => theme.color.gray[300]};
  font-weight: ${({ theme }) => theme.typography.small.fontWeight};
  line-height: 1;
`;
