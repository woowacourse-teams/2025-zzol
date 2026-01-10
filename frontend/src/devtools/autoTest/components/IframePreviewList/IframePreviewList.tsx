import { type MouseEvent } from 'react';
import {
  HOST_IFRAME_NAME,
  PRIMARY_GUEST_IFRAME_NAME,
} from '@/devtools/autoTest/hooks/useIframeRegistry';
import * as S from './IframePreviewList.styled';

type IframeData = {
  iframeNames: string[];
  iframePaths: Record<string, string>;
};

type IframeLayout = {
  iframeHeight: string;
  useMinHeight: boolean;
  canAddMore: boolean;
};

type IframeActions = {
  onAddIframe: () => void;
  onRemoveIframe: (name: string) => void;
  onRegisterIframeRef: (name: string, iframe: HTMLIFrameElement | null) => void;
};

export type IframePreviewListProps = {
  data: IframeData;
  layout: IframeLayout;
  actions: IframeActions;
};

const IframePreviewList = ({ data, layout, actions }: IframePreviewListProps) => {
  const { iframeNames, iframePaths } = data;
  const { iframeHeight, useMinHeight, canAddMore } = layout;
  const { onAddIframe, onRemoveIframe, onRegisterIframeRef } = actions;

  return (
    <S.IframePanel>
      {iframeNames.map((name, index) => {
        const path = iframePaths[name] || '';
        const labelText = path ? `${name} - ${path}` : name;
        const lastIndex = iframeNames.length - 1;
        const isLastIframe = index === lastIndex;
        const canDelete =
          name !== HOST_IFRAME_NAME && name !== PRIMARY_GUEST_IFRAME_NAME && isLastIframe;

        return (
          <S.IframeWrapper key={name} $height={iframeHeight} $useMinHeight={useMinHeight}>
            <S.IframeLabel>{labelText}</S.IframeLabel>
            {canDelete && (
              <S.DeleteButton
                type="button"
                data-delete-button
                onClick={(event: MouseEvent<HTMLButtonElement>) => {
                  event.stopPropagation();
                  onRemoveIframe(name);
                }}
                aria-label={`Remove ${name}`}
              >
                ×
              </S.DeleteButton>
            )}
            <S.PreviewIframe
              ref={(element) => onRegisterIframeRef(name, element)}
              name={name}
              title={`preview-${index === 0 ? 'left' : 'right'}`}
              src="/"
            />
          </S.IframeWrapper>
        );
      })}
      {canAddMore && (
        <S.IframeWrapper $height={iframeHeight} $useMinHeight={useMinHeight}>
          <S.AddIframeButton type="button" onClick={onAddIframe}>
            +
          </S.AddIframeButton>
        </S.IframeWrapper>
      )}
    </S.IframePanel>
  );
};

export default IframePreviewList;
