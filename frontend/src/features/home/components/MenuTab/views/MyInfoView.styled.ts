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
  gap: 10px;
  padding: 18px 20px 20px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
`;

export const StatLabel = styled.span`
  font-size: 12px;
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[400]};
  letter-spacing: -0.01em;
`;

export const StatValueRow = styled.div`
  display: flex;
  align-items: baseline;
  gap: 2px;
`;

export const StatNumber = styled.span`
  font-size: 30px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.04em;
  line-height: 1;
`;

export const StatUnit = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
  letter-spacing: -0.02em;
`;

export const InfoSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
`;

export const SectionTitle = styled.h4`
  font-size: 11px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[400]};
  padding-left: 4px;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  margin: 0;
`;

export const TooltipCard = styled.div`
  padding: 16px;
  background: ${({ theme }) => theme.color.gray[50]};
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
  gap: 4px;
`;

export const DangerCard = styled.div`
  display: flex;
  flex-direction: column;
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  overflow: hidden;
  background: ${({ theme }) => theme.color.white};
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  margin-top: 12px;
`;

export const DangerRow = styled.button`
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

  &:active {
    background: ${({ theme }) => theme.color.point[50]};
  }
`;

export const DangerLabel = styled.span`
  font-size: 13px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.point[500]};
`;

export const DangerIcon = styled.span`
  font-size: 16px;
  font-weight: 300;
  color: ${({ theme }) => theme.color.point[200]};
`;
