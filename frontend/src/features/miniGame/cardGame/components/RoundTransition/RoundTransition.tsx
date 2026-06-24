import Description from '@/components/@common/Description/Description';
import Headline1 from '@/components/@common/Headline1/Headline1';
import Layout from '@/layouts/Layout';
import { CARD_GAME_ROUND_MAP, CardGameRound } from '@/types/miniGame/cardGame';
import { PropsWithChildren } from 'react';
import * as S from './RoundTransition.styled';

type Props = {
  currentRound: CardGameRound;
} & PropsWithChildren;

const RoundTransition = ({ currentRound, children }: Props) => {
  return (
    <Layout color="point-400">
      <S.Container>
        <S.Wrapper>
          <S.DescriptionWrapper>
            <Headline1 color="white">Round {CARD_GAME_ROUND_MAP[currentRound]}</Headline1>
            <Description color="white">다음 라운드로 이동합니다!</Description>
          </S.DescriptionWrapper>
          {children}
        </S.Wrapper>
      </S.Container>
    </Layout>
  );
};

export default RoundTransition;
