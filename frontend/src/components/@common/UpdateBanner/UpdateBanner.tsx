import { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { useServiceWorkerUpdate } from '@/hooks/useServiceWorkerUpdate';
import * as S from './UpdateBanner.styled';

const UpdateBanner = () => {
  const { updateReady, applyUpdate } = useServiceWorkerUpdate();
  const [dismissed, setDismissed] = useState(false);
  const { pathname } = useLocation();

  if (!updateReady || dismissed || pathname !== '/') return null;

  return (
    <S.Banner role="status" aria-live="polite">
      <S.Message>새로운 버전이 준비됐습니다</S.Message>
      <S.Actions>
        <S.UpdateButton type="button" onClick={applyUpdate}>
          업데이트
        </S.UpdateButton>
        <S.CloseButton type="button" onClick={() => setDismissed(true)} aria-label="닫기">
          <span aria-hidden="true">✕</span>
        </S.CloseButton>
      </S.Actions>
    </S.Banner>
  );
};

export default UpdateBanner;
