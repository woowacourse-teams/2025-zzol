import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px 16px 32px;
  gap: 12px;
`;

export const ProfileHeader = styled.div`
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

export const ProfileInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const WelcomeMessage = styled.span`
  font-size: 17px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.white};
  letter-spacing: -0.02em;
`;

export const UserStatus = styled.span`
  font-size: 12px;
  font-weight: 400;
  color: rgba(255, 255, 255, 0.72);
`;

export const StatGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
`;

export const StatCard = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 20px 12px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
`;

export const StatLabel = styled.span`
  font-size: 11px;
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
`;

export const StatValue = styled.span`
  font-size: 28px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.point[500]};
  letter-spacing: -0.03em;
  line-height: 1;
`;

export const InfoSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
`;

export const SectionTitle = styled.h4`
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[400]};
  padding-left: 4px;
  letter-spacing: 0.01em;
`;

export const TooltipCard = styled.div`
  padding: 16px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 14px;
`;

export const TooltipList = styled.ul`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[500]};
  line-height: 1.7;
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
`;

export const ComingSoonText = styled.p`
  font-size: 12px;
  font-weight: 500;
  color: ${({ theme }) => theme.color.point[400]};
  margin: 10px 0 0;
  padding-top: 10px;
  border-top: 1px solid ${({ theme }) => theme.color.gray[100]};
`;
