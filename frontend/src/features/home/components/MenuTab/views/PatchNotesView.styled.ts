import styled from '@emotion/styled';

/* Empty state */
export const EmptyContainer = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  padding: 40px 32px 80px;
  gap: 12px;
`;

export const EmptyIconWrap = styled.div`
  width: 80px;
  height: 80px;
  border-radius: 24px;
  background: ${({ theme }) => theme.color.point[50]};
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36px;
  margin-bottom: 8px;
`;

export const EmptyTitle = styled.h3`
  font-size: 20px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.03em;
  margin: 0;
`;

export const EmptyDesc = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  line-height: 1.6;
  margin: 0;
  white-space: pre-line;
`;

export const VersionPill = styled.span`
  margin-top: 8px;
  padding: 6px 14px;
  border-radius: 20px;
  background: ${({ theme }) => theme.color.gray[100]};
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[500]};
`;

/* Timeline (live) */
export const Container = styled.div`
  display: flex;
  flex-direction: column;
  padding: 24px 20px 40px;
`;

export const LoadingText = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.color.gray[400]};
  text-align: center;
  padding: 20px 0;
  margin: 0;
`;

export const TimelineEntry = styled.div`
  display: flex;
  gap: 16px;
  padding-bottom: 32px;

  &:last-child {
    padding-bottom: 0;

    & > div:first-of-type > div:last-child {
      display: none;
    }
  }
`;

export const TimelineLeft = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 3px;
`;

export const VersionDot = styled.div`
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: ${({ theme }) => theme.color.point[500]};
  flex-shrink: 0;
  box-shadow: 0 0 0 3px ${({ theme }) => theme.color.point[100]};
`;

export const TimelineLine = styled.div`
  width: 2px;
  flex: 1;
  background: ${({ theme }) => theme.color.gray[100]};
  margin-top: 6px;
`;

export const TimelineRight = styled.div`
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding-bottom: 4px;
`;

export const TimelineHeader = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
  height: 16px;
`;

export const VersionTag = styled.span`
  font-size: 15px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.point[500]};
  letter-spacing: -0.02em;
`;

export const VersionDate = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const ChangeList = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 14px 16px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
`;

export const ChangeItem = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 10px;
`;

export const ChangeType = styled.span<{ $type: 'new' | 'fix' | 'improve' }>`
  font-size: 10px;
  font-weight: 700;
  padding: 2px 7px;
  border-radius: 6px;
  flex-shrink: 0;
  margin-top: 2px;
  letter-spacing: 0.03em;

  ${({ theme, $type }) => {
    if ($type === 'new') {
      return `
        background: ${theme.color.point[50]};
        color: ${theme.color.point[500]};
        border: 1px solid ${theme.color.point[100]};
      `;
    }
    if ($type === 'fix') {
      return `
        background: #EFF6FF;
        color: #3B82F6;
        border: 1px solid #BFDBFE;
      `;
    }
    return `
      background: ${theme.color.gray[100]};
      color: ${theme.color.gray[600]};
      border: 1px solid ${theme.color.gray[200]};
    `;
  }}
`;

export const ChangeText = styled.span`
  font-size: 13px;
  color: ${({ theme }) => theme.color.gray[700]};
  line-height: 1.6;
`;
