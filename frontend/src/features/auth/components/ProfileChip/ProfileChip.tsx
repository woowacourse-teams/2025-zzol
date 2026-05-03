import useModal from '@/components/@common/Modal/useModal';
import { useAuth } from '../../contexts/AuthContext';
import LoginSheet from '../LoginSheet/LoginSheet';
import LogoutSheet from '../LogoutSheet/LogoutSheet';
import * as S from './ProfileChip.styled';

const ProfileChip = () => {
  const { user, isAuthenticated } = useAuth();
  const { openModal } = useModal();

  const handleClick = () => {
    if (isAuthenticated) {
      openModal(<LogoutSheet />, { showCloseButton: false, closeOnBackdropClick: true });
    } else {
      openModal(<LoginSheet />, { showCloseButton: false, closeOnBackdropClick: true });
    }
  };

  return (
    <S.Button
      type="button"
      onClick={handleClick}
      aria-label={isAuthenticated ? '내 계정' : '로그인'}
    >
      {isAuthenticated && user ? (
        <S.Avatar>{user.nickname.slice(0, 1)}</S.Avatar>
      ) : (
        <>
          <S.PersonIcon viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
            <circle cx="12" cy="7" r="4" />
          </S.PersonIcon>
          <S.LoginLabel>로그인</S.LoginLabel>
        </>
      )}
    </S.Button>
  );
};

export default ProfileChip;
