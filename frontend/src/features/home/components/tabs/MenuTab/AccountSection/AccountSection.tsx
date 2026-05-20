import { useTheme } from '@emotion/react';
import useModal from '@/components/@common/Modal/useModal';
import useToast from '@/components/@common/Toast/useToast';
import { useAuth } from '@/features/auth/contexts/AuthContext';
import { MAX_NICKNAME_LENGTH, useNicknameEdit } from '@/features/auth/hooks/useNicknameEdit';
import LoginSheet from '@/features/auth/components/LoginSheet/LoginSheet';
import CopyIcon from '@/components/icons/CopyIcon';
import * as S from './AccountSection.styled';

const PROVIDER_LABEL: Record<string, string> = {
  google: 'Google 계정',
  kakao: '카카오 계정',
  naver: '네이버 계정',
};

const AccountSection = () => {
  const theme = useTheme();
  const { user, isAuthenticated, logout } = useAuth();
  const { openModal } = useModal();
  const { showToast } = useToast();
  const {
    isEditing,
    editValue,
    setEditValue,
    isSaving,
    inputRef,
    handleEditStart,
    handleEditSave,
    handleEditCancel,
    handleKeyDown,
  } = useNicknameEdit();

  const handleLoginClick = () => {
    openModal(<LoginSheet />, { showCloseButton: false, closeOnBackdropClick: true });
  };

  if (isAuthenticated && user) {
    return (
      <S.Section>
        <S.Card>
          <S.UserRow>
            <S.Avatar>{user.nickname.slice(0, 1)}</S.Avatar>
            <S.UserInfo>
              {isEditing ? (
                <S.EditRow>
                  <S.NicknameInput
                    ref={inputRef}
                    aria-label="닉네임"
                    value={editValue}
                    onChange={(e) => setEditValue(e.target.value.slice(0, MAX_NICKNAME_LENGTH))}
                    onKeyDown={handleKeyDown}
                    maxLength={MAX_NICKNAME_LENGTH}
                    disabled={isSaving}
                  />
                  <S.EditActions>
                    <S.SaveButton type="button" onClick={handleEditSave} disabled={isSaving}>
                      {isSaving ? '저장 중' : '저장'}
                    </S.SaveButton>
                    <S.CancelButton type="button" onClick={handleEditCancel} disabled={isSaving}>
                      취소
                    </S.CancelButton>
                  </S.EditActions>
                </S.EditRow>
              ) : (
                <S.NicknameRow>
                  <S.Nickname>{user.nickname}</S.Nickname>
                  <S.UserCode
                    type="button"
                    onClick={() => {
                      navigator.clipboard.writeText(user.userCode);
                      showToast({ message: '유저코드가 복사되었습니다', type: 'success' });
                    }}
                    aria-label="유저코드 복사"
                  >
                    #{user.userCode}
                    <CopyIcon size={12} />
                  </S.UserCode>
                </S.NicknameRow>
              )}
              {!isEditing && (
                <S.InfoRow>
                  <S.Provider>{PROVIDER_LABEL[user.provider] ?? user.provider}</S.Provider>
                  <S.EditButton type="button" onClick={handleEditStart} aria-label="닉네임 변경">
                    닉네임 변경
                  </S.EditButton>
                  <S.LogoutButton type="button" onClick={() => logout()}>
                    로그아웃
                  </S.LogoutButton>
                </S.InfoRow>
              )}
            </S.UserInfo>
          </S.UserRow>
        </S.Card>
      </S.Section>
    );
  }

  return (
    <S.Section>
      <S.LoginCard type="button" onClick={handleLoginClick}>
        <S.LoginLeft>
          <S.LoginAvatar>
            <svg viewBox="0 0 24 24" fill="none" width="22" height="22">
              <circle cx="12" cy="8" r="3.5" fill={theme.color.gray[400]} />
              <path
                d="M3.5 20.5C3.5 16.91 7.36 14 12 14s8.5 2.91 8.5 6.5H3.5z"
                fill={theme.color.gray[400]}
              />
            </svg>
          </S.LoginAvatar>
          <S.LoginTextGroup>
            <S.LoginTitle>로그인하기</S.LoginTitle>
            <S.LoginSub>소셜 계정으로 더 많은 기능을 이용해보세요</S.LoginSub>
          </S.LoginTextGroup>
        </S.LoginLeft>
        <S.LoginArrow>→</S.LoginArrow>
      </S.LoginCard>
    </S.Section>
  );
};

export default AccountSection;
