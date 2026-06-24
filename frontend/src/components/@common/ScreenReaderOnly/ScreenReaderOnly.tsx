import { RefAttributes } from 'react';
import * as S from './ScreenReaderOnly.styled';

type Props = {
  'aria-live'?: 'polite' | 'assertive' | 'off';
  'aria-atomic'?: boolean;
  children: string;
} & RefAttributes<HTMLDivElement>;

const ScreenReaderOnly = ({
  'aria-live': ariaLive = 'polite',
  'aria-atomic': ariaAtomic = true,
  children,
  ref,
}: Props) => {
  return (
    <S.ScreenReaderContainer ref={ref} aria-live={ariaLive} aria-atomic={ariaAtomic} tabIndex={-1}>
      {children}
    </S.ScreenReaderContainer>
  );
};

export default ScreenReaderOnly;
