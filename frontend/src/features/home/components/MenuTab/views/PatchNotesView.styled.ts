import styled from '@emotion/styled';
import { keyframes } from '@emotion/react';
import { type PatchNoteCategory } from '@/features/home/hooks/usePatchNotes';

/* Layout */
export const Container = styled.div`
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 16px 56px;
`;

export const ListSummary = styled.div`
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 4px 4px 8px;
`;

export const SummaryText = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
  letter-spacing: -0.01em;
`;

/* Note card */
export const NoteCard = styled.div<{ $category: PatchNoteCategory }>`
  background: ${({ theme }) => theme.color.white};
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition:
    transform 0.12s ease,
    box-shadow 0.12s ease;

  &:active {
    transform: scale(0.985);
    box-shadow: 0 1px 6px rgba(0, 0, 0, 0.06);
  }
`;

export const CardInner = styled.div`
  padding: 14px 16px 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
`;

export const CardMeta = styled.div`
  display: flex;
  align-items: center;
  gap: 6px;
`;

export const CategoryChip = styled.span<{ $category: PatchNoteCategory }>`
  display: inline-flex;
  align-items: center;
  gap: 4px;
  ${({ theme }) => theme.typography.caption}
  padding: 3px 8px;
  border-radius: 20px;
  letter-spacing: 0.01em;

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

export const NewBadge = styled.span`
  ${({ theme }) => theme.typography.caption}
  padding: 2px 6px;
  border-radius: 20px;
  background: ${({ theme }) => theme.color.point[400]};
  color: ${({ theme }) => theme.color.white};
  letter-spacing: 0.04em;
`;

export const MetaDate = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.gray[400]};
  margin-left: auto;
  letter-spacing: -0.01em;
`;

export const CardTitle = styled.h3`
  ${({ theme }) => theme.typography.h4}
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.03em;
  line-height: 1.35;
  margin: 0;
`;

export const CardPreview = styled.p`
  ${({ theme }) => theme.typography.small}
  color: ${({ theme }) => theme.color.gray[500]};
  line-height: 1.7;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
`;

export const CardFooter = styled.div`
  display: flex;
  justify-content: flex-end;
  padding-top: 2px;
`;

export const CardReadMore = styled.span`
  ${({ theme }) => theme.typography.caption}
  color: ${({ theme }) => theme.color.point[400]};
  letter-spacing: -0.01em;
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
  ${({ theme }) => theme.typography.paragraph}
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
  font-size: ${({ theme }) => theme.typography.h1.fontSize};
  margin-bottom: 8px;
`;

export const EmptyTitle = styled.h3`
  ${({ theme }) => theme.typography.h2}
  color: ${({ theme }) => theme.color.gray[900]};
  letter-spacing: -0.03em;
  margin: 0;
`;

export const EmptyDesc = styled.p`
  ${({ theme }) => theme.typography.paragraph}
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
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
`;

export const SkeletonInner = styled.div`
  padding: 14px 16px 16px;
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
