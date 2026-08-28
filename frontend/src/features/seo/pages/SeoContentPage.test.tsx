import { ThemeProvider } from '@emotion/react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { theme } from '@/styles/theme';
import SeoContentPage from './SeoContentPage';

const renderAt = (pathname: string) =>
  render(
    <ThemeProvider theme={theme}>
      <MemoryRouter initialEntries={[pathname]}>
        <SeoContentPage />
      </MemoryRouter>
    </ThemeProvider>
  );

describe('SeoContentPage', () => {
  it('게임 경로는 해당 게임의 h1 을 렌더한다', () => {
    renderAt('/games/card-game');

    expect(screen.getByText('카드게임 규칙과 내기 활용법')).toBeInTheDocument();
  });

  it('목록 경로는 미니게임 7종 링크를 렌더한다', () => {
    renderAt('/games');

    expect(screen.getByRole('link', { name: /눈치게임/ })).toHaveAttribute(
      'href',
      '/games/nunchi-game'
    );
  });

  it('pages.json 에 없는 slug 는 404 페이지를 렌더한다', () => {
    renderAt('/games/does-not-exist');

    expect(screen.getByText('페이지를 찾을 수 없습니다')).toBeInTheDocument();
  });
});
