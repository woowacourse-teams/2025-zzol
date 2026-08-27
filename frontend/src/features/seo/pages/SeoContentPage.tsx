import { useLocation } from 'react-router-dom';
import Guide1 from '@/assets/guide1.webp';
import Guide2 from '@/assets/guide2.webp';
import Guide4 from '@/assets/guide4.webp';
import BackButton from '@/components/@common/BackButton/BackButton';
import NotFoundPage from '@/features/notFound/pages/NotFoundPage';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { MINI_GAME_ILLUSTRATION_MAP } from '@/seo/illustrations';
import { findSeoPage, GAME_PAGES } from '@/seo/pages';
import {
  MINI_GAME_DESCRIPTION_MAP,
  MINI_GAME_ICON_MAP,
  MINI_GAME_NAME_MAP,
  type MiniGameType,
} from '@/types/miniGame/common';
import * as S from './SeoContentPage.styled';

/** 인앱 가이드 모달과 같은 실제 화면 캡처 — 스텝 순서(입장 · 미니게임 · 룰렛)에 맞춘다 */
const GUIDE_STEP_IMAGES = [Guide1, Guide2, Guide4];

/**
 * 본문을 문장 단위로 끊는다. 문구 자체는 `pages.json` 그대로 두고 배치만 나눈다 —
 * 정적 셸·sitemap 이 읽는 크롤러용 텍스트와 SPA 화면이 어긋나지 않게 한다.
 */
const toSentences = (body: string) =>
  body
    .replace(/\.\s*$/, '')
    .split(/\.\s+/)
    .map((sentence) => `${sentence}.`);

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
            <S.StepBody>
              <S.StepNumber aria-hidden="true">{index + 1}</S.StepNumber>
              <S.StepText>{step}</S.StepText>
            </S.StepBody>
            {GUIDE_STEP_IMAGES[index] && (
              <S.StepShot src={GUIDE_STEP_IMAGES[index]} alt={`${index + 1}단계 앱 화면`} />
            )}
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
    <S.MiniGrid>
      {GAME_PAGES.filter((gamePage) => gamePage.game !== current).map((gamePage) => (
        <li key={gamePage.path}>
          <S.MiniCard to={gamePage.path}>
            <S.MiniIconTile>
              <img src={MINI_GAME_ICON_MAP[gamePage.game]} alt="" aria-hidden="true" />
            </S.MiniIconTile>
            <S.MiniName>{MINI_GAME_NAME_MAP[gamePage.game]}</S.MiniName>
          </S.MiniCard>
        </li>
      ))}
    </S.MiniGrid>
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
  const illustrations = page.game ? MINI_GAME_ILLUSTRATION_MAP[page.game] : undefined;
  const gameName = page.game ? MINI_GAME_NAME_MAP[page.game] : '';
  const [lead, ...restSentences] = toSentences(page.body);

  return (
    <Layout>
      <Layout.TopBar left={<BackButton onClick={() => navigate('/')} />} />
      <Layout.Content>
        <S.Container>
          <S.Scroll>
            <S.Hero>
              {page.game && !illustrations && (
                <S.HeroIcon>
                  <img src={MINI_GAME_ICON_MAP[page.game]} alt="" aria-hidden="true" />
                </S.HeroIcon>
              )}
              <S.Title>{page.h1}</S.Title>
            </S.Hero>

            {isGuide ? (
              <GuideSteps body={page.body} />
            ) : (
              <>
                <S.Lead>{lead}</S.Lead>
                {illustrations && (
                  <S.Figure>
                    {illustrations.map((src) => (
                      <S.FigureImage key={src} src={src} alt={`${gameName} 설명 그림`} />
                    ))}
                  </S.Figure>
                )}
                {restSentences.length > 0 && <S.Body>{restSentences.join(' ')}</S.Body>}
              </>
            )}

            {page.path === '/games' && <GameGrid />}
            {page.game && <OtherGames current={page.game} />}
          </S.Scroll>

          <S.CtaBar>
            <S.PrimaryCta to="/">쫄에서 내기 시작하기</S.PrimaryCta>
            <S.SecondaryCta to={isGuide ? '/games' : '/guide'}>
              {isGuide ? '미니게임 7종 보기' : '쫄 이용 가이드 보기'}
            </S.SecondaryCta>
          </S.CtaBar>
        </S.Container>
      </Layout.Content>
    </Layout>
  );
};

export default SeoContentPage;
