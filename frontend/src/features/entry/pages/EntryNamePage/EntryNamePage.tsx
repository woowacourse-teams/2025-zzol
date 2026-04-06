import useLazyFetch from '@/apis/rest/useLazyFetch';
import BackButton from '@/components/@common/BackButton/BackButton';
import Button from '@/components/@common/Button/Button';
import RefreshIcon from '@/components/@common/RefreshIcon/RefreshIcon';
import Headline3 from '@/components/@common/Headline3/Headline3';
import Input from '@/components/@common/Input/Input';
import ProgressCounter from '@/components/@common/ProgressCounter/ProgressCounter';
import useToast from '@/components/@common/Toast/useToast';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { useTheme } from '@emotion/react';
import { useRef, useState } from 'react';
import { useRoomManagement } from './hooks/useRoomManagement';
import useRecentNicknames from './hooks/useRecentNicknames';
import * as S from './EntryNamePage.styled';

const MAX_NAME_LENGTH = 10;

type PlayerNameCheckResponse = {
  exist: boolean;
};

type RandomNicknameResponse = {
  nickname: string;
};

const EntryNamePage = () => {
  const [name, setName] = useState('');
  const navigate = useReplaceNavigate();
  const { setMyName, joinCode } = useIdentifier();
  const { playerType } = usePlayerType();
  const { showToast } = useToast();
  const buttonRef = useRef<HTMLButtonElement>(null);
  const { proceedToRoom, isLoading } = useRoomManagement();
  const theme = useTheme();
  const { recentNicknames, addNickname, removeNickname } = useRecentNicknames();

  const checkGuestNameQuery = new URLSearchParams({
    joinCode: joinCode ?? '',
    guestName: name,
  }).toString();

  const { execute: checkGuestName } = useLazyFetch<PlayerNameCheckResponse>({
    endpoint: `/rooms/check-guestName?${checkGuestNameQuery}`,
    errorDisplayMode: 'toast',
  });

  const randomNicknameEndpoint = joinCode
    ? `/rooms/nickname/random?${new URLSearchParams({ joinCode }).toString()}`
    : `/rooms/nickname/random`;

  const { execute: fetchRandomNickname, loading: isRandomLoading } =
    useLazyFetch<RandomNicknameResponse>({
      endpoint: randomNicknameEndpoint,
      errorDisplayMode: 'toast',
    });

  const handleNavigateToHome = () => {
    navigate('/');
  };

  const handleRandomNickname = async () => {
    const response = await fetchRandomNickname();
    if (response?.nickname) {
      const truncatedNickname = response.nickname.slice(0, MAX_NAME_LENGTH);
      setName(truncatedNickname);
    }
  };

  const handleProceedToRoom = async () => {
    if (playerType === 'GUEST') {
      const response = await checkGuestName();
      if (!response) return;
      if (response.exist) {
        showToast({
          type: 'error',
          message: '중복된 닉네임이 존재합니다. 새로운 닉네임을 입력해주세요.',
        });
        return;
      }
    }

    addNickname(name);
    setMyName(name);
    await proceedToRoom(name);
  };

  const isButtonDisabled = name.length === 0;

  return (
    <Layout>
      <Layout.TopBar left={<BackButton onClick={handleNavigateToHome} />} />
      <Layout.Content>
        <S.Container>
          <S.HeadlineRow>
            <Headline3>닉네임을 입력해주세요</Headline3>
            <S.RandomButton
              type="button"
              onClick={handleRandomNickname}
              disabled={isRandomLoading}
              aria-label="닉네임 자동 생성"
              data-testid="random-nickname-button"
            >
              <RefreshIcon size={16} fill={theme.color.point[400]} />
              <span>자동 생성</span>
            </S.RandomButton>
          </S.HeadlineRow>
          <S.Wrapper>
            <Input
              value={name}
              onChange={(e) => setName(e.target.value)}
              onClear={() => setName('')}
              placeholder="닉네임을 입력해주세요"
              maxLength={MAX_NAME_LENGTH}
              autoFocus
              onKeyDown={(e) => {
                if (e.key === 'Enter' && name.length > 0) {
                  buttonRef.current?.focus();
                }
              }}
              data-testid="player-name-input"
            />
            <S.ProgressWrapper $hasRecent={recentNicknames.length > 0}>
              {recentNicknames.length > 0 && (
                <S.RecentNicknamesLabel>최근 사용한 닉네임</S.RecentNicknamesLabel>
              )}
              <ProgressCounter
                current={name.length}
                total={MAX_NAME_LENGTH}
                ariaLabel={`${MAX_NAME_LENGTH}글자 중 ${name.length}글자 입력하였습니다`}
              />
            </S.ProgressWrapper>
            {recentNicknames.length > 0 && (
              <S.NicknameChipList>
                {recentNicknames.map((nickname) => (
                  <S.NicknameChip key={nickname} onClick={() => setName(nickname)}>
                    <S.NicknameChipText>{nickname}</S.NicknameChipText>
                    <S.NicknameChipDeleteButton
                      type="button"
                      aria-label={`${nickname} 삭제`}
                      onClick={(e) => {
                        e.stopPropagation();
                        removeNickname(nickname);
                      }}
                    >
                      ✕
                    </S.NicknameChipDeleteButton>
                  </S.NicknameChip>
                ))}
              </S.NicknameChipList>
            )}
          </S.Wrapper>
        </S.Container>
      </Layout.Content>
      <Layout.ButtonBar>
        <Button
          ref={buttonRef}
          variant={isButtonDisabled ? 'disabled' : 'primary'}
          onClick={handleProceedToRoom}
          isLoading={isLoading}
          data-testid={
            playerType === 'HOST' ? 'create-room-submit-button' : 'join-room-submit-button'
          }
        >
          {playerType === 'HOST' ? '방 만들러 가기' : '방 참가하기'}
        </Button>
      </Layout.ButtonBar>
    </Layout>
  );
};

export default EntryNamePage;
