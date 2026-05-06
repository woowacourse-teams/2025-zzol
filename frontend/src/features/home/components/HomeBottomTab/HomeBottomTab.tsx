import { FriendsIcon, HomeIcon, MenuIcon, RankingIcon } from './tabIcons';
import * as S from './HomeBottomTab.styled';

export type HomeTabType = 'home' | 'ranking' | 'friends' | 'menu';

const TABS: { key: HomeTabType; label: string; Icon: typeof HomeIcon }[] = [
  { key: 'home', label: '홈', Icon: HomeIcon },
  { key: 'ranking', label: '랭킹', Icon: RankingIcon },
  { key: 'friends', label: '친구', Icon: FriendsIcon },
  { key: 'menu', label: '더보기', Icon: MenuIcon },
];

type Props = {
  activeTab: HomeTabType;
  onTabChange: (tab: HomeTabType) => void;
  friendsBadgeCount?: number;
};

const HomeBottomTab = ({ activeTab, onTabChange, friendsBadgeCount = 0 }: Props) => (
  <S.TabBar role="tablist">
    {TABS.map(({ key, label, Icon }) => (
      <S.TabButton
        key={key}
        role="tab"
        aria-selected={activeTab === key}
        $active={activeTab === key}
        onClick={() => onTabChange(key)}
      >
        <S.IconWrap>
          <Icon />
          {key === 'friends' && friendsBadgeCount > 0 && <S.TabBadge />}
        </S.IconWrap>
        <S.TabLabel $active={activeTab === key}>{label}</S.TabLabel>
      </S.TabButton>
    ))}
  </S.TabBar>
);

export default HomeBottomTab;
