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
          <div
            style={{
              position: 'relative',
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
            }}
          >
            <img
              src={chatBubble}
              alt="chat bubble"
              style={{ width: '280px', filter: 'hue-rotate(320deg) brightness(0.8)' }}
            />
            <div style={{ position: 'absolute', top: '40%', transform: 'translateY(-50%)' }}>
              <Headline1 color="white">탈락 ㅠㅠ</Headline1>
            </div>
          </div>
        </S.MessageWrapper>

        <img src={coffee} alt="coffee" style={{ width: '80px', opacity: 0.9 }} />

        <S.RankContainer>
          <S.RankTitle>현재 순위</S.RankTitle>
          <BlockStackingRankList isCentered />
        </S.RankContainer>
      </S.Content>
    </S.Backdrop>
  );
};

export default EliminatedOverlay;
