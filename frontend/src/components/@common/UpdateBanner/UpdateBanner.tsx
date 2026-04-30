import { useServiceWorkerUpdate } from '@/hooks/useServiceWorkerUpdate';
import * as S from './UpdateBanner.styled';

const UpdateBanner = () => {
  const { updateReady, applyUpdate } = useServiceWorkerUpdate();

  if (!updateReady) return null;

  return (
    <S.Banner role="status" aria-live="polite">
      <S.Message>새로운 버전이 준비됐습니다</S.Message>
      <S.UpdateButton type="button" onClick={applyUpdate}>
        업데이트
      </S.UpdateButton>
    </S.Banner>
  );
};

export default UpdateBanner;
