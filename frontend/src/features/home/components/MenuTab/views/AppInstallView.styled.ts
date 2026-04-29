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

export const BenefitCard = styled.div`
  display: flex;
  flex-direction: column;
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 12px;
  overflow: hidden;
  background: ${({ theme }) => theme.color.white};
`;

export const BenefitItem = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[100]};
  }
`;

export const BenefitIcon = styled.span`
  font-size: 18px;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
`;

export const BenefitText = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[700]};
`;

export const InstallButton = styled.button`
  width: 100%;
  padding: 15px;
  border: none;
  border-radius: 12px;
  background: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 700;
  cursor: pointer;
  transition:
    background 0.15s ease,
    transform 0.1s ease;

  &:hover {
    background: ${({ theme }) => theme.color.point[500]};
  }

  &:active {
    transform: scale(0.98);
  }
`;

export const StatusCard = styled.div<{ $status: 'installed' | 'unavailable' }>`
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  border-radius: 12px;
  background: ${({ theme, $status }) =>
    $status === 'installed' ? theme.color.point[50] : theme.color.gray[50]};
  border: 1px solid
    ${({ theme, $status }) =>
      $status === 'installed' ? theme.color.point[100] : theme.color.gray[200]};
`;

export const StatusIcon = styled.span`
  ${({ theme }) => theme.typography.paragraph}
  font-weight: 700;
  color: ${({ theme }) => theme.color.point[400]};
  flex-shrink: 0;
`;

export const StatusText = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[600]};
  line-height: 1.6;
`;
