import { RacingPlayer } from '@/types/miniGame/racingGame';
import { useMemo, useState } from 'react';
import RankItem from '../RankItem/RankItem';
import * as S from './RacingRanks.styled';

type Player = Pick<RacingPlayer, 'playerName' | 'position'>;

type Props = {
  players: Player[];
  myName: string;
  endDistance: number;
};

const RacingRanks = ({ players, myName, endDistance }: Props) => {
  const [finishOrder, setFinishOrder] = useState<Player[]>([]);

  // 결승선 통과 순서를 렌더 중 누적한다(React 공식 "렌더 중 state 조정" 패턴).
  // 새 통과자가 있을 때만 setState → 즉시 재렌더 후 통과자 목록이 비어 무한 루프가 없다.
  const newFinishers = players.filter(
    ({ playerName, position }) =>
      position >= endDistance && !finishOrder.some((player) => player.playerName === playerName)
  );
  if (newFinishers.length > 0) {
    setFinishOrder((prev) => [
      ...prev,
      ...newFinishers.map(({ playerName, position }) => ({ playerName, position })),
    ]);
  }

  const rankedPlayers = useMemo(() => {
    const unFinishedSortedPlayers = players
      .filter((player) => !finishOrder.some((p) => p.playerName === player.playerName))
      .sort((a, b) => b.position - a.position);

    return [...finishOrder, ...unFinishedSortedPlayers];
  }, [players, finishOrder]);

  return (
    <S.Container>
      <S.RankList>
        {rankedPlayers.map((player, index) => {
          const isMe = player.playerName === myName;

          return (
            <RankItem
              key={player.playerName}
              playerName={player.playerName}
              rank={index + 1}
              isMe={isMe}
              isFixed={player.position >= endDistance}
            />
          );
        })}
      </S.RankList>
    </S.Container>
  );
};

export default RacingRanks;
