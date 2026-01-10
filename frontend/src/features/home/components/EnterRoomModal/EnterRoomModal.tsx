import useLazyFetch from '@/apis/rest/useLazyFetch';
import Button from '@/components/@common/Button/Button';
import Headline4 from '@/components/@common/Headline4/Headline4';
import Input from '@/components/@common/Input/Input';
import { JOIN_CODE_LENGTH } from '@/constants/joinCode';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { ChangeEvent, KeyboardEvent, useState } from 'react';
import * as S from './EnterRoomModal.styled';

type JoinCodeCheckResponse = {
  exist: boolean;
};

type Props = {
  onClose: () => void;
};

const EnterRoomModal = ({ onClose }: Props) => {
  const navigate = useReplaceNavigate();
  const { joinCode, setJoinCode } = useIdentifier();
  const [errorText, setErrorText] = useState<string | null>(null);

  const { execute: checkJoinCode } = useLazyFetch<JoinCodeCheckResponse>({
    endpoint: `/rooms/check-joinCode?joinCode=${joinCode}`,
    onError: (error: Error) => {
      setJoinCode('');
      setErrorText(error.message);
    },
    errorDisplayMode: 'text',
  });

  const handleEnter = async () => {
    if (!joinCode.trim()) {
      setErrorText('초대코드를 입력해주세요.');
      return;
    }

    const response = await checkJoinCode();

    if (!response) return;
    if (!response.exist) {
      setErrorText('참여코드가 유효한 방이 존재하지 않습니다.');
      return;
    }

    navigate(`/entry/name`);
    onClose();
  };

  const handleJoinCodeChange = (e: ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    const upperValue = value.toUpperCase();
    const lastChar = upperValue.slice(-1);

    const isTooLong = upperValue.length > JOIN_CODE_LENGTH;
    const isNotEmpty = upperValue.length > 0;
    const isInvalidChar = isNotEmpty && !/^[A-Z0-9]$/.test(lastChar);

    if (isTooLong || isInvalidChar) return;

    setJoinCode(value.toUpperCase());
  };

  const handleKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter') {
      handleEnter();
    }
  };

  return (
    <S.Container>
      <Headline4>초대코드를 입력해주세요</Headline4>
      <Input
        type="text"
        placeholder={`${JOIN_CODE_LENGTH}자리 영문과 숫자 조합 ex) AB12`}
        value={joinCode}
        onClear={() => {
          setJoinCode('');
          setErrorText(null);
        }}
        onChange={handleJoinCodeChange}
        onKeyDown={handleKeyDown}
        autoFocus
        aria-label="초대코드를 입력해주세요"
        data-testid="join-code-input"
      />
      <S.ErrorText aria-live="assertive">{errorText}</S.ErrorText>
      <S.ButtonContainer>
        <Button variant="secondary" onClick={onClose}>
          취소
        </Button>
        <Button variant="primary" onClick={handleEnter} data-testid="enter-room-button">
          입장
        </Button>
      </S.ButtonContainer>
    </S.Container>
  );
};

export default EnterRoomModal;
