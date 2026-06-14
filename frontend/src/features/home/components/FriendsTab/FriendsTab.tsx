import styled from '@emotion/styled';
import { useState } from 'react';
import { useAuth } from '@/features/auth/hooks/useAuth';
import { useFriends } from '@/features/friends/hooks/useFriends';
import useToast from '@/components/@common/Toast/useToast';
import { useFriendSearch } from '@/features/home/hooks/useFriendSearch';
import { theme } from '@/styles/theme';
import CopyIcon from '@/components/icons/CopyIcon';
import FriendsList from './FriendsList';
import LoginRequiredView from './LoginRequiredView';
import ReceivedRequestsList from './ReceivedRequestsList';
import SearchPanel from './SearchPanel';
import SentRequestsList from './SentRequestsList';

type TabKey = '친구' | '받은 요청' | '보낸 요청';
type SearchMode = '닉네임' | '유저코드';

const FriendsTab = () => {
  const { isAuthenticated, user } = useAuth();
  const { friends, receivedRequests, sentRequests } = useFriends();
  const { showToast } = useToast();
  const {
    searchQuery,
    searchMode,
    searchResults,
    searchLoading,
    isSearching,
    handleSearchChange,
    handleModeChange,
  } = useFriendSearch();

  const [activeTab, setActiveTab] = useState<TabKey>('친구');

  const handleCopyCode = () => {
    if (!user) return;
    navigator.clipboard.writeText(user.userCode);
    showToast({ message: '유저코드가 복사되었습니다', type: 'success' });
  };

  if (!isAuthenticated) {
    return <LoginRequiredView />;
  }

  const tabLabels: TabKey[] = ['친구', '받은 요청', '보낸 요청'];
  const tabCounts: Record<TabKey, number> = {
    친구: friends.length,
    '받은 요청': receivedRequests.length,
    '보낸 요청': sentRequests.length,
  };

  return (
    <S.Container>
      <S.Header>
        <S.Title>친구</S.Title>
      </S.Header>

      {user && (
        <S.MyCodeCard type="button" onClick={handleCopyCode} aria-label="친구 코드 복사">
          <S.MyCodeHeader>
            <S.MyCodeLabel>내 친구 코드</S.MyCodeLabel>
            <S.MyCodeCopyIcon>
              <CopyIcon size={14} />
            </S.MyCodeCopyIcon>
          </S.MyCodeHeader>
          <S.MyCodeBody>
            <S.MyNickname>{user.nickname}</S.MyNickname>
            <S.MyCode>#{user.userCode}</S.MyCode>
          </S.MyCodeBody>
        </S.MyCodeCard>
      )}

      <S.SearchArea>
        <S.ModeRow>
          {(['닉네임', '유저코드'] as SearchMode[]).map((mode) => (
            <S.ModeChip
              key={mode}
              $active={searchMode === mode}
              onClick={() => handleModeChange(mode)}
            >
              {mode}
            </S.ModeChip>
          ))}
        </S.ModeRow>
        <S.SearchInput
          type="text"
          value={searchQuery}
          onChange={handleSearchChange}
          placeholder={searchMode === '닉네임' ? '닉네임으로 검색' : '5자리 유저코드 입력'}
          maxLength={searchMode === '유저코드' ? 5 : 20}
        />
      </S.SearchArea>

      {isSearching ? (
        <SearchPanel results={searchResults} loading={searchLoading} />
      ) : (
        <>
          <S.TabRow>
            {tabLabels.map((tab) => (
              <S.TabChip key={tab} $active={activeTab === tab} onClick={() => setActiveTab(tab)}>
                {tab}
                {tabCounts[tab] > 0 && <S.Badge>{tabCounts[tab]}</S.Badge>}
              </S.TabChip>
            ))}
          </S.TabRow>

          <S.Content>
            {activeTab === '친구' && <FriendsList />}
            {activeTab === '받은 요청' && <ReceivedRequestsList />}
            {activeTab === '보낸 요청' && <SentRequestsList />}
          </S.Content>
        </>
      )}
    </S.Container>
  );
};

export default FriendsTab;

const S = {
  Container: styled.div`
    display: flex;
    flex-direction: column;
    min-height: 100%;
  `,

  Header: styled.div`
    padding: 24px 16px 8px;
  `,

  Title: styled.h2`
    font-size: 22px;
    font-weight: 800;
    color: ${theme.color.gray[900]};
    letter-spacing: -0.02em;
  `,

  MyCodeCard: styled.button`
    margin: 0 16px 4px;
    width: calc(100% - 32px);
    padding: 16px 18px;
    background: ${theme.color.point[50]};
    border: 1.5px solid ${theme.color.point[100]};
    border-radius: 18px;
    display: flex;
    flex-direction: column;
    gap: 8px;
    cursor: pointer;
    text-align: left;
    transition: background 0.12s;

    &:active {
      background: ${theme.color.point[100]};
    }
  `,

  MyCodeHeader: styled.div`
    display: flex;
    align-items: center;
    justify-content: space-between;
  `,

  MyCodeLabel: styled.span`
    font-size: ${theme.typography.caption.fontSize};
    font-weight: ${theme.typography.h4.fontWeight};
    color: ${theme.color.point[300]};
    letter-spacing: 0.02em;
  `,

  MyCodeCopyIcon: styled.span`
    display: flex;
    align-items: center;
    color: ${theme.color.point[300]};

    svg rect:last-of-type {
      fill: ${theme.color.point[50]};
    }
  `,

  MyCodeBody: styled.div`
    display: flex;
    align-items: baseline;
    gap: 8px;
    flex-wrap: wrap;
  `,

  MyNickname: styled.span`
    font-size: ${theme.typography.h2.fontSize};
    font-weight: ${theme.typography.h2.fontWeight};
    color: ${theme.color.gray[900]};
  `,

  MyCode: styled.span`
    font-size: ${theme.typography.paragraph.fontSize};
    font-weight: ${theme.typography.h4.fontWeight};
    color: ${theme.color.gray[500]};
    letter-spacing: 0.08em;
  `,

  SearchArea: styled.div`
    padding: 8px 16px 12px;
    display: flex;
    flex-direction: column;
    gap: 8px;
  `,

  ModeRow: styled.div`
    display: flex;
    gap: 6px;
  `,

  ModeChip: styled.button<{ $active: boolean }>`
    padding: 5px 12px;
    border-radius: 20px;
    border: 1.5px solid
      ${({ $active }) => ($active ? theme.color.point[400] : theme.color.gray[200])};
    background: ${({ $active }) => ($active ? theme.color.point[50] : theme.color.white)};
    color: ${({ $active }) => ($active ? theme.color.point[500] : theme.color.gray[500])};
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.15s;
  `,

  SearchInput: styled.input`
    width: 100%;
    height: 44px;
    padding: 0 16px;
    border: 1.5px solid ${theme.color.gray[200]};
    border-radius: 12px;
    font-size: 15px;
    color: ${theme.color.gray[800]};
    background: ${theme.color.gray[50]};
    box-sizing: border-box;
    outline: none;
    transition: border-color 0.15s;

    &::placeholder {
      color: ${theme.color.gray[300]};
    }

    &:focus {
      border-color: ${theme.color.point[400]};
      background: ${theme.color.white};
    }
  `,

  TabRow: styled.div`
    display: flex;
    padding: 0 16px;
    gap: 6px;
    border-bottom: 1px solid ${theme.color.gray[100]};
    padding-bottom: 12px;
    margin-bottom: 4px;
  `,

  TabChip: styled.button<{ $active: boolean }>`
    display: flex;
    align-items: center;
    gap: 4px;
    padding: 6px 14px;
    border-radius: 20px;
    border: none;
    background: ${({ $active }) => ($active ? theme.color.gray[900] : theme.color.gray[100])};
    color: ${({ $active }) => ($active ? theme.color.white : theme.color.gray[500])};
    font-size: 13px;
    font-weight: 600;
    cursor: pointer;
    transition: background 0.15s;
  `,

  Badge: styled.span`
    background: ${theme.color.point[400]};
    color: ${theme.color.white};
    font-size: 11px;
    font-weight: 700;
    border-radius: 10px;
    padding: 1px 6px;
    min-width: 18px;
    text-align: center;
  `,

  Content: styled.div`
    flex: 1;
    background: ${theme.color.white};
    border-top: 1px solid ${theme.color.gray[100]};
    margin: 0 16px;
    border-radius: 16px 16px 0 0;
    overflow: hidden;
  `,
};
