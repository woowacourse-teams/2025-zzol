import { useLocation } from 'react-router-dom';
import BackButton from '@/components/@common/BackButton/BackButton';
import NotFoundPage from '@/features/notFound/pages/NotFoundPage';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { findSeoPage, GAME_PAGES } from '@/seo/pages';
import {
  MINI_GAME_DESCRIPTION_MAP,
  MINI_GAME_ICON_MAP,
  MINI_GAME_NAME_MAP,
  type MiniGameType,
} from '@/types/miniGame/common';
import * as S from './SeoContentPage.styled';

/**
 * 본문을 문장 단위로 끊는다. 문구 자체는 `pages.json` 그대로 두고 배치만 나눈다 —
 * 정적 셸·sitemap 이 읽는 크롤러용 텍스트와 SPA 화면이 어긋나지 않게 한다.
 */
const toSentences = (body: string) =>
  body
    .replace(/\.\s*$/, '')
    .split(/\.\s+/)
    .map((sentence) => `${sentence}.`);

/** 첫 문장은 요약, 나머지는 본문 문단으로 */
const ArticleBody = ({ body }: { body: string }) => {
  const [lead, ...rest] = toSentences(body);

  return (
    <>
      <S.Lead>{lead}</S.Lead>
      {rest.length > 0 && <S.Body>{rest.join(' ')}</S.Body>}
    </>
  );
};

/** 가이드 본문: 첫 문장은 도입, 마지막 문장은 마무리, 가운데 문장들이 진행 단계다 */
const GuideSteps = ({ body }: { body: string }) => {
  const sentences = toSentences(body);

  if (sentences.length < 3) {
    return <S.Lead>{body}</S.Lead>;
  }

  const steps = sentences.slice(1, -1);

  return (
    <>
      <S.Lead>{sentences[0]}</S.Lead>
      <S.StepList>
        {steps.map((step, index) => (
          <S.StepItem key={step} $index={index}>
            <S.StepNumber aria-hidden="true">{index + 1}</S.StepNumber>
            <S.StepText>{step}</S.StepText>
          </S.StepItem>
        ))}
      </S.StepList>
      <S.Body>{sentences[sentences.length - 1]}</S.Body>
    </>
  );
};

const GameGrid = () => (
  <S.GameGrid>
    {GAME_PAGES.map((gamePage, index) => (
      <S.GameGridItem key={gamePage.path} $index={index}>
        <S.GameCard to={gamePage.path}>
          <S.GameIconTile>
            <img src={MINI_GAME_ICON_MAP[gamePage.game]} alt="" aria-hidden="true" />
          </S.GameIconTile>
          <S.GameName>{MINI_GAME_NAME_MAP[gamePage.game]}</S.GameName>
          <S.GameDesc>{MINI_GAME_DESCRIPTION_MAP[gamePage.game][0]}</S.GameDesc>
        </S.GameCard>
      </S.GameGridItem>
    ))}
  </S.GameGrid>
);

const OtherGames = ({ current }: { current: MiniGameType }) => (
  <S.Section>
    <S.SectionTitle>다른 게임 보기</S.SectionTitle>
    <S.ChipRow>
      {GAME_PAGES.filter((gamePage) => gamePage.game !== current).map((gamePage) => (
        <li key={gamePage.path}>
          <S.Chip to={gamePage.path}>
            <img src={MINI_GAME_ICON_MAP[gamePage.game]} alt="" aria-hidden="true" />
            {MINI_GAME_NAME_MAP[gamePage.game]}
          </S.Chip>
        </li>
      ))}
    </S.ChipRow>
  </S.Section>
);

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

  const isGuide = page.path === '/guide';

  return (
    <Layout>
      <Layout.TopBar left={<BackButton onClick={() => navigate('/')} />} />
      <Layout.Content>
        <S.Container>
          <S.Hero>
            {page.game && (
              <S.HeroIcon>
                <img src={MINI_GAME_ICON_MAP[page.game]} alt="" aria-hidden="true" />
              </S.HeroIcon>
            )}
            <S.Title>{page.h1}</S.Title>
          </S.Hero>

          {isGuide ? <GuideSteps body={page.body} /> : <ArticleBody body={page.body} />}

          {page.path === '/games' && <GameGrid />}
          {page.game && <OtherGames current={page.game} />}

          <S.CtaGroup>
            <S.PrimaryCta to="/">쫄에서 내기 시작하기</S.PrimaryCta>
            <S.SecondaryCta to={isGuide ? '/games' : '/guide'}>
              {isGuide ? '미니게임 7종 보기' : '쫄 이용 가이드 보기'}
            </S.SecondaryCta>
          </S.CtaGroup>
        </S.Container>
      </Layout.Content>
    </Layout>
  );
};

export default SeoContentPage;
