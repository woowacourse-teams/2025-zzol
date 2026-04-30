import ProfileChip from '@/features/auth/components/ProfileChip/ProfileChip';
import * as S from './HomeHeader.styled';

const HomeHeader = () => (
  <S.Header>
    <S.Logo>
      <S.LogoText>ZZOL</S.LogoText>
    </S.Logo>
    <ProfileChip />
  </S.Header>
);

export default HomeHeader;
