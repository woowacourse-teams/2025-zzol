import Headline4 from '@/components/@common/Headline4/Headline4';
import { useIdentifier } from '@/contexts/Identifier/IdentifierContext';
import { useNunchiGameContext } from '@/contexts/NunchiGame/NunchiGameContext';
import { useReplaceNavigate } from '@/hooks/useReplaceNavigate';
import Layout from '@/layouts/Layout';
import { useEffect } from 'react';
import { useParams } from 'react-router-dom';
import NunchiStage from '../components/NunchiStage/NunchiStage';
import NunchiKeypad from '../components/NunchiKeypad/NunchiKeypad';
import * as S from './NunchiGamePlayPage.styled';

/**
 * 눈치게임 Play 페이지.
 *
 * 화면 구성(ADR 컨텍스트): 상단 = 현재 숫자/일어선 사람/일어서기·충돌 애니메이션,
 * 하단 = 입력 키패드.
 *
 * TODO(구현 — 컴포넌트 조립):
 *  - <NunchiStage/>: 현재 숫자(currentNumber) + 일어선 사람(stood, "일어섰다"만 — H)
 *    + 충돌 그룹(collided) 흔들림/빨강 애니메이션 + 쿨다운 재개 카운트다운(resumeAtEpochMs).
 *  - <NunchiKeypad/>: 큰 타겟 버튼, press() 호출, canPress 로 비활성, 끊김 경고(J).
 *  - useNunchiCountdown 으로 idle/hardCap·resumeAt 카운트다운 표시(G).
 *  - DONE 시 결과 페이지로 navigate(BlockStacking 패턴, 아래 useEffect).
 */
const NunchiGamePlayPage = () => {
  const { joinCode } = useIdentifier();
  const { gameState } = useNunchiGameContext();
  const navigate = useReplaceNavigate();
  const { miniGameType } = useParams();

  useEffect(() => {
    if (gameState === 'DONE') {
      navigate(`/room/${joinCode}/${miniGameType}/result`);
    }
  }, [gameState, joinCode, navigate, miniGameType]);

  return (
    <Layout>
      <Layout.TopBar center={<Headline4>눈치게임</Headline4>} />
      <Layout.Content>
        <S.Container>
          <NunchiStage />
          <NunchiKeypad />
        </S.Container>
      </Layout.Content>
    </Layout>
  );
};

export default NunchiGamePlayPage;
