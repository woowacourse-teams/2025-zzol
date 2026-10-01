import type { MiniGameType } from '@/types/miniGame/common';
import pages from './pages.json';

/**
 * 라우트별 정적 HTML(webpack.common.js)·sitemap·SPA 콘텐츠 페이지가 공유하는 단일 소스.
 * 빌드 설정(webpack)에서도 읽으므로 `.json`으로 둔다 — TS 모듈이면 webpack이 못 읽는다.
 */
export type SeoPage = {
  path: string;
  title: string;
  description: string;
  h1: string;
  /** 정적 셸에는 HTML 로, SPA 에는 텍스트로 들어간다 — 홈만 `<br />` 를 쓴다 */
  body: string;
  changefreq: string;
  priority: string;
  game?: MiniGameType;
  noindex?: boolean;
  /** 홈만 직접 정의한다. 나머지는 webpack 이 WebPage 스키마를 만들어 넣는다 */
  jsonLd?: Record<string, unknown>;
};

export const SEO_PAGES = pages as SeoPage[];

export const findSeoPage = (path: string) => SEO_PAGES.find((page) => page.path === path);

type GameSeoPage = SeoPage & { game: MiniGameType };

export const GAME_PAGES = SEO_PAGES.filter((page): page is GameSeoPage => page.game !== undefined);
