import BreadLogoWhiteIcon from '@/assets/logo/bread-logo-white.png';
import Button from '@/components/@common/Button/Button';
import Headline1 from '@/components/@common/Headline1/Headline1';
import Headline3 from '@/components/@common/Headline3/Headline3';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { useLocation } from 'react-router-dom';
import * as S from './RouletteResultPage.styled';

const RouletteResultPage = () => {
  const navigate = useReplaceNavigate();
  const location = useLocation();
  const winner = location.state?.winner ?? '알 수 없는 사용자';

  const handleClickGoMain = () => {
    navigate('/');
  };

  return (
    <Layout color="point-400">
      <Layout.Content>
        <S.Container>
          <S.Logo src={BreadLogoWhiteIcon} />
          <Headline1 color="white">{winner}</Headline1>
          <Headline3 color="white">님이 당첨되었습니다!</Headline3>
        </S.Container>
      </Layout.Content>
      <Layout.ButtonBar>
        <Button variant="secondary" onClick={handleClickGoMain}>
          메인 화면으로 가기
        </Button>
      </Layout.ButtonBar>
    </Layout>
  );
};

export default RouletteResultPage;
