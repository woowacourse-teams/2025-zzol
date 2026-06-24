import { useButtonInteraction } from '@/hooks/useButtonInteraction';
import { ComponentProps, KeyboardEvent } from 'react';
import CloseIcon from '../CloseIcon/CloseIcon';
import * as S from './Input.styled';

type Props = {
  height?: string;
  onClear: () => void;
} & ComponentProps<'input'>;

const Input = ({ height = '32px', onClear, value, onChange, ref, ...rest }: Props) => {
  const hasValue = Boolean(value && String(value).length > 0);

  const { touchState, pointerHandlers } = useButtonInteraction({ onClick: onClear });

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      e.currentTarget.blur();
    }
  };

  return (
    <S.Container $height={height} $hasValue={hasValue}>
      <S.Input ref={ref} value={value} onChange={onChange} onKeyDown={handleKeyDown} {...rest} />
      <S.ClearButton
        type="button"
        {...pointerHandlers}
        aria-label="입력 내용 지우기"
        $hasValue={hasValue}
        $touchState={touchState}
      >
        <CloseIcon />
      </S.ClearButton>
    </S.Container>
  );
};

export default Input;
