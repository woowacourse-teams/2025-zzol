import { HomeIcon, MenuIcon, RankingIcon } from './tabIcons';
import * as S from './HomeBottomTab.styled';

export type HomeTabType = 'home' | 'ranking' | 'menu';

const TABS: { key: HomeTabType; label: string; Icon: typeof HomeIcon }[] = [
  { key: 'home', label: '홈', Icon: HomeIcon },
  { key: 'ranking', label: '랭킹', Icon: RankingIcon },
  { key: 'menu', label: '더보기', Icon: MenuIcon },
];

type Props = {
  activeTab: HomeTabType;
  onTabChange: (tab: HomeTabType) => void;
};

const HomeBottomTab = ({ activeTab, onTabChange }: Props) => (
  <S.TabBar role="tablist">
    {TABS.map(({ key, label, Icon }) => (
      <S.TabButton
        key={key}
        role="tab"
        aria-selected={activeTab === key}
        $active={activeTab === key}
        onClick={() => onTabChange(key)}
      >
        <Icon />
        <S.TabLabel $active={activeTab === key}>{label}</S.TabLabel>
      </S.TabButton>
    ))}
  </S.TabBar>
);

export default HomeBottomTab;
