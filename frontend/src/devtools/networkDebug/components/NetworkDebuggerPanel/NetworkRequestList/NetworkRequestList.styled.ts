import styled from '@emotion/styled';

export const List = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

export const ListHeader = styled.div`
  display: flex;
  align-items: center;
  padding: 8px 12px;
  background: #f8f9fa;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
  font-weight: 600;
  font-size: 11px;
  color: #666;
  position: sticky;
  top: 0;
  z-index: 1;
`;

export const HeaderCell = styled.div<{ $width?: string; $flex?: number }>`
  padding: 0 8px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  ${({ $width }) => ($width ? `width: ${$width};` : '')}
  ${({ $flex }) => ($flex !== undefined ? `flex: ${$flex};` : '')}
`;

export const ListBody = styled.div`
  flex: 1;
  overflow-y: auto;
  user-select: none;
`;

export const RequestRow = styled.div<{ selected: boolean }>`
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  cursor: pointer;
  background: ${({ selected }) => (selected ? '#e8f0fe' : '#ffffff')};
  transition: background 0.1s ease;
  user-select: none;

  &:hover {
    background: ${({ selected }) => (selected ? '#e8f0fe' : '#f8f9fa')};
  }
`;

export const RequestCell = styled.div<{ $width?: string; $flex?: number }>`
  padding: 0 8px;
  font-size: 12px;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  user-select: none;
  ${({ $width }) => ($width ? `width: ${$width};` : '')}
  ${({ $flex }) => ($flex !== undefined ? `flex: ${$flex};` : '')}
`;

export const TypeBadge = styled.span<{ type: 'fetch' | 'websocket' }>`
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  background: ${({ type }) => (type === 'fetch' ? '#e8f5e9' : '#fff3e0')};
  color: ${({ type }) => (type === 'fetch' ? '#2e7d32' : '#e65100')};
`;

export const ContextBadge = styled.span`
  display: inline-block;
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 600;
  background: #e3f2fd;
  color: #1976d2;
`;

export const UrlText = styled.span`
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 11px;
`;

export const StatusText = styled.span<{ color: string }>`
  color: ${({ color }) => color};
  font-weight: 500;
`;

export const EmptyState = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #999;
`;

export const EmptyText = styled.p`
  margin: 0;
  font-size: 13px;
`;
