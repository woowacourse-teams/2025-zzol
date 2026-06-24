import Button from '@/components/@common/Button/Button';
import * as S from './ErrorFallback.styled';
import Headline3 from '@/components/@common/Headline3/Headline3';
import ErrorIcon from '@/components/@common/ErrorIcon/ErrorIcon';
import { getErrorInfo } from '@/utils/errorMessages';

type Props = {
  error: Error;
  handleRetry: () => void;
};

const LocalErrorFallback = ({ error, handleRetry }: Props) => {
  const { message, description } = getErrorInfo(error);

  return (
    <S.Container>
      <ErrorIcon />
      <Headline3>{message}</Headline3>
      <S.Message>{description}</S.Message>
      <Button variant="secondary" width="50%" onClick={handleRetry}>
        다시 시도하기
      </Button>
    </S.Container>
  );
};

export default LocalErrorFallback;
