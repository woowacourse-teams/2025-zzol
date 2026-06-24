import * as S from './NetworkFilterBar.styled';

type Props = {
  contexts: string[];
  selectedContext: string | null;
  selectedType: 'fetch' | 'websocket' | null;
  onContextChange: (context: string | null) => void;
  onTypeChange: (type: 'fetch' | 'websocket' | null) => void;
};

const NetworkFilterBar = ({
  contexts,
  selectedContext,
  selectedType,
  onContextChange,
  onTypeChange,
}: Props) => {
  return (
    <S.FilterBar>
      <S.FilterGroup>
        <S.FilterLabel>Context:</S.FilterLabel>
        <S.FilterButton
          type="button"
          active={selectedContext === null}
          onClick={() => onContextChange(null)}
        >
          All
        </S.FilterButton>
        {contexts.map((context) => {
          const upperContext = context.toUpperCase();
          let displayName = context;

          if (upperContext === 'MAIN') {
            displayName = 'Main';
          } else if (upperContext === 'HOST') {
            displayName = 'Host';
          } else if (upperContext.startsWith('GUEST')) {
            const match = context.match(/^guest(\d+)$/i);
            if (match) {
              displayName = `Guest${match[1]}`;
            } else {
              displayName = context.charAt(0).toUpperCase() + context.slice(1).toLowerCase();
            }
          } else {
            displayName = context.charAt(0).toUpperCase() + context.slice(1).toLowerCase();
          }

          return (
            <S.FilterButton
              key={context}
              type="button"
              active={selectedContext === context}
              onClick={() => onContextChange(context)}
            >
              {displayName}
            </S.FilterButton>
          );
        })}
      </S.FilterGroup>

      <S.FilterGroup>
        <S.FilterLabel>Type:</S.FilterLabel>
        <S.FilterButton
          type="button"
          active={selectedType === null}
          onClick={() => onTypeChange(null)}
        >
          All
        </S.FilterButton>
        <S.FilterButton
          type="button"
          active={selectedType === 'fetch'}
          onClick={() => onTypeChange(selectedType === 'fetch' ? null : 'fetch')}
        >
          Fetch
        </S.FilterButton>
        <S.FilterButton
          type="button"
          active={selectedType === 'websocket'}
          onClick={() => onTypeChange(selectedType === 'websocket' ? null : 'websocket')}
        >
          WebSocket
        </S.FilterButton>
      </S.FilterGroup>
    </S.FilterBar>
  );
};

export default NetworkFilterBar;
