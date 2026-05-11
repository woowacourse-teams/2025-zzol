import { useState } from 'react';
import useModal from '@/components/@common/Modal/useModal';
import useToast from '@/components/@common/Toast/useToast';
import { useAuth } from '../../contexts/AuthContext';
import * as S from './DeleteAccountSheet.styled';

const DeleteAccountSheet = () => {
  const { deleteAccount } = useAuth();
  const { closeModal } = useModal();
  const { showToast } = useToast();
  const [isDeleting, setIsDeleting] = useState(false);

  const handleDelete = async () => {
    setIsDeleting(true);
    try {
      await deleteAccount();
      closeModal();
      showToast({ message: '회원 탈퇴가 완료되었습니다.', type: 'success' });
    } catch {
      showToast({ message: '회원 탈퇴에 실패했습니다. 다시 시도해주세요.', type: 'error' });
    } finally {
      setIsDeleting(false);
    }
  };

  return (
    <S.Wrapper>
      <S.WarningBox>
        <S.WarningIcon>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path
              d="M12 9v4m0 4h.01M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"
              stroke="currentColor"
              strokeWidth="2"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </S.WarningIcon>
        <S.WarningText>
          탈퇴하면 계정 정보와 활동 데이터가 모두 삭제되며, 복구할 수 없습니다.
        </S.WarningText>
      </S.WarningBox>

      <S.InfoList>
        <li>• 닉네임 및 프로필 정보가 삭제됩니다.</li>
        <li>• 동일 소셜 계정으로 재가입은 가능합니다.</li>
      </S.InfoList>

      <S.ButtonGroup>
        <S.CancelButton type="button" onClick={closeModal} disabled={isDeleting}>
          취소
        </S.CancelButton>
        <S.DeleteButton
          type="button"
          onClick={handleDelete}
          disabled={isDeleting}
          aria-busy={isDeleting}
        >
          {isDeleting ? '탈퇴 중...' : '탈퇴하기'}
        </S.DeleteButton>
      </S.ButtonGroup>
    </S.Wrapper>
  );
};

export default DeleteAccountSheet;
