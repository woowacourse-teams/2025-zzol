import Button from '@/components/@common/Button/Button';
import Paragraph from '@/components/@common/Paragraph/Paragraph';
import * as S from './ConfirmModal.styled';

type Props = {
  message: string;
  onConfirm: () => void;
  onCancel: () => void;
};

const ConfirmModal = ({ message, onConfirm, onCancel }: Props) => {
  return (
    <S.Container>
      <Paragraph>{message}</Paragraph>
      <S.ButtonContainer>
        <Button variant="secondary" onClick={onCancel}>
          취소
        </Button>
        <Button variant="primary" onClick={onConfirm}>
          확인
        </Button>
      </S.ButtonContainer>
    </S.Container>
  );
};

export default ConfirmModal;
