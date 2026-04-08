import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px;
  gap: 16px;
`;

export const AppHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  background: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[100]};
  border-radius: 16px;
`;

export const AppIcon = styled.div`
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[100]};
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;

  img {
    width: 36px;
    height: 36px;
    object-fit: contain;
  }
`;

export const AppMeta = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
`;

export const AppName = styled.span`
  ${({ theme }) => theme.typography.h3}
  color: ${({ theme }) => theme.color.gray[800]};
`;

export const AppTagline = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const InfoCard = styled.div`
  display: flex;
  flex-direction: column;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 12px;
  overflow: hidden;
  background: ${({ theme }) => theme.color.white};
`;

export const InfoRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 16px;

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[100]};
  }
`;

export const InfoLabel = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const InfoValue = styled.span`
  ${({ theme }) => theme.typography.small}
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const LinkRow = styled.a`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 13px 16px;
  text-decoration: none;
  transition: background 0.15s ease;

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[100]};
  }

  &:hover {
    background: ${({ theme }) => theme.color.point[50]};
  }

  &:active {
    background: ${({ theme }) => theme.color.point[100]};
  }
`;

export const LinkLabel = styled.span`
  ${({ theme }) => theme.typography.small}
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const LinkIcon = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.point[400]};
`;
