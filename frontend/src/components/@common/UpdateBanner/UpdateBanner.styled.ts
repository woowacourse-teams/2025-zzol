import styled from '@emotion/styled';

export const Banner = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background-color: ${({ theme }) => theme.color.gray[900]};
  color: ${({ theme }) => theme.color.white};
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
`;

export const Message = styled.span`
  font-size: ${({ theme }) => theme.typography.paragraph.fontSize};
  font-weight: ${({ theme }) => theme.typography.paragraph.fontWeight};
  line-height: ${({ theme }) => theme.typography.paragraph.lineHeight};
`;

export const UpdateButton = styled.button`
  flex-shrink: 0;
  padding: 6px 14px;
  border: 1.5px solid ${({ theme }) => theme.color.white};
  border-radius: 6px;
  background: transparent;
  color: ${({ theme }) => theme.color.white};
  font-size: ${({ theme }) => theme.typography.small.fontSize};
  font-weight: 600;
  cursor: pointer;

  &:active {
    opacity: 0.8;
  }
`;
