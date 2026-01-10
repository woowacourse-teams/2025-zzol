import styled from '@emotion/styled';

export const FilterBar = styled.div`
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  background: #f8f9fa;
  flex-wrap: wrap;
`;

export const FilterGroup = styled.div`
  display: flex;
  align-items: center;
  gap: 4px;
`;

export const FilterLabel = styled.span`
  font-size: 12px;
  color: #666;
  font-weight: 500;
  margin-right: 4px;
`;

export const FilterButton = styled.button<{ active: boolean }>`
  appearance: none;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: ${({ active }) => (active ? '#1a73e8' : '#ffffff')};
  color: ${({ active }) => (active ? '#ffffff' : '#222')};
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 11px;
  cursor: pointer;
  transition: all 0.15s ease;

  &:hover {
    background: ${({ active }) => (active ? '#1557b0' : '#f0f0f0')};
  }
`;
