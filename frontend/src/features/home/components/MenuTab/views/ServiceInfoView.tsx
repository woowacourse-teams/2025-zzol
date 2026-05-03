import { useNavigate } from 'react-router-dom';
import { EXTERNAL_LINKS } from '@/constants/external';
import ProfileRedSvg from '@/assets/profile-red.svg';
import * as S from './ServiceInfoView.styled';

const ServiceInfoView = () => {
  const navigate = useNavigate();

  return (
    <S.Container>
      <S.AppHeader>
        <S.AppIcon>
          <img src={ProfileRedSvg} alt="쫄 프로필" />
        </S.AppIcon>
        <S.AppMeta>
          <S.AppName>쫄 (ZZOL)</S.AppName>
          <S.AppTagline>미니게임 기반 당첨자 추첨 서비스</S.AppTagline>
        </S.AppMeta>
      </S.AppHeader>

      <S.InfoCard>
        <S.InfoRow>
          <S.InfoLabel>서비스</S.InfoLabel>
          <S.InfoValue>zzol.site</S.InfoValue>
        </S.InfoRow>
        <S.InfoRow>
          <S.InfoLabel>버전</S.InfoLabel>
          <S.InfoValue>v{process.env.VERSION || '1.0.0'}</S.InfoValue>
        </S.InfoRow>
        <S.InfoRow>
          <S.InfoLabel>개발팀</S.InfoLabel>
          <S.InfoValue>우아한테크코스 7기</S.InfoValue>
        </S.InfoRow>
      </S.InfoCard>

      <S.InfoCard>
        <S.LinkRow href={EXTERNAL_LINKS.GITHUB} target="_blank" rel="noopener noreferrer">
          <S.LinkLabel>GitHub 보기</S.LinkLabel>
          <S.LinkIcon>↗</S.LinkIcon>
        </S.LinkRow>
        <S.InternalLinkRow onClick={() => navigate('/privacy')}>
          <S.LinkLabel>개인정보 처리방침</S.LinkLabel>
          <S.LinkIcon>›</S.LinkIcon>
        </S.InternalLinkRow>
      </S.InfoCard>
    </S.Container>
  );
};

export default ServiceInfoView;
