import Button from '@/components/@common/Button/Button';
import * as S from './ErrorFallback.styled';
import Headline3 from '@/components/@common/Headline3/Headline3';
import ErrorIcon from '@/components/@common/ErrorIcon/ErrorIcon';
import { getErrorInfo } from '@/utils/errorMessages';

type Props = {
  error: Error;
};

const GlobalErrorFallback = ({ error }: Props) => {
  const { message, description } = getErrorInfo(error);

  return (
    <S.Container>
      <ErrorIcon />
      <Headline3>{message}</Headline3>
      <S.Message>{description}</S.Message>
      <Button variant="secondary" width="50%" onClick={() => (window.location.href = '/')}>
        메인으로 돌아가기
      </Button>
    </S.Container>
  );
};

export default GlobalErrorFallback;
