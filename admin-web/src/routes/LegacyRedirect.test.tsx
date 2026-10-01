import { render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom';
import { describe, expect, it } from 'vitest';
import { LegacyRedirect } from '@/routes/LegacyRedirect';

/**
 * 옛 주소로 오는 링크는 "이 코드를 찾아라"라는 뜻을 쿼리에 담고 온다. 경로만 넘기면
 * 목록은 열리는데 찾으라던 코드가 사라진다.
 */
function Landed() {
  const location = useLocation();
  return <span data-testid="landed">{location.pathname + location.search + location.hash}</span>;
}

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/trace" element={<LegacyRedirect to="/rooms" />} />
        <Route path="/rooms" element={<Landed />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe('LegacyRedirect', () => {
  it('검색어를 들고 넘어간다', () => {
    renderAt('/trace?q=ABCD');

    expect(screen.getByTestId('landed')).toHaveTextContent('/rooms?q=ABCD');
  });

  it('쿼리가 여럿이어도 그대로 넘긴다', () => {
    renderAt('/trace?q=ABCD&open=room%3A12');

    expect(screen.getByTestId('landed')).toHaveTextContent('/rooms?q=ABCD&open=room%3A12');
  });

  it('해시도 넘긴다', () => {
    renderAt('/trace?q=ABCD#players');

    expect(screen.getByTestId('landed')).toHaveTextContent('/rooms?q=ABCD#players');
  });

  it('쿼리가 없으면 경로만 넘긴다', () => {
    renderAt('/trace');

    expect(screen.getByTestId('landed')).toHaveTextContent('/rooms');
  });
});
