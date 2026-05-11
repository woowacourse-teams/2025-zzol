import ZzolLogo from '@/assets/ZZOL.svg';
import ProfileChip from '@/features/auth/components/ProfileChip/ProfileChip';
import * as S from './HomeHeader.styled';

const HomeHeader = () => (
  <S.Header>
    <S.Logo>
      <img src={ZzolLogo} alt="ZZOL" height={18} />
    </S.Logo>
    <ProfileChip />
  </S.Header>
);

export default HomeHeader;
