import { RANKING_CATEGORIES } from './rankingConfigs';

const RECORD_FIELDS = ['maxFloor', 'bestTime', 'bestErrorMillis', 'bestSurvivalMillis'];

const rawRow = (masked: boolean) => ({
  playerName: masked ? '처리 중인 닉네임' : '철수',
  masked,
  ...Object.fromEntries(RECORD_FIELDS.map((field) => [field, 1000])),
});

describe('RANKING_CATEGORIES', () => {
  it.each(RANKING_CATEGORIES.map((category) => [category.key, category] as const))(
    '%s 는 서버가 가린 행을 masked 로 넘긴다',
    (_, category) => {
      const items = category.transformData([rawRow(true), rawRow(false)]);

      expect(items.map((item) => [item.name, item.masked])).toEqual([
        ['처리 중인 닉네임', true],
        ['철수', false],
      ]);
    }
  );
});
