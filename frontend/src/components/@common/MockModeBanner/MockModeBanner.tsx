import { useMockMode } from '@/hooks/useMockMode';
import * as S from './MockModeBanner.styled';

const MockModeBanner = () => {
  const { mockEnabled, toggle } = useMockMode();

  if (process.env.NODE_ENV !== 'development') return null;

  return (
    <S.Banner role="status">
      <S.Left>
        <S.Badge $on={mockEnabled}>MOCK {mockEnabled ? 'ON' : 'OFF'}</S.Badge>
        <S.Message>
          {mockEnabled ? '대시보드 목업 데이터 사용 중' : '실제 API 데이터 사용 중'}
        </S.Message>
      </S.Left>
      <S.ToggleButton type="button" onClick={toggle}>
        {mockEnabled ? '끄기' : '켜기'}
      </S.ToggleButton>
    </S.Banner>
  );
};

export default MockModeBanner;
