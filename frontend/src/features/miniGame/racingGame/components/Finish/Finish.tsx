import Headline1 from '@/components/@common/Headline1/Headline1';
import { RankedPlayer } from '../../hooks/useRaceRanking';
import * as S from './Finish.styled';

const PODIUM_SIZE = 3;

type Props = {
  rankedPlayers: RankedPlayer[];
  myName: string;
};

/**
 * 결과 화면으로 넘어가기 전 2초 동안 머무는 완주 연출.
 *
 * 순위는 순위표와 같은 useRaceRanking 결과를 쓴다. 서버가 완주 시각을 내려주면 기록을
 * 함께 세울 자리다. 지금 브로드캐스트에는 그 값이 없어 위치 순서로만 세운다.
 */
const Finish = ({ rankedPlayers, myName }: Props) => {
  const myRank = rankedPlayers.findIndex((player) => player.playerName === myName) + 1;

  return (
    <S.Container role="status" aria-live="assertive">
      <Headline1 color="white">Finish</Headline1>

      <S.Podium>
        {rankedPlayers.slice(0, PODIUM_SIZE).map((player, index) => (
          <S.PodiumItem key={player.playerName} $order={index} $isMe={player.playerName === myName}>
            <S.Rank>{index + 1}</S.Rank>
            <S.Name>{player.playerName}</S.Name>
          </S.PodiumItem>
        ))}
      </S.Podium>

      {myRank > 0 && (
        <S.MyResult>
          {rankedPlayers.length}명 중 {myRank}등
        </S.MyResult>
      )}
    </S.Container>
  );
};

export default Finish;
