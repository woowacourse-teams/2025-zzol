import { theme } from '@/styles/theme';
import { ThemeProvider } from '@emotion/react';
import { render, screen } from '@testing-library/react';
import NunchiTierBadge from './NunchiTierBadge';

const renderBadge = (tier: 'SOLO' | 'COLLISION' | 'MISS') =>
  render(
    <ThemeProvider theme={theme}>
      <NunchiTierBadge tier={tier} />
    </ThemeProvider>
  );

describe('NunchiTierBadge', () => {
  it('SOLO 는 "성공" 라벨을 렌더한다', () => {
    renderBadge('SOLO');
    expect(screen.getByText('성공')).toBeInTheDocument();
  });

  it('COLLISION 은 "충돌" 라벨을 렌더한다', () => {
    renderBadge('COLLISION');
    expect(screen.getByText('충돌')).toBeInTheDocument();
  });

  it('MISS 는 "미입력" 라벨을 렌더한다', () => {
    renderBadge('MISS');
    expect(screen.getByText('미입력')).toBeInTheDocument();
  });
});
