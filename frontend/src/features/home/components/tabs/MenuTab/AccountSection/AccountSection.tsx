import { type KeyboardEvent, useRef, useState } from 'react';
import useModal from '@/components/@common/Modal/useModal';
import useToast from '@/components/@common/Toast/useToast';
import { useAuth } from '@/features/auth/contexts/AuthContext';
import LoginSheet from '@/features/auth/components/LoginSheet/LoginSheet';
import * as S from './AccountSection.styled';

const PROVIDER_LABEL: Record<string, string> = {
  google: 'Google 계정',
  kakao: '카카오 계정',
  naver: '네이버 계정',
};

const MAX_NICKNAME_LENGTH = 10;

const AccountSection = () => {
  const { user, isAuthenticated, logout, updateNickname } = useAuth();
  const { openModal } = useModal();
  const { showToast } = useToast();

  const [isEditing, setIsEditing] = useState(false);
  const [editValue, setEditValue] = useState('');
  const [isSaving, setIsSaving] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleLoginClick = () => {
    openModal(<LoginSheet />, { showCloseButton: false, closeOnBackdropClick: true });
  };

  const handleEditStart = () => {
    setEditValue(user!.nickname);
    setIsEditing(true);
    setTimeout(() => inputRef.current?.focus(), 0);
  };

  const handleEditSave = async () => {
    const trimmed = editValue.trim();
    if (!trimmed) {
      showToast({ message: '닉네임을 입력해주세요.', type: 'error' });
      return;
    }
    if (trimmed === user!.nickname) {
      setIsEditing(false);
      return;
    }
    setIsSaving(true);
    try {
      await updateNickname(trimmed);
      showToast({ message: '닉네임이 변경되었습니다.', type: 'success' });
      setIsEditing(false);
    } catch {
      showToast({ message: '닉네임 변경에 실패했습니다.', type: 'error' });
    } finally {
      setIsSaving(false);
    }
  };

  const handleEditCancel = () => {
    setIsEditing(false);
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter') handleEditSave();
    if (e.key === 'Escape') handleEditCancel();
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
                  <S.EditButton type="button" onClick={handleEditStart} aria-label="닉네임 수정">
                    수정
                  </S.EditButton>
                </S.NicknameRow>
              )}
              <S.Provider>{PROVIDER_LABEL[user.provider] ?? user.provider}</S.Provider>
            </S.UserInfo>
            {!isEditing && (
              <S.LogoutButton type="button" onClick={() => logout()}>
                로그아웃
              </S.LogoutButton>
            )}
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
              <circle cx="12" cy="8" r="3.5" fill="#9CA3AF" />
              <path d="M3.5 20.5C3.5 16.91 7.36 14 12 14s8.5 2.91 8.5 6.5H3.5z" fill="#9CA3AF" />
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
