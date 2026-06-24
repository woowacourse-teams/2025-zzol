import TouchCell from '../TouchCell/TouchCell';
import * as S from './TouchGrid.styled';

type Props = {
  numbers: number[];
  nextNumber: number;
  isFinished: boolean;
  onTouch: (number: number) => void;
};

const TouchGrid = ({ numbers, nextNumber, isFinished, onTouch }: Props) => {
  return (
    <>
      <S.NextIndicator>
        {isFinished ? (
          '완주! 🎉'
        ) : (
          <>
            다음 숫자: <S.NextNumber>{nextNumber}</S.NextNumber>
          </>
        )}
      </S.NextIndicator>
      <S.Grid>
        {numbers.map((num) => (
          <TouchCell key={num} number={num} touched={num < nextNumber} onTouch={onTouch} />
        ))}
      </S.Grid>
    </>
  );
};

export default TouchGrid;
