import styled from '@emotion/styled';

export const Container = styled.div`
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 24px 20px 40px;
`;

export const Header = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 40px;
`;

export const Title = styled.h1`
  font-size: 22px;
  font-weight: 700;
  color: ${({ theme }) => theme.color.gray[900]};
  line-height: 1.4;
`;

export const Subtitle = styled.p`
  font-size: 14px;
  color: ${({ theme }) => theme.color.gray[500]};
  line-height: 1.5;
`;

export const TermsList = styled.ul`
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
`;

export const TermsItem = styled.li`
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 16px;
  background: ${({ theme }) => theme.color.white};
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-radius: 12px;
`;

export const Checkbox = styled.input`
  width: 20px;
  height: 20px;
  margin-top: 1px;
  flex-shrink: 0;
  accent-color: ${({ theme }) => theme.color.point[500]};
  cursor: pointer;
`;

export const TermsLabel = styled.label`
  display: flex;
  flex-direction: column;
  gap: 4px;
  cursor: pointer;
`;

export const TermsTitle = styled.span`
  font-size: 14px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.gray[900]};
`;

export const TermsDesc = styled.span`
  font-size: 12px;
  color: ${({ theme }) => theme.color.gray[500]};
  line-height: 1.5;
`;

export const ButtonWrapper = styled.div`
  margin-top: 24px;
`;
