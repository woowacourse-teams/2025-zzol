import { ThemeProvider } from '@emotion/react';
import { render, screen } from '@testing-library/react';
import { theme } from '@/styles/theme';
import RankingNickname from './RankingNickname';

const renderNickname = (masked: boolean) =>
  render(
    <ThemeProvider theme={theme}>
      <RankingNickname masked={masked}>{masked ? '처리 중인 닉네임' : '세라'}</RankingNickname>
    </ThemeProvider>
  );

describe('RankingNickname', () => {
  it('처리 중인 닉네임은 스크린리더에 검열 처리 중이라고 알린다', () => {
    renderNickname(true);

    expect(screen.getByText(', 검열 처리 중')).toBeInTheDocument();
  });

  it('공개된 닉네임은 설명 없이 그대로 그린다', () => {
    renderNickname(false);

    expect(screen.getByText('세라')).toBeInTheDocument();
    expect(screen.queryByText(', 검열 처리 중')).not.toBeInTheDocument();
  });
});
