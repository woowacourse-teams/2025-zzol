import { MINI_GAME_NAME_MAP } from '@/types/miniGame/common';
import { GAME_PAGES, SEO_PAGES } from './pages';

describe('SEO 페이지 메타', () => {
  it('path 는 중복이 없고 / 로 시작한다', () => {
    const paths = SEO_PAGES.map((page) => page.path);

    expect(new Set(paths).size).toBe(paths.length);
    paths.forEach((path) => expect(path.startsWith('/')).toBe(true));
  });

  it('title 은 검색결과에서 잘리지 않도록 60자 이하다', () => {
    const tooLong = SEO_PAGES.filter((page) => page.title.length > 60).map((page) => page.path);

    expect(tooLong).toEqual([]);
  });

  it('description 은 50자 이상 160자 이하다 — 구글 스니펫이 160자 부근에서 잘린다', () => {
    SEO_PAGES.forEach((page) => {
      expect(page.description.length).toBeGreaterThanOrEqual(50);
    });

    const tooLong = SEO_PAGES.filter((page) => page.description.length > 160).map(
      (page) => page.path
    );

    expect(tooLong).toEqual([]);
  });

  it('게임 페이지 body 는 "내용 없는 페이지"로 색인 제외되지 않도록 200자 이상이다', () => {
    GAME_PAGES.forEach((page) => {
      expect(page.body.length).toBeGreaterThanOrEqual(200);
    });
  });

  it('게임 페이지는 MINI_GAME_NAME_MAP 과 정확히 일치한다', () => {
    expect(GAME_PAGES.map((page) => page.game).sort()).toEqual(
      Object.keys(MINI_GAME_NAME_MAP).sort()
    );
  });
});
