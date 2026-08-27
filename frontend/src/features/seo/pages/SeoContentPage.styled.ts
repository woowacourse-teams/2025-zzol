import styled from '@emotion/styled';
import { Link } from 'react-router-dom';

export const Container = styled.article`
  height: 100%;
  overflow-y: auto;
  padding: 24px 20px 48px;
  display: flex;
  flex-direction: column;
  gap: 24px;
`;

export const Header = styled.header`
  display: flex;
  align-items: center;
  gap: 12px;
`;

export const Icon = styled.img`
  width: 48px;
  height: 48px;
`;

export const Title = styled.h1`
  ${({ theme }) => theme.typography.h2}
  color: ${({ theme }) => theme.color.gray[900]};
`;

export const Body = styled.p`
  ${({ theme }) => theme.typography.paragraph}
  color: ${({ theme }) => theme.color.gray[700]};
  line-height: 1.8;
  white-space: pre-line;
`;

export const LinkList = styled.ul`
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

export const LinkItem = styled.li`
  a {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 14px;
    border: 1px solid ${({ theme }) => theme.color.gray[200]};
    border-radius: 12px;
    color: ${({ theme }) => theme.color.gray[800]};
    text-decoration: none;
  }

  img {
    width: 28px;
    height: 28px;
  }
`;

export const LinkTitle = styled.span`
  ${({ theme }) => theme.typography.paragraph}
`;

export const Cta = styled(Link)`
  ${({ theme }) => theme.typography.paragraph}
  align-self: flex-start;
  color: ${({ theme }) => theme.color.point[500]};
  text-decoration: underline;
`;
