import styled from '@emotion/styled';

export const Container = styled.div`
  display: flex;
  flex-direction: column;
`;

export const QRSection = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.5rem;
  padding: 2rem 0;
`;

export const QRCode = styled.div`
  display: flex;
  justify-content: center;
  align-items: center;
  width: 150px;
  height: 150px;
`;

export const QRPlaceholder = styled.div`
  width: 100%;
  height: 100%;
  background-color: ${({ theme }) => theme.color.gray[700]};
  border-radius: 12px;
`;

export const ShareButton = styled.button`
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.5rem;
  background-color: ${({ theme }) => theme.color.gray[100]};
  border: none;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: background-color 0.2s ease;

  &:hover {
    background-color: ${({ theme }) => theme.color.gray[200]};
  }

  &::before {
    content: '🔗';
    font-size: 1rem;
  }
`;

export const Description = styled.p`
  text-align: center;
  color: ${({ theme }) => theme.color.gray[600]};
  font-size: 0.9rem;
  line-height: 1.4;
  margin: 0;
`;

export const CodeSection = styled.div`
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
  padding: 2rem 0;
`;

export const Wrapper = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 8px;
`;

export const CodeBox = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: ${({ theme }) => theme.color.gray[100]};
  padding: 1rem;
  border-radius: 12px;
`;

export const EmptyBox = styled.div`
  width: 20px;
`;

export const CopyIcon = styled.img`
  width: 20px;
  cursor: pointer;
  transition: opacity 0.2s ease;

  &:hover {
    opacity: 0.7;
  }
`;
