import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px 16px 40px;
  gap: 10px;
`;

export const HeroCard = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 28px 20px 24px;
  background: linear-gradient(
    135deg,
    ${({ theme }) => theme.color.point[500]} 0%,
    ${({ theme }) => theme.color.point[400]} 100%
  );
  border-radius: 20px;
  box-shadow: 0 4px 16px ${({ theme }) => theme.color.point[300]}44;
  margin-bottom: 6px;
`;

export const HeroIconWrap = styled.div`
  width: 60px;
  height: 60px;
  border-radius: 18px;
  background: ${({ theme }) => theme.color.white}33;
  display: flex;
  align-items: center;
  justify-content: center;
`;

export const HeroTitle = styled.span`
  ${({ theme }) => theme.typography.h3}
  color: ${({ theme }) => theme.color.white};
  letter-spacing: -0.02em;
`;

export const HeroSub = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.white}B8;
  text-align: center;
`;

export const SectionLabel = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
  text-transform: uppercase;
  letter-spacing: 0.06em;
  padding: 0 4px;
  margin-top: 4px;
`;

export const Card = styled.div`
  display: flex;
  flex-direction: column;
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  overflow: hidden;
  background: ${({ theme }) => theme.color.white};
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
`;

export const BenefitRow = styled.div`
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};
  }
`;

export const BenefitEmoji = styled.span`
  font-size: ${({ theme }) => theme.typography.h2.fontSize};
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: ${({ theme }) => theme.color.gray[50]};
  border-radius: 10px;
  flex-shrink: 0;
`;

export const BenefitInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
`;

export const BenefitTitle = styled.span`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.gray[800]};
  letter-spacing: -0.01em;
`;

export const BenefitDesc = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const InstallButton = styled.button`
  width: 100%;
  padding: 17px;
  margin-top: 4px;
  border: none;
  border-radius: 14px;
  background: ${({ theme }) => theme.color.point[500]};
  color: ${({ theme }) => theme.color.white};
  ${({ theme }) => theme.typography.h4}
  letter-spacing: -0.01em;
  cursor: pointer;
  transition:
    background 0.15s ease,
    transform 0.1s ease;

  &:active {
    background: ${({ theme }) => theme.color.point[500]};
    transform: scale(0.98);
  }
`;

export const StatusBanner = styled.div<{ $type: 'installed' }>`
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 16px 18px;
  margin-top: 4px;
  border-radius: 14px;
  background: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[100]};
`;

export const StatusEmoji = styled.span`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.point[500]};
  flex-shrink: 0;
`;

export const StatusText = styled.span`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.point[500]};
`;

export const GuideRow = styled.div`
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;

  &:not(:last-child) {
    border-bottom: 1px solid ${({ theme }) => theme.color.gray[50]};
  }
`;

export const GuideBadge = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.point[500]};
  background: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[100]};
  border-radius: 6px;
  padding: 3px 8px;
  flex-shrink: 0;
  min-width: 36px;
  text-align: center;
`;

export const GuideDesc = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[600]};
`;
