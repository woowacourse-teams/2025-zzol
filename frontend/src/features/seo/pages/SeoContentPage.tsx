import { Link, useLocation } from 'react-router-dom';
import BackButton from '@/components/@common/BackButton/BackButton';
import NotFoundPage from '@/features/notFound/pages/NotFoundPage';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { findSeoPage, GAME_PAGES } from '@/seo/pages';
import { MINI_GAME_ICON_MAP, MINI_GAME_NAME_MAP } from '@/types/miniGame/common';
import * as S from './SeoContentPage.styled';

/**
 * /guide · /games · /games/:slug 공통 콘텐츠 페이지.
 * 본문은 정적 HTML(webpack)·sitemap과 같은 `src/seo/pages.json`을 읽는다 — 두 벌 관리를 막는다.
 */
const SeoContentPage = () => {
  const navigate = useReplaceNavigate();
  const { pathname } = useLocation();
  const page = findSeoPage(pathname.replace(/\/+$/, '') || '/');

  if (!page) {
    return <NotFoundPage />;
  }

  return (
    <Layout>
      <Layout.TopBar left={<BackButton onClick={() => navigate('/')} />} />
      <Layout.Content>
        <S.Container>
          <S.Header>
            {page.game && <S.Icon src={MINI_GAME_ICON_MAP[page.game]} alt="" />}
            <S.Title>{page.h1}</S.Title>
          </S.Header>

          <S.Body>{page.body}</S.Body>

          {page.path === '/games' && (
            <S.LinkList>
              {GAME_PAGES.map((gamePage) => (
                <S.LinkItem key={gamePage.path}>
                  <Link to={gamePage.path}>
                    <img src={MINI_GAME_ICON_MAP[gamePage.game]} alt="" />
                    <S.LinkTitle>{MINI_GAME_NAME_MAP[gamePage.game]}</S.LinkTitle>
                  </Link>
                </S.LinkItem>
              ))}
            </S.LinkList>
          )}

          {page.game && <S.Cta to="/games">다른 미니게임 보기 &gt;</S.Cta>}
          {page.path !== '/guide' && <S.Cta to="/guide">쫄 이용 가이드 보기 &gt;</S.Cta>}
          <S.Cta to="/">쫄에서 내기 시작하기 &gt;</S.Cta>
        </S.Container>
      </Layout.Content>
    </Layout>
  );
};

export default SeoContentPage;
