import styled from '@emotion/styled';

export const DetailContainer = styled.div`
  padding: 16px;
  height: 100%;
  overflow-y: auto;
  position: relative;
  user-select: none;
`;

export const CloseButton = styled.button`
  position: absolute;
  top: 10px;
  right: 12px;
  appearance: none;
  border: none;
  background: transparent;
  color: #666;
  padding: 4px;
  cursor: pointer;
  font-size: 16px;
  line-height: 1;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  z-index: 10;

  &:hover {
    background: rgba(0, 0, 0, 0.05);
  }
`;

export const Section = styled.div`
  margin-bottom: 24px;

  &:last-child {
    margin-bottom: 0;
  }
`;

export const SectionTitle = styled.h4`
  margin: 0 0 12px 0;
  font-size: 13px;
  font-weight: 600;
  color: #222;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.1);
`;

export const DetailGrid = styled.div`
  display: flex;
  flex-direction: column;
  gap: 8px;
`;

export const DetailRow = styled.div`
  display: flex;
  gap: 12px;
`;

export const DetailLabel = styled.span`
  font-weight: 600;
  color: #666;
  min-width: 100px;
  font-size: 12px;
`;

export const DetailValue = styled.span`
  color: #222;
  font-size: 12px;
  flex: 1;
  word-break: break-all;
  user-select: none;
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
  word-break: break-all;
  user-select: none;
`;

export const CodeBlock = styled.div`
  background: #f5f5f5;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  padding: 12px;
  overflow-x: auto;
  user-select: none;

  pre {
    margin: 0;
    font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
    font-size: 11px;
    line-height: 1.5;
    color: #222;
    white-space: pre-wrap;
    word-wrap: break-word;
    user-select: none;
  }
`;

export const ErrorBlock = styled.div`
  background: #ffebee;
  border: 1px solid #ffcdd2;
  border-radius: 4px;
  padding: 12px;
  overflow-x: auto;
  user-select: none;

  pre {
    margin: 0;
    font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
    font-size: 11px;
    line-height: 1.5;
    color: #c62828;
    white-space: pre-wrap;
    word-wrap: break-word;
    user-select: none;
  }
`;

export const MessageRow = styled.div<{ isExpanded: boolean }>`
  display: flex;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  cursor: pointer;
  background: ${({ isExpanded }) => (isExpanded ? '#e8f0fe' : '#ffffff')};
  transition: background 0.1s ease;

  &:hover {
    background: ${({ isExpanded }) => (isExpanded ? '#e8f0fe' : '#f8f9fa')};
  }
`;

export const MessageArrow = styled.span<{ type: 'sent' | 'received' }>`
  display: inline-block;
  margin-right: 8px;
  font-size: 12px;
  color: ${({ type }) => (type === 'sent' ? '#0f9d58' : '#d93025')};
  font-weight: 600;
`;

export const MessageSummary = styled.span`
  flex: 1;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  font-size: 11px;
  color: #222;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  user-select: none;
`;

export const MessageTime = styled.span`
  font-size: 11px;
  color: #666;
  margin-left: 8px;
  font-family: 'Monaco', 'Menlo', 'Consolas', monospace;
  user-select: none;
`;

export const ExpandedMessageDetail = styled.div`
  padding: 16px;
  background: #f8f9fa;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
  user-select: none;
`;

export const PayloadSectionTitle = styled(SectionTitle)`
  margin: 16px 0 12px 0;
  font-size: 13px;
  font-weight: 600;
  color: #222;
  border: none;
  padding: 0;
`;

export const DetailRowWithMargin = styled(DetailRow)<{ $marginBottom?: string }>`
  margin-bottom: ${({ $marginBottom }) => $marginBottom || '0'};
`;

export const DetailLabelStyled = styled(DetailLabel)`
  font-size: 12px;
  color: #666;
  min-width: 140px;
  font-weight: 600;
`;

export const DetailValueStyled = styled(DetailValue)<{ $color?: string }>`
  font-size: 12px;
  color: ${({ $color }) => $color || '#222'};
  font-weight: 600;
`;

export const CodeBlockWithMargin = styled(CodeBlock)`
  margin-top: 4px;
`;

export const ErrorMessageWrapper = styled.div`
  margin-top: 12px;
`;
