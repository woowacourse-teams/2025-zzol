import Headline1 from '@/components/@common/Headline1/Headline1';
import * as S from './EliminatedOverlay.styled';
import chatBubble from '@/assets/chat_bubble.svg';
import coffee from '@/assets/logo/coffee-white.png';
import BlockStackingRankList from '../BlockStackingRankList/BlockStackingRankList';

const EliminatedOverlay = () => {
  return (
    <S.Backdrop>
      <S.Content>
        <S.MessageWrapper>
          <S.ChatBubbleWrapper>
            <S.ChatBubble src={chatBubble} alt="chat bubble" />
            <S.MessageContent>
              <S.EliminatedText>탈락 ㅠㅠ</S.EliminatedText>
            </S.MessageContent>
          </S.ChatBubbleWrapper>
        </S.MessageWrapper>

        <S.GameIcon src={coffee} alt="coffee" />

        <S.RankContainer>
          <S.RankTitle>현재 순위</S.RankTitle>
          <BlockStackingRankList isCentered />
        </S.RankContainer>
      </S.Content>
    </S.Backdrop>
  );
};

export default EliminatedOverlay;
