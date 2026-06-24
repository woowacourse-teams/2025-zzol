import Headline2 from '@/components/@common/Headline2/Headline2';
import Paragraph from '@/components/@common/Paragraph/Paragraph';
import Layout from '@/layouts/Layout';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import * as S from './NotFoundPage.styled';

const NotFoundPage = () => {
  const navigate = useReplaceNavigate();

  const handleClickHomeButton = () => {
    navigate('/');
  };

  return (
    <Layout color="point-400">
      <Layout.Content>
        <S.Container>
          <S.NotFoundText>404</S.NotFoundText>
          <Headline2 color="white">페이지를 찾을 수 없습니다</Headline2>
          <S.ButtonContainer onClick={handleClickHomeButton}>
            <Paragraph color="white">홈으로 돌아가기 &gt;</Paragraph>
          </S.ButtonContainer>
        </S.Container>
      </Layout.Content>
    </Layout>
  );
};

export default NotFoundPage;
