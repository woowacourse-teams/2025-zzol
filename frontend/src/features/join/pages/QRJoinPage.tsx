import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import * as S from './QRJoinPage.styled';
import { usePlayerType } from '@/contexts/PlayerType/PlayerTypeContext';

const QRJoinPage = () => {
  const navigate = useReplaceNavigate();
  const { joinCode } = useParams<{ joinCode: string }>();
  const { setJoinCode } = useIdentifier();
  const { setPlayerType } = usePlayerType();

  useEffect(() => {
    if (joinCode) {
      setJoinCode(joinCode);
      setPlayerType('GUEST');
      navigate('/entry/name');
    } else {
      navigate('/');
    }
  }, [joinCode, setJoinCode, navigate, setPlayerType]);

  return <S.Container>방에 참여하는 중...</S.Container>;
};

export default QRJoinPage;
