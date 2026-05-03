import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';
import { type PatchNoteCategory } from '@/features/home/hooks/usePatchNotes';

/* Layout */
export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px 16px 48px;
`;

export const ListHeader = styled.p`
  font-size: 12px;
  font-weight: 500;
  color: ${({ theme }) => theme.color.gray[400]};
  margin: 0 0 4px;
  letter-spacing: -0.01em;
`;

/* Note card */
export const NoteCard = styled.div<{ $category: PatchNoteCategory }>`
  background: ${({ theme }) => theme.color.white};
  border-radius: 16px;
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-left: 3px solid
    ${({ theme, $category }) => {
      if ($category === 'UPDATE') return theme.color.point[400];
      if ($category === 'NOTICE') return theme.color.blue;
      if ($category === 'EVENT') return theme.color.yellow;
      return theme.color.gray[300];
    }};
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition:
    background-color 0.15s ease,
    transform 0.12s ease;

  &:active {
    background-color: ${({ theme }) => theme.color.gray[50]};
    transform: scale(0.99);
  }
`;

export const CardMeta = styled.div`
  display: flex;
  align-items: center;
  gap: 8px;
`;

export const CategoryChip = styled.span<{ $category: PatchNoteCategory }>`
  font-size: 11px;
  font-weight: 700;
  padding: 2px 8px;
  border-radius: 20px;
  letter-spacing: 0.02em;

  ${({ theme, $category }) => {
    if ($category === 'UPDATE')
      return `background: ${theme.color.point[50]}; color: ${theme.color.point[500]};`;
    if ($category === 'NOTICE')
      return `background: ${theme.color.gray[100]}; color: ${theme.color.blue};`;
    if ($category === 'EVENT')
      return `background: ${theme.color.gray[100]}; color: ${theme.color.gray[700]};`;
    return `background: ${theme.color.gray[100]}; color: ${theme.color.gray[500]};`;
  }}
`;

export const MetaDate = styled.span`
  font-size: 11px;
  color: ${({ theme }) => theme.color.gray[400]};
  margin-left: auto;
`;

export const CardTitle = styled.h3`
  font-size: 16px;
  font-weight: 800;
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.03em;
  line-height: 1.35;
  margin: 0;
`;

export const CardPreview = styled.p`
  font-size: 13px;
  color: ${({ theme }) => theme.color.gray[600]};
  line-height: 1.75;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
`;

export const CardReadMore = styled.span`
  font-size: 12px;
  font-weight: 600;
  color: ${({ theme }) => theme.color.point[400]};
  align-self: flex-end;
`;

/* Modal detail */
export const DetailContainer = styled.div`
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

export const DetailBody = styled.p`
  font-size: 15px;
  color: ${({ theme }) => theme.color.gray[700]};
  line-height: 1.8;
  margin: 0;
  white-space: pre-wrap;
`;

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

/* Skeleton */
const shimmer = keyframes`
  0% { background-position: -200% 0; }
  100% { background-position: 200% 0; }
`;

export const SkeletonCard = styled.div`
  background: ${({ theme }) => theme.color.white};
  border-radius: 16px;
  border: 1px solid ${({ theme }) => theme.color.gray[100]};
  border-left: 3px solid ${({ theme }) => theme.color.gray[200]};
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const SkeletonLine = styled.div<{ $width?: string; $height?: string }>`
  height: ${({ $height }) => $height ?? '14px'};
  width: ${({ $width }) => $width ?? '100%'};
  border-radius: 6px;
  background: linear-gradient(
    90deg,
    ${({ theme }) => theme.color.gray[100]} 25%,
    ${({ theme }) => theme.color.gray[50]} 50%,
    ${({ theme }) => theme.color.gray[100]} 75%
  );
  background-size: 200% 100%;
  animation: ${shimmer} 1.4s infinite linear;
`;
