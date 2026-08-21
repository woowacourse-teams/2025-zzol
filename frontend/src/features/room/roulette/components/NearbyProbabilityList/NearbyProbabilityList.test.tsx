import { ThemeProvider } from '@emotion/react';
import { render, screen, within } from '@testing-library/react';
import { ContextType } from 'react';
import { colorList } from '@/constants/color';
import { IdentifierContext } from '@/contexts/Identifier/IdentifierContext';
import { ProbabilityHistoryContext } from '@/contexts/ProbabilityHistory/ProbabilityHistoryContext';
import { theme } from '@/styles/theme';
import { PlayerProbability } from '@/types/roulette';
import NearbyProbabilityList from './NearbyProbabilityList';

const player = (playerName: string, probability: number): PlayerProbability => ({
  playerName,
  probability,
  playerColor: colorList[0],
});

const CURRENT = [
  player('선두', 30),
  player('추격자', 20),
  player('바로위', 11),
  player('나', 10),
  player('바로아래', 9),
  player('아래아래', 8),
  player('꼴등', 2),
];

const renderList = ({
  current = CURRENT,
  prev = CURRENT,
  myName = '나',
}: { current?: PlayerProbability[]; prev?: PlayerProbability[]; myName?: string } = {}) =>
  render(
    <ThemeProvider theme={theme}>
      <IdentifierContext.Provider value={{ myName } as ContextType<typeof IdentifierContext>}>
        <ProbabilityHistoryContext.Provider
          value={
            { probabilityHistory: { prev, current } } as ContextType<
              typeof ProbabilityHistoryContext
            >
          }
        >
          <NearbyProbabilityList isProbabilitiesLoading={false} />
        </ProbabilityHistoryContext.Provider>
      </IdentifierContext.Provider>
    </ThemeProvider>
  );

describe('NearbyProbabilityList', () => {
  it('나와 확률이 가까운 사람들을 확률 내림차순으로 보여준다', () => {
    renderList();

    const names = screen.getAllByRole('listitem').map((row) => row.textContent);
    expect(names.map((text) => text?.replace(/[\d.,%+-]|증가|감소/g, ''))).toEqual([
      '바로위',
      '나',
      '바로아래',
      '아래아래',
      '꼴등',
    ]);
  });

  it('내 행에만 변화량을 붙인다', () => {
    renderList({ prev: [player('나', 8)] });

    const rows = screen.getAllByRole('listitem');
    const myRow = rows.find((row) => row.textContent?.includes('나'));

    expect(within(myRow!).getByText('+2.00%')).toBeInTheDocument();
    expect(screen.getAllByText(/^[+-]/)).toHaveLength(1);
  });

  it('확률이 줄었으면 음수로 보여준다', () => {
    renderList({ prev: [player('나', 15)] });

    expect(screen.getByText('-5.00%')).toBeInTheDocument();
  });

  it('이전 기록이 없으면 현재 확률만큼 오른 것으로 본다', () => {
    renderList({ prev: [] });

    expect(screen.getByText('+10.00%')).toBeInTheDocument();
  });

  it('확률은 소수점 두 자리로 맞춰 세로 정렬이 흔들리지 않게 한다', () => {
    renderList({ current: [player('나', 10), player('상대', 7.5)] });

    expect(screen.getByText('10.00%')).toBeInTheDocument();
    expect(screen.getByText('7.50%')).toBeInTheDocument();
  });

  it('내가 목록에 없으면 아무것도 그리지 않는다', () => {
    const { container } = renderList({ myName: '없는사람' });

    expect(container).toBeEmptyDOMElement();
  });
});
