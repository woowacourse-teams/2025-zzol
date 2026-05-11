import useModal from '@/components/@common/Modal/useModal';
import { useAuth } from '../../contexts/AuthContext';
import * as S from './LogoutSheet.styled';

const LogoutSheet = () => {
  const { user, logout } = useAuth();
  const { closeModal } = useModal();

  const handleLogout = async () => {
    closeModal();
    await logout();
  };

  if (!user) return null;

  return (
    <S.Wrapper>
      <S.UserInfo>
        <S.Avatar>{user.nickname.slice(0, 1)}</S.Avatar>
        <S.NicknameText>{user.nickname}</S.NicknameText>
      </S.UserInfo>
      <S.LogoutButton type="button" onClick={handleLogout}>
        로그아웃
      </S.LogoutButton>
    </S.Wrapper>
  );
};

export default LogoutSheet;
