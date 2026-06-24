import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px 16px 32px;
  gap: 12px;
`;

export const AppHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: linear-gradient(
    135deg,
    ${({ theme }) => theme.color.point[500]} 0%,
    ${({ theme }) => theme.color.point[400]} 100%
  );
  border-radius: 20px;
  box-shadow: 0 4px 16px ${({ theme }) => theme.color.point[300]}44;
`;

export const AppIcon = styled.div`
  width: 52px;
  height: 52px;
  border-radius: 14px;
  background: ${({ theme }) => theme.color.white}38;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;

  img {
    width: 34px;
    height: 34px;
    object-fit: contain;
  }
`;

export const AppMeta = styled.div`
  display: flex;
  flex-direction: column;
  gap: 3px;
`;

export const AppName = styled.span`
  ${({ theme }) => theme.typography.h3}
  color: ${({ theme }) => theme.color.white};
  letter-spacing: -0.02em;
`;

export const AppTagline = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.white}B8;
`;

export const InfoCard = styled.div`
  display: flex;
  flex-direction: column;
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  overflow: hidden;
  background: ${({ theme }) => theme.color.white};
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
`;

export const InfoRow = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};
  }
`;

export const InfoLabel = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const InfoValue = styled.span`
  ${({ theme }) => theme.typography.small}
  font-weight: ${({ theme }) => theme.typography.h4.fontWeight};
  color: ${({ theme }) => theme.color.gray[800]};
`;

export const LinkRow = styled.a`
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 18px;
  text-decoration: none;
  transition: background 0.15s ease;

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};
  }

  &:active {
    background: ${({ theme }) => theme.color.point[50]};
  }
`;

export const LinkLabel = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[800]};
`;

export const LinkIcon = styled.span`
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.gray[300]};
`;

export const InternalLinkRow = styled.button`
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 14px 18px;
  border: none;
  background: transparent;
  cursor: pointer;
  text-align: left;
  transition: background 0.15s ease;

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};
  }

  &:active {
    background: ${({ theme }) => theme.color.point[50]};
  }
`;
