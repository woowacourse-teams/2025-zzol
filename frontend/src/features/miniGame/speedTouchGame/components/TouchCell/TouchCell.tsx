import CoffeeWhite from '@/assets/coffee-white.svg';
import * as S from './TouchCell.styled';

type Props = {
  number: number;
  touched: boolean;
  onTouch: (number: number) => void;
};

const TouchCell = ({ number, touched, onTouch }: Props) => {
  const handleClick = () => {
    if (touched) return;
    onTouch(number);
  };

  return (
    <S.Cell $touched={touched} onClick={handleClick}>
      {touched ? (
        <S.CoffeeCircle>
          <S.CoffeeIcon src={CoffeeWhite} alt="" />
        </S.CoffeeCircle>
      ) : (
        <S.Number>{number}</S.Number>
      )}
    </S.Cell>
  );
};

export default TouchCell;
