import useModal from '@/components/@common/Modal/useModal';
import { useAuth } from '../../contexts/AuthContext';
import { MAX_NICKNAME_LENGTH, useNicknameEdit } from '../../hooks/useNicknameEdit';
import * as S from './ProfileSheet.styled';

const ProfileSheet = () => {
  const { user, logout } = useAuth();
  const { closeModal } = useModal();
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

  const handleLogout = async () => {
    closeModal();
    await logout();
  };

  if (!user) return null;

  return (
    <S.Wrapper>
      <S.UserInfo>
        <S.Avatar>{user.nickname.slice(0, 1)}</S.Avatar>
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
            <S.NicknameText>{user.nickname}</S.NicknameText>
            <S.EditButton type="button" onClick={handleEditStart} aria-label="닉네임 변경">
              변경
            </S.EditButton>
          </S.NicknameRow>
        )}
      </S.UserInfo>
      <S.LogoutButton type="button" onClick={handleLogout}>
        로그아웃
      </S.LogoutButton>
    </S.Wrapper>
  );
};

export default ProfileSheet;
