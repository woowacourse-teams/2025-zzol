import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 20px;
  gap: 16px;
`;

export const ProfileHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: ${({ theme }) => theme.color.point[50]};
  border: 1px solid ${({ theme }) => theme.color.point[100]};
  border-radius: 20px;
`;

export const ProfileIcon = styled.div`
  width: 60px;
  height: 60px;
  border-radius: 18px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.point[200]};
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  flex-shrink: 0;
  box-shadow: ${({ theme }) => `0 4px 12px ${theme.color.point[300]}1A`};
`;

export const ProfileInfo = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const WelcomeMessage = styled.span`
  ${({ theme }) => theme.typography.h3}
  color: ${({ theme }) => theme.color.gray[800]};
  font-weight: 700;
`;

export const UserStatus = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
`;

export const StatGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
`;

export const StatCard = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 20px 10px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[200]};
  border-radius: 16px;
  transition: transform 0.2s ease;

  &:active {
    transform: scale(0.98);
  }
`;

export const StatLabel = styled.span`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
  font-weight: 500;
`;

export const StatValue = styled.span`
  ${({ theme }) => theme.typography.h2}
  color: ${({ theme }) => theme.color.point[400]};
  font-weight: 800;
`;

export const InfoSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 8px;
`;

export const SectionTitle = styled.h4`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[600]};
  font-weight: 600;
  padding-left: 4px;
`;

export const TooltipCard = styled.div`
  padding: 14px;
  background: ${({ theme }) => theme.color.gray[50]};
  border-radius: 12px;
  border: 1px dashed ${({ theme }) => theme.color.gray[300]};
`;

export const TooltipList = styled.ul`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
  line-height: 1.5;
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const ComingSoonText = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.point[400]};
  font-weight: 500;
  margin: 10px 0 0;
  padding-top: 10px;
  border-top: 1px solid ${({ theme }) => theme.color.gray[200]};
`;
