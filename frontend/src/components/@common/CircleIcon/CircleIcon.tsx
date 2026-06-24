import * as S from './CircleIcon.styled';

type Props = {
  color: string;
  imageUrl: string;
  iconAlt?: string;
};

const CircleIcon = ({ color, imageUrl, iconAlt = 'icon' }: Props) => {
  return (
    <S.Container $color={color}>
      <S.Icon src={imageUrl} alt={iconAlt} />
    </S.Container>
  );
};

export default CircleIcon;
