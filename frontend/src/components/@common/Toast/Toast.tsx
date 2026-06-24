import Description from '../Description/Description';
import Portal from '../Portal/Portal';
import ErrorIcon from './Icons/ErrorIcon';
import InfoIcon from './Icons/InfoIcon';
import SuccessIcon from './Icons/SuccessIcon';
import WarningIcon from './Icons/WarningIcon';
import * as S from './Toast.styled';
import { ToastOptions } from './types';

type Props = {
  isExiting?: boolean;
} & Omit<ToastOptions, 'duration'>;

const Toast = ({ message, type, isExiting = false }: Props) => {
  const renderIcon = () => {
    switch (type) {
      case 'success':
        return <SuccessIcon />;
      case 'error':
        return <ErrorIcon />;
      case 'warning':
        return <WarningIcon />;
      case 'info':
        return <InfoIcon />;
      default:
        return null;
    }
  };

  return (
    <Portal containerId="toast-root">
      <S.Container
        $type={type}
        className={isExiting ? 'toast-exit' : ''}
        role={['error', 'warning'].includes(type) ? 'alert' : 'status'}
        aria-live={type === 'error' ? 'assertive' : 'polite'}
        aria-atomic="true"
      >
        <S.IconWrapper>{renderIcon()}</S.IconWrapper>
        <Description>{message}</Description>
      </S.Container>
    </Portal>
  );
};

export default Toast;
