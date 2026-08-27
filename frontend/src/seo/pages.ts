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
  body: string;
  changefreq: string;
  priority: string;
  game?: MiniGameType;
  noindex?: boolean;
};

export const SEO_PAGES = pages as SeoPage[];

export const findSeoPage = (path: string) => SEO_PAGES.find((page) => page.path === path);

export type GameSeoPage = SeoPage & { game: MiniGameType };

export const GAME_PAGES = SEO_PAGES.filter((page): page is GameSeoPage => page.game !== undefined);
