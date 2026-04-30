import styled from '@emotion/styled';

export const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const Track = styled.div`
  display: flex;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;

  &::-webkit-scrollbar {
    display: none;
  }
`;

export const Card = styled.div`
  flex: 0 0 100%;
  scroll-snap-align: start;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 20px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: background 0.15s ease;

  &:active {
    background: ${({ theme }) => theme.color.gray[50]};
  }
`;

export const Tag = styled.span`
  display: inline-flex;
  align-items: center;
  padding: 3px 8px;
  border-radius: 20px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.03em;
  width: fit-content;
  background: ${({ theme }) => theme.color.point[50]};
  color: ${({ theme }) => theme.color.point[500]};
`;

export const CardTop = styled.div`
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
`;

export const Title = styled.span`
  font-size: 15px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[900]};
  line-height: 1.4;
  letter-spacing: -0.01em;
`;

export const Date = styled.span`
  font-size: 12px;
  font-weight: 400;
  color: ${({ theme }) => theme.color.gray[400]};
  flex-shrink: 0;
  padding-top: 2px;
`;

export const Body = styled.p`
  font-size: 13px;
  font-weight: 400;
  color: ${({ theme }) => theme.color.gray[500]};
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
`;

export const ReadMore = styled.span`
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.point[400]};
  margin-top: 2px;
  align-self: flex-end;
`;

export const Detail = styled.div`
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 4px 0 8px;
`;

export const DetailMeta = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const DetailDate = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[400]};
`;

export const DetailBody = styled.p`
  font-size: 15px;
  font-weight: 400;
  color: ${({ theme }) => theme.color.gray[700]};
  line-height: 1.8;
`;

export const Dots = styled.div`
  display: flex;
  justify-content: center;
  gap: 5px;
`;

export const Dot = styled.span<{ $active: boolean }>`
  width: ${({ $active }) => ($active ? '16px' : '5px')};
  height: 5px;
  border-radius: 3px;
  background: ${({ theme, $active }) => ($active ? theme.color.point[400] : theme.color.gray[200])};
  transition:
    width 0.2s ease,
    background 0.2s ease;
`;
