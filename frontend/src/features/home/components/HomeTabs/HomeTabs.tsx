import * as S from './HomeTabs.styled';

export type HomeTabType = 'game' | 'ranking' | 'menu';

const TABS: { key: HomeTabType; label: string }[] = [
  { key: 'game', label: '게임' },
  { key: 'ranking', label: '랭킹' },
  { key: 'menu', label: '메뉴' },
];

type Props = {
  activeTab: HomeTabType;
  onTabChange: (tab: HomeTabType) => void;
};

const HomeTabs = ({ activeTab, onTabChange }: Props) => {
  return (
    <S.TabBar role="tablist">
      {TABS.map(({ key, label }) => (
        <S.TabButton
          key={key}
          role="tab"
          aria-selected={activeTab === key}
          $active={activeTab === key}
          onClick={() => onTabChange(key)}
        >
          {label}
        </S.TabButton>
      ))}
    </S.TabBar>
  );
};

export default HomeTabs;
